package graphRec;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;

import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datastax.dse.driver.api.core.graph.GraphNode;
import com.datastax.dse.driver.api.core.graph.GraphResultSet;

import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.PulsarClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@CrossOrigin(
  methods = {POST, GET, PUT},
  maxAge = 3600,
  allowedHeaders = {"x-requested-with", "origin", "content-type", "accept"},
  origins = "*" 
)
@RequestMapping("/api/v1/recommendations/")
@Tag(name = "Recommendations Service", description="Provide crud operations for recommendations")
public class RecommendationsRestController {
	private RecommendationsDAL recomRepo;
	private PulsarClient client;
	
	private static final String DSE_ENDPOINT = System.getenv("DSE_ENDPOINT");
	private static final String DSE_DC = System.getenv("DSE_DATACENTER");
	
	private static final String SERVICE_URL = System.getenv("ASTRA_STREAM_URL");
	private static final String YOUR_PULSAR_TOKEN = System.getenv("ASTRA_STREAM_TOKEN");
	private static final String STREAMING_TENANT = System.getenv("ASTRA_STREAM_TENANT");
	private static final String STREAMING_PREFIX = STREAMING_TENANT + "/default/";
	private static final String PENDING_ORDER_TOPIC = "persistent://" + STREAMING_PREFIX + "user-ratings";

	public RecommendationsRestController() {
		DseDAL dse = new DseDAL(DSE_ENDPOINT, DSE_DC);
		recomRepo = new RecommendationsDAL(dse.getSession());
		
		// Create Pulsar/Astra Streaming client
		try {
			client = PulsarClient.builder()
		        .serviceUrl(SERVICE_URL)
		        .authentication(
		            AuthenticationFactory.token(YOUR_PULSAR_TOKEN)
		        )
		        .build();
		} catch (PulsarClientException e) {
			// issue building the client stream connection
			e.printStackTrace();
		}
	}
	
