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
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowsePathResult;
import org.opcfoundation.ua.core.CreateSubscriptionResponse;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MonitoredItemCreateResult;
import org.opcfoundation.ua.core.PublishResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.WriteValue;

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
	public void testBrowseNodeOjectsAndVariables() {
		try {
			NodeId[] nodeIdArray = new NodeId[] { Identifiers.RootFolder };
			ReferenceDescription[] references1 = myClient.browseNodeOjectsAndVariables(nodeIdArray); // "/"
			assertEquals(3, references1.length);
			assertEquals("Objects", references1[0].getBrowseName().getName());
			assertEquals("Types", references1[1].getBrowseName().getName());
			assertEquals("Views", references1[2].getBrowseName().getName());

			ReferenceDescription[] references2 = myClient.browseNodeOjectsAndVariables(references1[0].getNodeId()); // "/Objects"
			assertEquals(3, references2.length);
			assertEquals("Server", references2[0].getBrowseName().getName());
			assertEquals("MyCNCDevice", references2[1].getBrowseName().getName());
			assertEquals("MyCNCDevice", references2[2].getBrowseName().getName());

			ReferenceDescription[] references3 = myClient.browseNodeOjectsAndVariables(references2[0].getNodeId());// "/Objects/Server"
			assertEquals(12, references3.length);
			assertEquals("ServerArray", references3[0].getBrowseName().getName());
			assertEquals("NamespaceArray", references3[1].getBrowseName().getName());
			assertEquals("ServerStatus", references3[2].getBrowseName().getName());
			assertEquals("ServiceLevel", references3[3].getBrowseName().getName());
			assertEquals("Auditing", references3[4].getBrowseName().getName());
			assertEquals("EstimatedReturnTime", references3[5].getBrowseName().getName());
			assertEquals("ServerCapabilities", references3[6].getBrowseName().getName());
			assertEquals("ServerDiagnostics", references3[7].getBrowseName().getName());
			assertEquals("VendorServerInfo", references3[8].getBrowseName().getName());
			assertEquals("ServerRedundancy", references3[9].getBrowseName().getName());
			assertEquals("Namespaces", references3[10].getBrowseName().getName());
			assertEquals("ServerConfiguration", references3[11].getBrowseName().getName());

			ReferenceDescription[] references4 = myClient.browseNodeOjectsAndVariables(references2[1].getNodeId());// "/Objects/MyCNCDevice"
			assertEquals(4, references4.length);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testTranslateBrowsePathsToNodeIds() {
		try {
			BrowsePathResult[] res = null;
			String[] names = null;

			names = new String[] { "Objects", "Objects/Server", "Objects/Server/ServerStatus" };
			res = myClient.translateBrowsePathsToNodeIds(Identifiers.RootFolder, names);
			assertEquals(3, res.length);
			for (int i = 0; i < res.length; ++i) {
				assertEquals(1, res[i].getTargets().length);
				assertNotNull(res[i].getTargets()[0].getTargetId().toString());
			}

			names = new String[] { "/Objects", "/Objects/Server", "/Objects/Server/ServerStatus" };
			res = myClient.translateRootBrowsePathsToNodeIds(names);
			assertEquals(3, res.length);
			for (int i = 0; i < res.length; ++i) {
				assertEquals(1, res[i].getTargets().length);
				assertNotNull(res[i].getTargets()[0].getTargetId().toString());
			}

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testReadVariableValue() {
		try {
			DataValue[] dataValues = null;

			// With NodeIds
			NodeId[] nodeIdArray = new NodeId[] { Identifiers.Server_NamespaceArray, new NodeId(1, 1007), new NodeId(1, 1006), new NodeId(1, "Boolean"), new NodeId(1, "/Objects/MyCNCDevice/Model") };
			dataValues = myClient.readVariableValue(nodeIdArray);

			assertEquals(StatusCode.GOOD, dataValues[0].getStatusCode());
			assertEquals(StatusCode.GOOD, dataValues[1].getStatusCode());
			assertEquals(StatusCode.GOOD, dataValues[2].getStatusCode());
			assertNotEquals(StatusCode.GOOD, dataValues[3].getStatusCode());
			assertNotNull(dataValues[0].getValue().toString());
			assertNotNull(dataValues[1].getValue().toString());
			assertNotNull(dataValues[2].getValue().toString());

			// With BrowsePaths
			String[] pathArray = new String[] { "/Objects/Server/ServerArray", "/Objects/Server/ServerStatus", "/Objects/Server/ServiceLevel" };
			dataValues = myClient.readVariableValue(pathArray);

			assertEquals(StatusCode.GOOD, dataValues[0].getStatusCode());
			assertEquals(StatusCode.GOOD, dataValues[1].getStatusCode());
			assertEquals(StatusCode.GOOD, dataValues[2].getStatusCode());
			assertNotNull(dataValues[0].getValue().toString());
			assertNotNull(dataValues[1].getValue().toString());
			assertNotNull(dataValues[2].getValue().toString());

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testBrowseHierarchyOfNodeVariables() {
		try {
			// Retrieve the NodeId of each Variable
			NodeId[] nodeIdArray = new NodeId[] { Identifiers.RootFolder };
			List<ReferenceDescription> references = myClient.browseHierarchyOfNodeVariables(nodeIdArray);
			assertTrue(references.size() > 0);

			// Read each Variable
			nodeIdArray = new NodeId[references.size()];
			for (int i = 0; i < nodeIdArray.length; ++i) {
				nodeIdArray[i] = myClient.toNodeId(references.get(i).getNodeId());
			}
			DataValue[] dataValues = myClient.readVariableValue(nodeIdArray);
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
	public void testbrowseHierarchyOfNodeVariablesUnderObjects() {
		try {
			BrowsePathResult[] res = myClient.translateRootBrowsePathsToNodeIds("/Objects");
			ExpandedNodeId objectsNodeId = res[0].getTargets()[0].getTargetId();

			List<ReferenceDescription> references = myClient.browseHierarchyOfNodeVariables(myClient.toNodeId(objectsNodeId));
			assertTrue(references.size() > 0);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testWriteVariables() {
		try {
			BrowsePathResult[] var = myClient.translateRootBrowsePathsToNodeIds("/Objects/MyCNCDevice/Model");
			NodeId varNodeId = myClient.toNodeId(var[0].getTargets()[0].getTargetId());
			DataValue[] dataValues = myClient.readVariableValue(varNodeId);
			assertEquals("NX1234", dataValues[0].getValue().toString());

			dataValues[0].setValue(new Variant("NX5678"));

			WriteValue writeValue = toWriteValue(varNodeId, dataValues[0]);

			StatusCode[] res = myClient.write(writeValue);
			assertEquals(UnsignedInteger.ZERO, res[0].getValue());

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private WriteValue toWriteValue(NodeId nodeId, DataValue dataValue) {
		WriteValue writeValue = new WriteValue();
		writeValue.setNodeId(nodeId);
		writeValue.setAttributeId(Attributes.Value);
		writeValue.setValue(dataValue);
		return writeValue;
	}

	//@Test
	public void testSusbribeVariable() {
		try {
			CreateSubscriptionResponse subscription = myClient.createSubscription();

			BrowsePathResult[] var = myClient.translateRootBrowsePathsToNodeIds("/Objects/MyCNCDevice/Model");
			NodeId varNodeId = myClient.toNodeId(var[0].getTargets()[0].getTargetId());

			ReadValueId itemToMonitor = new ReadValueId(varNodeId, Attributes.Value, null, null);

			MonitoredItemCreateResult[] monitoredItems = myClient.createMonitoredItems(subscription.getSubscriptionId(), itemToMonitor);
			assertTrue(monitoredItems.length == 1);
			assertEquals(StatusCode.GOOD, monitoredItems[0].getStatusCode());
			assertNotNull(monitoredItems[0].getMonitoredItemId());

			PublishResponse res = myClient.publish(subscription.getSubscriptionId(), 1);

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
