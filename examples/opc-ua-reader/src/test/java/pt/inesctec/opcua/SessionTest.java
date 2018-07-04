package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.CreateSubscriptionResponse;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MonitoredItemCreateResult;
import org.opcfoundation.ua.core.PublishResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.WriteValue;

import pt.inesctec.opcua.model.OpcUaProperties;

public class SessionTest {

	//private final String serverUrl = "opc.tcp://194.117.27.107:53530/OPCUA/SimulationServer";
	private final String serverUrl = "opc.tcp://localhost:4334/UA/teste";
	//private final String serverUrl = "opc.tcp://194.117.27.178:4334/UA/teste";

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
	public void testBrowseNodeOjectsAndVariables() {
		try {

			NodeId[] nodeIdArray = new NodeId[] { Identifiers.RootFolder };
			BrowseResult[] res1 = opcUaClient.browseNodeOjectsAndVariables(serverUrl, nodeIdArray); // "/"
			assertEquals(3, res1[0].getReferences().length);
			assertEquals("Objects", res1[0].getReferences()[0].getBrowseName().getName());
			assertEquals("Types", res1[0].getReferences()[1].getBrowseName().getName());
			assertEquals("Views", res1[0].getReferences()[2].getBrowseName().getName());

			BrowseResult[] res2 = opcUaClient.browseNodeOjectsAndVariables(serverUrl, res1[0].getReferences()[0].getNodeId()); // "/Objects"
			assertEquals(3, res2[0].getReferences().length);
			assertEquals("Server", res2[0].getReferences()[0].getBrowseName().getName());
			assertEquals("MyCNCDevice", res2[0].getReferences()[1].getBrowseName().getName());
			assertEquals("MyCNCDevice", res2[0].getReferences()[2].getBrowseName().getName());

			BrowseResult[] res3 = opcUaClient.browseNodeOjectsAndVariables(serverUrl, res2[0].getReferences()[0].getNodeId());// "/Objects/Server"
			assertEquals(12, res3[0].getReferences().length);
			assertEquals("ServerArray", res3[0].getReferences()[0].getBrowseName().getName());
			assertEquals("NamespaceArray", res3[0].getReferences()[1].getBrowseName().getName());
			assertEquals("ServerStatus", res3[0].getReferences()[2].getBrowseName().getName());
			assertEquals("ServiceLevel", res3[0].getReferences()[3].getBrowseName().getName());
			assertEquals("Auditing", res3[0].getReferences()[4].getBrowseName().getName());
			assertEquals("EstimatedReturnTime", res3[0].getReferences()[5].getBrowseName().getName());
			assertEquals("ServerCapabilities", res3[0].getReferences()[6].getBrowseName().getName());
			assertEquals("ServerDiagnostics", res3[0].getReferences()[7].getBrowseName().getName());
			assertEquals("VendorServerInfo", res3[0].getReferences()[8].getBrowseName().getName());
			assertEquals("ServerRedundancy", res3[0].getReferences()[9].getBrowseName().getName());
			assertEquals("Namespaces", res3[0].getReferences()[10].getBrowseName().getName());
			assertEquals("ServerConfiguration", res3[0].getReferences()[11].getBrowseName().getName());

			BrowseResult[] res4 = opcUaClient.browseNodeOjectsAndVariables(serverUrl, res2[0].getReferences()[1].getNodeId());// "/Objects/MyCNCDevice"
			assertEquals(4, res4[0].getReferences().length);
		}
		catch (Throwable t) {
			fail(t.getMessage());
		}
	}

	@Test
	public void testTranslateBrowsePathsToNodeIds() {
		try {
			BrowsePathResult[] res = null;

			MyBrowsePath myBrowsePath = new MyBrowsePath(Identifiers.RootFolder);
			myBrowsePath.setElements("0/Objects", "0/Objects/0/Server", "0/Objects/0/Server/0/ServerStatus");
			res = opcUaClient.translateBrowsePathsToNodeIds(serverUrl, myBrowsePath);
			assertEquals(3, res.length);
			for (int i = 0; i < res.length; ++i) {
				assertEquals(1, res[i].getTargets().length);
				assertNotNull(res[i].getTargets()[0].getTargetId().toString());
			}
		}
		catch (Throwable t) {
			fail(t.getMessage());
		}
	}

	@Test
	public void testReadVariableValue() {
		try {
			DataValue[] dataValues = null;

			// With NodeIds
			NodeId[] nodeIdArray = new NodeId[] { Identifiers.Server_NamespaceArray, new NodeId(1, 1007), new NodeId(1, 1006), new NodeId(1, "Boolean"), new NodeId(1, "/Objects/MyCNCDevice/Model") };
			dataValues = opcUaClient.readVariableValue(serverUrl, nodeIdArray);

			assertEquals(StatusCode.GOOD, dataValues[0].getStatusCode());
			assertEquals(StatusCode.GOOD, dataValues[1].getStatusCode());
			assertEquals(StatusCode.GOOD, dataValues[2].getStatusCode());
			assertNotEquals(StatusCode.GOOD, dataValues[3].getStatusCode());
			assertNotNull(dataValues[0].getValue().toString());
			assertNotNull(dataValues[1].getValue().toString());
			assertNotNull(dataValues[2].getValue().toString());

			// TODO ctoscano reimplement using 0/Objects/0/Server/0/ServerArr
			// With BrowsePaths
			String[] pathArray = new String[] { "/Objects/Server/ServerArray", "/Objects/Server/NamespaceArray", "/Objects/Server/ServerStatus", "/Objects/Server/ServiceLevel", "/Objects/Server/Auditing",
			    "/Objects/Server/EstimatedReturnTime", "/Objects/Server/ServerCapabilities", "/Objects/Server/ServerDiagnostics", "/Objects/Server/VendorServerInfo", "/Objects/Server/ServerRedundancy",
			    "/Objects/Server/Namespaces", "/Objects/Server/ServerConfiguration" };
			dataValues = opcUaClient.readVariableValue(serverUrl, pathArray);

			assertEquals(StatusCode.GOOD, dataValues[0].getStatusCode());
			assertEquals(StatusCode.GOOD, dataValues[1].getStatusCode());
			assertEquals(StatusCode.GOOD, dataValues[2].getStatusCode());
			assertNotNull(dataValues[0].getValue().toString());
			assertNotNull(dataValues[1].getValue().toString());
			assertNotNull(dataValues[2].getValue().toString());
		}
		catch (Throwable t) {
			fail(t.getMessage());
		}
	}

