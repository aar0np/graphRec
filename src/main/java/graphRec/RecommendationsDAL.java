package graphRec;

import com.datastax.dse.driver.api.core.graph.FluentGraphStatement;
import com.datastax.dse.driver.api.core.graph.GraphResultSet;
import com.datastax.oss.driver.api.core.CqlSession;

import java.util.Map;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import static com.datastax.dse.driver.api.core.graph.DseGraph.g;

public class RecommendationsDAL {
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
		stmt.setGraphName("movies_prod");
		GraphResultSet result = session.execute(stmt);
		 
		return result;
	}
	
	public GraphResultSet getRecommendationsByMovie(int movieId) {
		GraphTraversal<Vertex, Object> traversal = g.V().has("Movie", "movie_id", movieId)
			.aggregate("original_movie")
		    .inE("rated").has("rating",P.gt(4.5)).outV()
		    .outE("rated").has("rating",P.gt(4.5)).inV()
		    .where(P.without("originalMovie"))
		    .group()
		        .by("movie_title")
		        .by(__.count())
		    .unfold()
		    .order()
		        .by(__.values(),Order.desc);
	
		FluentGraphStatement stmt = FluentGraphStatement.newInstance(traversal);
		stmt.setGraphName("movies_dev");
		GraphResultSet result = session.execute(stmt);
		 
		return result;	    
	}
}
