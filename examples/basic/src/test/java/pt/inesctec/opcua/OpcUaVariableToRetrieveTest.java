package pt.inesctec.opcua;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;

public class OpcUaVariableToRetrieveTest {

	private final String serverUrl = "opc.tcp://localhost:4334/UA/teste";
	private OpcUaClient opcUaClient;

	@Before
	public void setUp() throws ServiceResultException {
		opcUaClient = new OpcUaClient();
		opcUaClient.create("SampleClient");
		opcUaClient.createOpcUaSession(serverUrl);
	}

	@After
	public void shutdown() throws ServiceFaultException, ServiceResultException {
		opcUaClient.shutdownOpcUaSession(serverUrl);
	}

	@Test
	public void testBrowseNodeOjectsAndVariables() {
		try {
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
