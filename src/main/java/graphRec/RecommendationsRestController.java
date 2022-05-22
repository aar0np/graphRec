package graphRec;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;

import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
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
import io.swagger.v3.oas.annotations.media.Schema;
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
	
	private static final String SERVICE_URL = System.getenv("ASTRA_STREAM_URL");
	private static final String YOUR_PULSAR_TOKEN = System.getenv("ASTRA_STREAM_TOKEN");
	private static final String STREAMING_TENANT = System.getenv("ASTRA_STREAM_TENANT");
	private static final String STREAMING_PREFIX = STREAMING_TENANT + "/default/";
	private static final String PENDING_ORDER_TOPIC = "persistent://" + STREAMING_PREFIX + "pending-orders";

	public RecommendationsRestController() {
		DseDAL dse = new DseDAL();
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
           schema = @Schema(implementation = String.class, name = "String")
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
    public ResponseEntity<String> findPreCompRecommendationsByMovieId(
            HttpServletRequest req, 
            @PathVariable(value = "movieid")
            @Parameter(name = "movieid", description = "movie identifier", example = "1270")
            int movieid) {
    	
    	GraphResultSet results = recomRepo.getPrecomputedRecommendationsByMovie(movieid);
    	// String recommendationsJSON = new Gson().toJson(results);
    	String recommendations = parseResults(results);
    	
    	return ResponseEntity.ok(recommendations);
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
           schema = @Schema(implementation = String.class, name = "String")
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
    public ResponseEntity<String> findRealtimeRecommendationsByMovieId(
            HttpServletRequest req, 
            @PathVariable(value = "movieid")
            @Parameter(name = "movieid", description = "movie identifier", example = "1270")
            int movieid) {
    	
    	GraphResultSet results = recomRepo.getRecommendationsByMovie(movieid);
    	// String recommendationsJSON = new Gson().toJson(results);
    	String recommendations = parseResults(results);
    	
    	return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/user/{userid}/rating/")
    @Operation(
     summary = "Place an order",
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
    
    private void sendToRatingStream(String message) throws Exception {
        // Create producer on a topic
    	try {
	    	Producer<byte[]> orderProducer = client.newProducer()
	                .topic(PENDING_ORDER_TOPIC)
	                .create();
	
	    	// Send a message to the topic
	        orderProducer.send(message.getBytes());
	        orderProducer.close();
		} catch (PulsarClientException e) {
			// issue creating the streaming message producer
			e.printStackTrace();
		}
    }
    
    private String parseResults(GraphResultSet results) {
    	StringBuilder output = new StringBuilder();
    	
    	for (GraphNode node : results) {
    		output.append(node).append("\n");
    	}
    	
    	return output.toString();
    }
    
}