package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
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
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	//	@Test
	//	public void testTranslateRoot() {
	//		try {
	//			BrowsePath path = new BrowsePath();
	//			path.setStartingNode(Identifiers.RootFolder);
	//			RelativePath relativePath = new RelativePath();
	//			RelativePathElement e = 
	//			relativePath.setElements(Elements);
	//			path.setRelativePath(RelativePath);
	//
	//			TranslateBrowsePathsToNodeIdsResponse res = myClient.sessionChannel.TranslateBrowsePathsToNodeIds(null, path);
	//			checkTranslateBrowsePathsToNodeIdsResponse(res);
	//		}
	//		catch (Throwable t) {
	//			t.printStackTrace();
	//		}
	//	}
	//
	//	private void checkTranslateBrowsePathsToNodeIdsResponse(TranslateBrowsePathsToNodeIdsResponse res) {
	//		assertEquals(0, res.getDiagnosticInfos().length);
	//
	//		assertEquals(StatusCode.GOOD, res.getResults()[0].getStatusCode());
	//
	//	}

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
}
