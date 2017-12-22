package pt.inesctec.opcua;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;

public class SessionManagementTest {

	private final String serverUrl = "opc.tcp://localhost:4334/UA/teste";
	private MyCLient myClient;

	public void setUp() throws ServiceResultException {
		myClient = new MyCLient();
		myClient.create("SampleClient");
		myClient.createSession(serverUrl);

		assertNotNull(myClient.client);
		assertNotNull(myClient.sessionChannel);

		assertNotNull(myClient.client.getApplicationHttpsSettings());
		assertNotNull(myClient.client.getApplicatioOpcTcpSettings());
		assertNotNull(myClient.client.getEndpointConfiguration());
		assertNotNull(myClient.client.getEncoderContext());

		assertNotNull(myClient.sessionChannel.getSecureChannel());
		assertNotNull(myClient.sessionChannel.getSession());
	}

	public void shutdown() throws ServiceFaultException, ServiceResultException {
		myClient.shutdownSession();
	}

	@Test
	public void testCreateSession() {
		try {
			for (int i = 0; i < 3; ++i) {
				setUp();
				shutdown();
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	//	@Test
	//	public void test() {
	//		fail("Not yet implemented");
	//	}

}
