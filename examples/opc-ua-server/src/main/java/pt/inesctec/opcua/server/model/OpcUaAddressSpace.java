package pt.inesctec.opcua.server.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.ReferenceDescription;

public class OpcUaAddressSpace {

	public List<OpcUaObject> opcUaObjectList = new ArrayList<OpcUaObject>();

	public Map<NodeId, BrowseResult> browseActionsMap = new HashMap<NodeId, BrowseResult>();
	public Map<NodeId, Map<UnsignedInteger, DataValue>> readResultsMap = new HashMap<NodeId, Map<UnsignedInteger, DataValue>>();

	public OpcUaAddressSpace() {
	}

	public ReferenceDescription getReferenceDescription(int index) {
		return opcUaObjectList.get(index).referenceToObject;
	}

	public void init() {
		// Create two object instances as examples
		final RoboticManipulator roboticManipulator1 = new RoboticManipulator("ROBOT_ID_1", "ROBOT_NAME_1", "ROBOT_STATUS_1");
		final RoboticManipulator roboticManipulator2 = new RoboticManipulator("ROBOT_ID_2", "ROBOT_NAME_2", "ROBOT_STATUS_2");

		OpcUaObject opcUaObject1 = new OpcUaObject(roboticManipulator1);
		opcUaObjectList.add(opcUaObject1);
		OpcUaObject opcUaObject2 = new OpcUaObject(roboticManipulator2);
		opcUaObjectList.add(opcUaObject2);

		for (OpcUaObject opcUaObject : opcUaObjectList)
			try {
				new OpcUaObjectAttributesBuilder(opcUaObject, DateTime.currentTime()).buildAttributes();

				// Set the readResultsMap
				readResultsMap.put(opcUaObject.objectAttributes.nodeId, opcUaObject.objectAttributes.attributes);
				readResultsMap.put(opcUaObject.objectTypeAttributes.nodeId, opcUaObject.objectTypeAttributes.attributes);
				for (NodeId nodeId : opcUaObject.opcUaVariables.keySet()) {
					readResultsMap.put(nodeId, opcUaObject.opcUaVariables.get(nodeId).attributes);
				}

				new OpcUaObjectReferencesBuilder(opcUaObject).buildReferences();

				// Set the browseActionsMap
				browseActionsMap.put(opcUaObject.objectAttributes.nodeId, new BrowseResult(StatusCode.GOOD, null, opcUaObject.objectReferences));
				browseActionsMap.put(opcUaObject.objectTypeAttributes.nodeId, new BrowseResult(StatusCode.GOOD, null, opcUaObject.objectTypeReferences));
				for (ReferencesMap map : opcUaObject.variableReferences) {
					browseActionsMap.put(map.nodeId, new BrowseResult(StatusCode.GOOD, null, map.references));
				}
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
	}

}
