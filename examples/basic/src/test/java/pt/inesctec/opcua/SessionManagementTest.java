package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.StatusCodes;

public class SessionManagementTest {

	private final String goodServerUrl = "opc.tcp://194.117.27.178:4334/UA/teste";
	private final String badServerUrl = "opc.tcp://localhost:4334/UA/teste";
	private MyCLient myClient;

	public void setUp(String serverURL) throws ServiceResultException {
		myClient = new MyCLient();
		myClient.create("SampleClient");
		myClient.createSession(serverURL);

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
	public void testCreateSessionWithExistingServer() {
		try {
			for (int i = 0; i < 3; ++i) {
				setUp(goodServerUrl);
				shutdown();
			}
		}
		catch (Throwable t) {
			fail(t.getMessage());
		}
	}

	@Test
	public void testCreateSessionWithNonExistingServer() {
		try {
			for (int i = 0; i < 3; ++i) {
				setUp(badServerUrl);
				fail("Session to server established but this wasn't expected");
				shutdown();
			}
		}
		catch (ServiceResultException t) {
			assertEquals(StatusCodes.Bad_ConnectionRejected, t.getStatusCode().getValue());
		}
	}

	
	public void testReconnectionOfBrokebSession() {
		try {
			setUp(goodServerUrl);

			myClient.translateRootBrowsePathsToNodeIds("/Objects");

			//myClient.sessionChannel.getSecureChannel().isOpen()); // it is always true ....
			//myClient.sessionChannel.getSecureChannel().getClass(); // org.opcfoundation.ua.transport.tcp.io.SecureChannelTcp)
			//myClient.sessionChannel.getSecureChannel().getConnection(); // it is always null
			//SecureChannelTcp ss = (SecureChannelTcp) myClient.sessionChannel.getSecureChannel();

			// shutdown manually the server and then restart it

			try {
				myClient.translateRootBrowsePathsToNodeIds("/Objects");
			}
			catch (ServiceResultException t) {
				assertEquals(StatusCodes.Bad_Timeout, t.getStatusCode().getValue());
				//assertEquals(StatusCodes.Bad_ServerNotConnected, t.getStatusCode().getValue());
				//assertEquals(StatusCodes.Bad_ConnectionRejected, t.getStatusCode().getValue());
			}

			myClient.translateRootBrowsePathsToNodeIds("/Objects");

			shutdown();
		}
		catch (Throwable t) {
			fail(t.getMessage());
		}
	}

}
