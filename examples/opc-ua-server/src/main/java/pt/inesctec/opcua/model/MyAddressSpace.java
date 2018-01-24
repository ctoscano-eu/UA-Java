package pt.inesctec.opcua.model;

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

public class MyAddressSpace {

	public List<OpcUaObject> opcUaObjectList = new ArrayList<OpcUaObject>();

	public Map<NodeId, BrowseResult> browseActionsMap = new HashMap<NodeId, BrowseResult>();
	public Map<NodeId, Map<UnsignedInteger, DataValue>> readResultsMap = new HashMap<NodeId, Map<UnsignedInteger, DataValue>>();

	public MyAddressSpace() {
	}

	public ReferenceDescription getReferenceDescription() {
		return opcUaObjectList.get(0).referenceToObject;
	}

	public void init() {
		final RoboticManipulator roboticManipulator = new RoboticManipulator("ROBOT_ID", "ROBOT_NAME", "ROBOT_STATUS");
		OpcUaObject opcUaObject = new OpcUaObject(roboticManipulator);
		opcUaObjectList.add(opcUaObject);

		for (OpcUaObject obj : opcUaObjectList)
			try {
				new OpcUaObjectAttributesBuilder(obj, DateTime.currentTime()).buildAttributes();

				// Set the readResultsMap
				readResultsMap.put(obj.objectAttributes.nodeId, opcUaObject.objectAttributes.attributes);
				readResultsMap.put(opcUaObject.objectTypeAttributes.nodeId, opcUaObject.objectTypeAttributes.attributes);
				for (AttributesMap map : obj.variableAttributes)
					readResultsMap.put(map.nodeId, map.attributes);

				new OpcUaObjectReferencesBuilder(obj).buildReferences();

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
