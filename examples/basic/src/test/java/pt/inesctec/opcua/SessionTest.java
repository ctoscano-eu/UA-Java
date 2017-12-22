package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
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
			BrowseDescription startingNode = new BrowseDescription();
			startingNode.setNodeId(Identifiers.RootFolder);
			startingNode.setBrowseDirection(BrowseDirection.Forward);
			startingNode.setIncludeSubtypes(true);
			startingNode.setNodeClassMask(NodeClass.Object, NodeClass.Variable);
			startingNode.setResultMask(BrowseResultMask.All);
			//browse.setReferenceTypeId

			BrowseResponse res = myClient.sessionChannel.Browse(null, null, null, startingNode);
			checkBrowseResponseForRoot(res);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void checkBrowseResponseForRoot(BrowseResponse res) {
		assertNotNull(res);

		assertEquals(0, res.getDiagnosticInfos().length);

		assertEquals(StatusCode.GOOD, res.getResults()[0].getStatusCode());

		checkReferencesForRoot(res.getResults()[0].getReferences());
	}

	private void checkReferencesForRoot(ReferenceDescription[] references) {
		assertEquals(3, references.length);

		assertEquals("Objects", references[0].getBrowseName().getName());
		assertEquals("Types", references[1].getBrowseName().getName());
		assertEquals("Views", references[2].getBrowseName().getName());
	}

}
