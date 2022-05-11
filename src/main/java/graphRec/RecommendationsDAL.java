package graphRec;

import com.datastax.dse.driver.api.core.graph.FluentGraphStatement;
import com.datastax.dse.driver.api.core.graph.GraphResultSet;
import com.datastax.oss.driver.api.core.CqlSession;

import java.util.Map;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import static com.datastax.dse.driver.api.core.graph.DseGraph.g;

public class RecommendationsDAL {
	//private GraphTraversalSource graph;
	private CqlSession session;

	public RecommendationsDAL(CqlSession session) {
		this.session = session;
	}
	
	public GraphResultSet getPrecomputedRecommendationsByMovie(int movieId) {
		
		 GraphTraversal<Vertex, Map<String, Object>> traversal = g.V().has("Movie", "movie_id", movieId).as("original_movie")
				.outE("recommend")
				.limit(5)
				.project("Original", "Recommendation", "Score")
					.by(__.select("original_movie").values("movie_title"))
					.by(__.inV().values("movie_title"))
					.by(__.values("nps_score"));
		
		FluentGraphStatement stmt = FluentGraphStatement.newInstance(traversal);
		GraphResultSet result = session.execute(stmt);
		 
		return result;
	}
}
