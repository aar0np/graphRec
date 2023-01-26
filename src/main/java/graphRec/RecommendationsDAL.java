package graphRec;

import com.datastax.dse.driver.api.core.graph.FluentGraphStatement;
import com.datastax.dse.driver.api.core.graph.GraphResultSet;
import com.datastax.oss.driver.api.core.CqlSession;

import java.util.Map;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import static org.apache.tinkerpop.gremlin.process.traversal.Order.desc;
import static com.datastax.dse.driver.api.core.graph.DseGraph.g;

public class RecommendationsDAL {
	private CqlSession session;

	public RecommendationsDAL(CqlSession session) {
		this.session = session;
	}
	
	public GraphResultSet getPrecomputedRecommendationsByMovie(int movieId) {
		
		GraphTraversal<Vertex, Map<String, Object>> traversal = g.V()
				.has("Movie", "movie_id", movieId).as("original_movie")
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
//		dev.V().has("Movie","movie_id",1270).
//	    aggregate("originalMovie").
//	    inE("rated").has("rating",gt(4.5)).outV().
//	    outE("rated").has("rating",gt(4.5)).inV().
//	    where(without("originalMovie")).
//	    group().
//	        by("movie_title").
//	        by(count()).
//	    unfold().
//	    order().
//	        by(values, desc)
	        
		GraphTraversal<Vertex, Object> traversal = g.with("allow-filtering")
			.V().has("Movie", "movie_id", movieId)
				.aggregate("originalMovie")
			    .inE("rated").has("rating",P.gt("4.5")).outV()
			    .outE("rated").has("rating",P.gt("4.5")).inV()
			    .where(P.without("originalMovie"))
			    .group()
			        .by("movie_title")
			        .by(__.count()).as("count")
			    .order(Scope.local)
			        .by(Column.values,desc)
			    .unfold()
		        .limit(5);
	
		FluentGraphStatement stmt = FluentGraphStatement.newInstance(traversal);
		stmt.setGraphName("movies_dev");
		GraphResultSet result = session.execute(stmt);
		 
		return result;	    
	}
	
	public GraphResultSet getRecommendationsByUser(int userId) {
//		dev.V().has("User","user_id", 694).   // look up a user
//		   outE("rated").                     // traverse to all rated movies
//		     order().by("timestamp", desc).   // order all edges by time
//		     limit(1).inV().                  // traverse to the most recent rated movie
//		     aggregate("originalMovie").      // put this movie in a collection
//		   inE("rated").has("rating", gt(4.5)).outV().  // all users who rated this movie a 5
//		   outE("rated").has("rating", gt(4.5)).inV().  // the full recomendation set
//		   where(without('originalMovie')).   // remove the original movie
//		   group().                           // create a map of the recommendations
//		     by("movie_title").               // an entry's key is the movie title, 
//		     by(count()).                     // the value will be the total # of ratings
//		   unfold().                          // unfold all map entries into the pipline
//		   order().                           // order the results
//		     by(values, desc)                 // by their count, descending

		GraphTraversal<Vertex, Object> traversal = g.with("allow-filtering")
			.V().has("User","user_id", userId)
				.outE("rated")
					.order().by("timestamp", desc)
					.limit(1).inV()
					.aggregate("originalMovie")
				.inE("rated").has("rating", P.gt("4.5")).outV()
				.outE("rated").has("rating", P.gt("4.5")).inV()
				.where(P.without("originalMovie"))
				.group()
					.by("movie_title")
					.by(__.count())
				.unfold()
				.order(Scope.local)
					.by(__.values(), desc);
				
		FluentGraphStatement stmt = FluentGraphStatement.newInstance(traversal);
		stmt.setGraphName("movies_dev");
		GraphResultSet result = session.execute(stmt);
		 
		return result;	    
	}
}