	@Test
	public void testBrowseHierarchyOfNodeVariables() {
		try {
			// Retrieve the NodeId of each Variable
			NodeId[] nodeIdArray = new NodeId[] { Identifiers.RootFolder };
			List<BrowseResult> res = opcUaClient.browseHierarchyOfNodeVariables(serverUrl, nodeIdArray);
			assertTrue(res.get(0).getReferences().length > 0);

			// Read each Variable
			nodeIdArray = new NodeId[res.get(0).getReferences().length];
			for (int i = 0; i < nodeIdArray.length; ++i) {
				nodeIdArray[i] = opcUaClient.toNodeId(serverUrl, res.get(0).getReferences()[i].getNodeId());
			}
			DataValue[] dataValues = opcUaClient.readVariableValue(serverUrl, nodeIdArray);
			for (int i = 0; i < nodeIdArray.length; ++i) {
				//assertEquals(StatusCode.GOOD, dataValues[i].getStatusCode());  some of the status are: Bad_WaitingForInitialData (0x80320000) "Waiting for the server to obtain values from the underlying data source." 
				assertNotNull(dataValues[i].getValue().toString());
			}
		}
		catch (Throwable t) {
			fail(t.getMessage());
		}
	}

	@Test
	public void testbrowseHierarchyOfNodeVariablesUnderObjects() {
		try {
			BrowsePathResult[] res1 = opcUaClient.translateRootBrowsePathsToNodeIds(serverUrl, "/Objects");
			ExpandedNodeId objectsNodeId = res1[0].getTargets()[0].getTargetId();

			List<BrowseResult> res2 = opcUaClient.browseHierarchyOfNodeVariables(serverUrl, opcUaClient.toNodeId(serverUrl, objectsNodeId));
			assertTrue(res2.get(0).getReferences().length > 0);
		}
		catch (Throwable t) {
			fail(t.getMessage());
		}
	}

	@Test
	public void testWriteVariables() {
		try {
			BrowsePathResult[] var = opcUaClient.translateRootBrowsePathsToNodeIds(serverUrl, "/Objects/MyCNCDevice/Model");
			NodeId varNodeId = opcUaClient.toNodeId(serverUrl, var[0].getTargets()[0].getTargetId());
			DataValue[] dataValues = opcUaClient.readVariableValue(serverUrl, varNodeId);
			assertEquals("NX1234", dataValues[0].getValue().toString());

			dataValues[0].setValue(new Variant("NX5678"));

			WriteValue writeValue = toWriteValue(varNodeId, dataValues[0]);

			StatusCode[] res = opcUaClient.write(serverUrl, writeValue);
			assertEquals(UnsignedInteger.ZERO, res[0].getValue());

		}
		catch (Throwable t) {
			fail(t.getMessage());
		}
	}

	private WriteValue toWriteValue(NodeId nodeId, DataValue dataValue) {
		WriteValue writeValue = new WriteValue();
		writeValue.setNodeId(nodeId);
		writeValue.setAttributeId(Attributes.Value);
		writeValue.setValue(dataValue);
		return writeValue;
	}

	@Test
	public void testSusbribeVariable() {
		try {
			CreateSubscriptionResponse subscription = opcUaClient.createSubscription(serverUrl);

			BrowsePathResult[] var = opcUaClient.translateRootBrowsePathsToNodeIds(serverUrl, "/Objects/MyCNCDevice/Model");
			NodeId varNodeId = opcUaClient.toNodeId(serverUrl, var[0].getTargets()[0].getTargetId());

			ReadValueId itemToMonitor = new ReadValueId(varNodeId, Attributes.Value, null, null);

			MonitoredItemCreateResult[] monitoredItems = opcUaClient.createMonitoredItems(serverUrl, subscription.getSubscriptionId(), itemToMonitor);
			assertTrue(monitoredItems.length == 1);
			assertEquals(StatusCode.GOOD, monitoredItems[0].getStatusCode());
			assertNotNull(monitoredItems[0].getMonitoredItemId());

			PublishResponse res = opcUaClient.publish(serverUrl, subscription.getSubscriptionId(), 0);
			res = opcUaClient.publish(serverUrl, subscription.getSubscriptionId(), 1);
			res = opcUaClient.publish(serverUrl, subscription.getSubscriptionId(), 1);

		}
		catch (Throwable t) {
			fail(t.getMessage());
		}
	}
}