    @GetMapping("/movies/precomputed/{movieid}")
    @Operation(
     summary = "Retrieve precomputed recommendations by movieid",
     description= "Find precomputed recommendations by movieid",
     responses = {
       @ApiResponse(
         responseCode = "200",
         description = "A list of movies recommended by movieid",
         content = @Content(
           mediaType = "application/json",
           schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class, name = "String")
         )
       ),
       @ApiResponse(
         responseCode = "404", 
         description = "movieId not found",
         content = @Content(mediaType = "")),
       @ApiResponse(
         responseCode = "400",
         description = "Invalid parameter check movieId format."),
       @ApiResponse(
         responseCode = "500",
         description = "Internal error.") 
    })
    public ResponseEntity<Stream<Recommendation>> findPreCompRecommendationsByMovieId(
            HttpServletRequest req, 
            @PathVariable(value = "movieid")
            @Parameter(name = "movieid", description = "movie identifier", example = "1270")
            int movieid) {
    	
    	List<Recommendation> recommendations = findPreComputedRecsByMovieId(movieid);
    	return ResponseEntity.ok(recommendations.stream());
    }
    
    @GetMapping("/movies/recommend/{movieid}")
    @Operation(
     summary = "Retrieve realtime recommendations by movieid",
     description= "Find realtime recommendations by movieid",
     responses = {
       @ApiResponse(
         responseCode = "200",
         description = "A list of movies recommended by movieid",
         content = @Content(
           mediaType = "application/json",
           schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class, name = "String")
         )
       ),
       @ApiResponse(
         responseCode = "404", 
         description = "movieId not found",
         content = @Content(mediaType = "")),
       @ApiResponse(
         responseCode = "400",
         description = "Invalid parameter check movieId format."),
       @ApiResponse(
         responseCode = "500",
         description = "Internal error.") 
    })
    public ResponseEntity<Stream<Recommendation>> findRealtimeRecommendationsByMovieId(
            HttpServletRequest req, 
            @PathVariable(value = "movieid")
            @Parameter(name = "movieid", description = "movie identifier", example = "1270")
            int movieid) {
    	
    	List<Recommendation> recommendations = findRealtimeRecsByMovieId(movieid);
    	return ResponseEntity.ok(recommendations.stream());
    }

    @GetMapping("/users/recommend/{userid}")
    @Operation(
     summary = "Retrieve realtime recommendations by userid",
     description= "Find realtime recommendations by userid",
     responses = {
       @ApiResponse(
         responseCode = "200",
         description = "A list of movies recommended by userid",
         content = @Content(
           mediaType = "application/json",
           schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class, name = "String")
         )
       ),
       @ApiResponse(
         responseCode = "404", 
         description = "userId not found",
         content = @Content(mediaType = "")),
       @ApiResponse(
         responseCode = "400",
         description = "Invalid parameter check userid format."),
       @ApiResponse(
         responseCode = "500",
         description = "Internal error.") 
    })
    public ResponseEntity<Stream<Recommendation>> findRealtimeRecommendationsByUserId(
            HttpServletRequest req, 
            @PathVariable(value = "userid")
            @Parameter(name = "userid", description = "user identifier", example = "694")
            int userid) {
    	
    	List<Recommendation> recommendations = findRealtimeRecsByUserId(userid);
    	
    	return ResponseEntity.ok(recommendations.stream());
    }
    
    @PostMapping("/user/{userid}/rating/")
    @Operation(
     summary = "User rates a new movie",
     description= "Create an edge for a user's rating of a movie",
     responses = {
       @ApiResponse(
         responseCode = "200",
         description = "Create a movie rating for the user",
         content = @Content(
           mediaType = "application/json")
       ),
       @ApiResponse(
         responseCode = "404", 
         description = "An error occured",
         content = @Content(mediaType = "")),
       @ApiResponse(
         responseCode = "400",
         description = "Invalid parameter."),
       @ApiResponse(
         responseCode = "500",
         description = "Internal error.")
    })
    public ResponseEntity<String> addUserRating(
    		HttpServletRequest req,
            @RequestBody UserRating rating,
            @PathVariable(value = "userid")
            @Parameter(name = "userid", description = "user identifier (int)", example = "694")
            int userid) {
    	
    	// make sure the request has the userid and the timestamp
    	rating.setUserId(userid);
    	rating.setTimestamp(new Date());
    	
    	// send to pulsar/astra stream topic
    	String ratingJSON = new Gson().toJson(rating);
    	
		try {
			sendToRatingStream(ratingJSON);
		} catch (Exception e) {
			return ResponseEntity.ok("Error w/ data stream: " + e.getMessage());
		}
		
    	return ResponseEntity.ok("Rating submitted!");
    }
    
    public List<Recommendation> findPreComputedRecsByMovieId(int movieid) {

    	GraphResultSet results = recomRepo.getPrecomputedRecommendationsByMovie(movieid);
    	List<Recommendation> recommendations = parsePreComputeResults(results);

    	return recommendations;
    }
    
    public List<Recommendation> findRealtimeRecsByMovieId(int movieid) {

    	GraphResultSet results = recomRepo.getRecommendationsByMovie(movieid);
    	List<Recommendation> recommendations = parseResults(results);

    	return recommendations;
    }
    
    public List<Recommendation> findRealtimeRecsByUserId(int userid) {
    	GraphResultSet results = recomRepo.getRecommendationsByUser(userid);
    	List<Recommendation> recommendations = parseResults(results);	

    	return recommendations;
    }
    
    private void sendToRatingStream(String message) throws Exception {
        // Create producer on a topic
    	try {
	    	Producer<String> ratingProducer = client.newProducer(Schema.STRING)
	                .topic(PENDING_ORDER_TOPIC)
	                .create();
	
	    	// Send a message to the topic
	    	ratingProducer.send(message);
	        ratingProducer.close();
		} catch (PulsarClientException e) {
			// issue creating the streaming message producer
			e.printStackTrace();
		}
    }
    
    private List<Recommendation> parseResults(GraphResultSet results) {
    	List<Recommendation> returnVal = new ArrayList<Recommendation>();
    	
    	for (GraphNode gNode : results) {
    		Recommendation rec = new Recommendation();
    		String node = gNode.toString()
    				.replace('{',' ')
    				.replace('}',' ');
    		String[] values = node.trim().split("=");
    		
    		rec.setRecommendation(values[0]);
    		rec.setScore(Double.parseDouble(values[1]));
    		
    		returnVal.add(rec);
    	}
    	
    	return returnVal;
    }
    
    private List<Recommendation> parsePreComputeResults(GraphResultSet results) {
    	List<Recommendation> returnVal = new ArrayList<Recommendation>();
    	
    	for (GraphNode node : results) {
    		Recommendation rec = new Recommendation();
    		
    		rec.setOriginal(node.getByKey("Original").asString());
    		rec.setRecommendation(node.getByKey("Recommendation").asString());
    		rec.setScore(node.getByKey("Score").asDouble());
    		
    		returnVal.add(rec);
    	}
    	
    	return returnVal;
    }
    
}