package graphRec;

import java.net.InetSocketAddress;

import com.datastax.oss.driver.api.core.CqlSession;

public class DseDAL {
	private static final String CONTACT_POINT = "127.0.0.1";
	private static final String DATA_CENTER_NAME = "DC1";
	
	private CqlSession session;
	
	public DseDAL(String contactPoint, String dataCenterName) {
		
		if (contactPoint == null || contactPoint.trim().isEmpty()) {
			contactPoint = CONTACT_POINT;
		}
		
		if (dataCenterName == null || dataCenterName.trim().isEmpty()) {
			dataCenterName = DATA_CENTER_NAME;
		}
		
		InetSocketAddress address = new InetSocketAddress(CONTACT_POINT, 9042);
		session = CqlSession.builder()
				.addContactPoint(address)
				.withLocalDatacenter("MovingPictures")
				.build();
	}
	
	public CqlSession getSession() {
		return this.session;
	}
}
