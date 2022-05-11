package graphRec;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datastax.dse.driver.api.core.graph.GraphNode;
import com.datastax.dse.driver.api.core.graph.GraphResultSet;

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
	
	public RecommendationsRestController() {
		DseDAL dse = new DseDAL();
		recomRepo = new RecommendationsDAL(dse.getSession());
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
    
    private String parseResults(GraphResultSet results) {
    	StringBuilder output = new StringBuilder();
    	
    	for (GraphNode node : results) {
    		output.append(node).append("\n");
    	}
    	
    	return output.toString();
    }
    
}