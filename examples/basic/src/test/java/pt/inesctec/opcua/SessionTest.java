package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.BrowsePathResult;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.ReferenceDescription;

public class SessionTest {

	private final String serverUrl = "opc.tcp://localhost:4334/UA/teste";
	private MyCLient myClient;

	@Before
	public void setUp() throws ServiceResultException {
		myClient = new MyCLient();
		myClient.create("SampleClient");
		myClient.createSession(serverUrl);
	}

	@After
	public void shutdown() throws ServiceFaultException, ServiceResultException {
		myClient.shutdownSession();
	}

	@Test
	public void testBrowseRoot() {
		try {
			NodeId[] nodeIdArray = new NodeId[] { Identifiers.RootFolder };
			ReferenceDescription[] references = myClient.browse(nodeIdArray);
			assertEquals(3, references.length);
			assertEquals("Objects", references[0].getBrowseName().getName());
			assertEquals("Types", references[1].getBrowseName().getName());
			assertEquals("Views", references[2].getBrowseName().getName());

			references = myClient.browse(references[0].getNodeId());
			assertEquals(3, references.length);
			assertEquals("Server", references[0].getBrowseName().getName());
			assertEquals("MyCNCDevice", references[1].getBrowseName().getName());
			assertEquals("MyCNCDevice", references[2].getBrowseName().getName());

			references = myClient.browse(references[0].getNodeId());
			assertEquals(12, references.length);
			assertEquals("ServerArray", references[0].getBrowseName().getName());
			assertEquals("NamespaceArray", references[1].getBrowseName().getName());
			assertEquals("ServerStatus", references[2].getBrowseName().getName());
			assertEquals("ServiceLevel", references[3].getBrowseName().getName());
			assertEquals("Auditing", references[4].getBrowseName().getName());
			assertEquals("EstimatedReturnTime", references[5].getBrowseName().getName());
			assertEquals("ServerCapabilities", references[6].getBrowseName().getName());
			assertEquals("ServerDiagnostics", references[7].getBrowseName().getName());
			assertEquals("VendorServerInfo", references[8].getBrowseName().getName());
			assertEquals("ServerRedundancy", references[9].getBrowseName().getName());
			assertEquals("Namespaces", references[10].getBrowseName().getName());
			assertEquals("ServerConfiguration", references[11].getBrowseName().getName());
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testTranslateRoot() {
		try {
			BrowsePathResult[] res = null;
			String[] names = null;

			names = new String[] { "Objects", "Objects/Server", "Objects/Server/ServerStatus" };
			for (int i = 0; i < names.length; ++i) {
				res = myClient.translateBrowsePathsToNodeIds(Identifiers.RootFolder, names[i]);
				assertEquals(1, res.length);
				assertEquals(1, res[0].getTargets().length);
				assertNotNull(res[0].getTargets()[0].getTargetId().toString());
			}

			names = new String[] { "/Objects", "/Objects/Server", "/Objects/Server/ServerStatus" };
			for (int i = 0; i < names.length; ++i) {
				res = myClient.translateBrowsePathsToNodeIds(names[i]);
				assertEquals(1, res.length);
				assertEquals(1, res[0].getTargets().length);
				assertNotNull(res[0].getTargets()[0].getTargetId().toString());
			}

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testReadVariables() {
		try {
			NodeId[] nodeIdArray = new NodeId[] { Identifiers.Server_NamespaceArray, new NodeId(1, 1007), new NodeId(1, 1006), new NodeId(1, "Boolean") };
			DataValue[] dataValues = myClient.read(nodeIdArray);

			assertEquals(StatusCode.GOOD, dataValues[0].getStatusCode());
			assertEquals(StatusCode.GOOD, dataValues[1].getStatusCode());
			assertEquals(StatusCode.GOOD, dataValues[1].getStatusCode());
			assertNotEquals(StatusCode.GOOD, dataValues[3].getStatusCode());
			assertNotNull(dataValues[0].getValue().toString());
			assertNotNull(dataValues[1].getValue().toString());
			assertNotNull(dataValues[1].getValue().toString());
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testRetrieveAllVariablesUnderRoot() {
		try {
			// Retrieve the NodeId of each Variable
			NodeId[] nodeIdArray = new NodeId[] { Identifiers.RootFolder };
			List<ReferenceDescription> references = myClient.retrieveAllVariables(nodeIdArray);
			assertTrue(references.size() > 0);

			// Read each Variable
			nodeIdArray = new NodeId[references.size()];
			for (int i = 0; i < nodeIdArray.length; ++i) {
				nodeIdArray[i] = myClient.toNodeId(references.get(i).getNodeId());
			}
			DataValue[] dataValues = myClient.read(nodeIdArray);
			for (int i = 0; i < nodeIdArray.length; ++i) {
				//assertEquals(StatusCode.GOOD, dataValues[i].getStatusCode());  some of the status are: Bad_WaitingForInitialData (0x80320000) "Waiting for the server to obtain values from the underlying data source." 
				assertNotNull(dataValues[i].getValue().toString());
			}

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testRetrieveAllVariablesUnderObjects() {
		try {
			BrowsePathResult[] res = myClient.translateBrowsePathsToNodeIds("/Objects");
			ExpandedNodeId objectsNodeId = res[0].getTargets()[0].getTargetId();

			List<ReferenceDescription> references = myClient.retrieveAllVariables(myClient.toNodeId(objectsNodeId));
			assertTrue(references.size() > 0);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
