package graphRec;

import java.net.InetSocketAddress;

import com.datastax.oss.driver.api.core.CqlSession;

public class DseDAL {
	private static final String CONTACT_POINT = "127.0.0.1";
	//private static final String GRAPH_NAME = "movies_prod";

	private CqlSession session;
	
	public DseDAL() {
		InetSocketAddress address = new InetSocketAddress(CONTACT_POINT, 9042);
		session = CqlSession.builder()
				.addContactPoint(address)
				.build();
	}
	
	public CqlSession getSession() {
		return this.session;
	}
	
}
