package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;

import pt.inesctec.opcua.model.OpcUaProperties;

public class OpcUaVariablesToFetchTest {

	private final String serverUrl = "opc.tcp://localhost:4334/UA/teste";
	private OpcUaClient opcUaClient;

	@Before
	public void setUp() throws ServiceResultException {
		opcUaClient = new OpcUaClient();
		opcUaClient.create("OpcUaApplication");
		opcUaClient.createOpcUaSession(new OpcUaProperties(serverUrl, null, null));
	}

	@After
	public void shutdown() throws ServiceFaultException, ServiceResultException {
		opcUaClient.shutdownOpcUaSession(serverUrl);
	}

	@Test
	public void testReadVariablesFromSameServer() {
		try {
			String[] variables = new String[] { "/0/Objects/0/Server/0/ServerArray", "/0/Objects/0/Server/0/NamespaceArray", "/0/Objects/0/Server/0/ServerStatus", "/0/Objects/0/Server/0/ServerStatus" };

			DataValue[] res = opcUaClient.readVariableValue("opc.tcp://localhost:4334/UA/teste", variables);
			assertEquals(4, res.length);
			for (int i = 0; i < res.length; ++i)
				assertEquals(StatusCode.GOOD, res[i].getStatusCode());
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
