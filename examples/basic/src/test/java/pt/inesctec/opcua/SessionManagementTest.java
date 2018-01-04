package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.StatusCodes;

import pt.inesctec.opcua.model.OpcUaProperties;

public class SessionManagementTest {

	private final String goodServerUrl = "opc.tcp://localhost:4334/UA/teste"; // Rui: "opc.tcp://194.117.27.178:4334/UA/teste"
	private final String badServerUrl = "opc.tcp://194.117.27.178:4333/UA/teste"; // what is wrong in this url is the port
	private OpcUaClient opcUaClient;

	public void setUp(String serverURL) throws ServiceResultException {
		opcUaClient = new OpcUaClient();
		opcUaClient.create("OpcUaApplication");
		opcUaClient.createOpcUaSession(new OpcUaProperties(serverURL, null, null));

		assertNotNull(opcUaClient.client);
		assertNotNull(opcUaClient.getOpcUaSession(serverURL).sessionChannel);

		assertNotNull(opcUaClient.client.getApplicationHttpsSettings());
		assertNotNull(opcUaClient.client.getApplicatioOpcTcpSettings());
		assertNotNull(opcUaClient.client.getEndpointConfiguration());
		assertNotNull(opcUaClient.client.getEncoderContext());

		assertNotNull(opcUaClient.getOpcUaSession(serverURL).sessionChannel.getSecureChannel());
		assertNotNull(opcUaClient.getOpcUaSession(serverURL).sessionChannel.getSession());
	}

	public void shutdown() throws ServiceFaultException, ServiceResultException {
		opcUaClient.shutdownOpcUaSession(null);
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

	public void testReconnectionOfBrokenSession() {
		try {
			setUp(goodServerUrl);

			opcUaClient.translateRootBrowsePathsToNodeIds(goodServerUrl, "/Objects");

			//myClient.sessionChannel.getSecureChannel().isOpen()); // it is always true ....
			//myClient.sessionChannel.getSecureChannel().getClass(); // org.opcfoundation.ua.transport.tcp.io.SecureChannelTcp)
			//myClient.sessionChannel.getSecureChannel().getConnection(); // it is always null
			//SecureChannelTcp ss = (SecureChannelTcp) myClient.sessionChannel.getSecureChannel();

			// shutdown manually the server and then restart it

			try {
				opcUaClient.translateRootBrowsePathsToNodeIds(goodServerUrl, "/Objects");
			}
			catch (ServiceResultException t) {
				assertEquals(StatusCodes.Bad_Timeout, t.getStatusCode().getValue());
				//assertEquals(StatusCodes.Bad_ServerNotConnected, t.getStatusCode().getValue());
				//assertEquals(StatusCodes.Bad_ConnectionRejected, t.getStatusCode().getValue());
			}

			opcUaClient.translateRootBrowsePathsToNodeIds(goodServerUrl, "/Objects");

			shutdown();
		}
		catch (Throwable t) {
			fail(t.getMessage());
		}
	}

}
