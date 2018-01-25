package pt.inesctec.opcua.server.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.ReferenceDescription;

public class OpcUaAddressSpace {

	public List<OpcUaObject> opcUaObjectList = new ArrayList<OpcUaObject>();

	public Map<NodeId, BrowseResult> browseActionsMap = new HashMap<NodeId, BrowseResult>();

	private Map<NodeId, OpcUaObject> opcUaObjectMap = new HashMap<NodeId, OpcUaObject>();
	private Map<NodeId, OpcUaObjectType> opcUaObjectTypeMap = new HashMap<NodeId, OpcUaObjectType>();
	private Map<NodeId, OpcUaVariable> opcUavariableMap = new HashMap<NodeId, OpcUaVariable>();

	public OpcUaAddressSpace() {
	}

	public ReferenceDescription getReferenceDescription(int index) {
		return opcUaObjectList.get(index).referenceToObject;
	}

	public void init() throws Throwable {
		// Create two object instances as examples
		final RoboticManipulator roboticManipulator1 = new RoboticManipulator("ROBOT_ID_1", "ROBOT_NAME_1", "ROBOT_STATUS_1");
		final RoboticManipulator roboticManipulator2 = new RoboticManipulator("ROBOT_ID_2", "ROBOT_NAME_2", "ROBOT_STATUS_2");

		// Create OpcUaObject and internal hierarchy (OpcUaOjectType, OpcUaVariables)
		try {
			OpcUaObject opcUaObject1 = new OpcUaObject(roboticManipulator1);
			opcUaObjectList.add(opcUaObject1);
			OpcUaObject opcUaObject2 = new OpcUaObject(roboticManipulator2);
			opcUaObjectList.add(opcUaObject2);
		}
		catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}

		for (OpcUaObject opcUaObject : opcUaObjectList) {
			// Set opcUaObjectMap, opcUaObjectTypeMap, opcUavariableMap 
			opcUaObjectMap.put(opcUaObject.objectAttributes.nodeId, opcUaObject);
			opcUaObjectTypeMap.put(opcUaObject.opcUaObjectType.nodeId, opcUaObject.opcUaObjectType);
			for (OpcUaVariable var : opcUaObject.opcUaVariables.values()) {
				opcUavariableMap.put(var.nodeId, var);
			}

			// Set the browseActionsMap
			browseActionsMap.put(opcUaObject.objectAttributes.nodeId, new BrowseResult(StatusCode.GOOD, null, opcUaObject.objectReferences));
			browseActionsMap.put(opcUaObject.opcUaObjectType.nodeId, new BrowseResult(StatusCode.GOOD, null, opcUaObject.objectTypeReferences));
			for (ReferencesMap map : opcUaObject.variableReferences) {
				browseActionsMap.put(map.nodeId, new BrowseResult(StatusCode.GOOD, null, map.references));
			}
		}
	}

	public Map<UnsignedInteger, DataValue> getResultsMap(NodeId key) {
		OpcUaObject o = opcUaObjectMap.get(key);
		if (o != null)
			return o.objectAttributes.attributes;

		OpcUaObjectType t = opcUaObjectTypeMap.get(key);
		if (t != null)
			return t.attributes;

		OpcUaVariable v = opcUavariableMap.get(key);
		if (v != null)
			return v.attributes;

		return null;
	}

	public boolean resultsMapContainsKey(NodeId key) {
		boolean b1 = opcUaObjectMap.containsKey(key);
		if (b1)
			return b1;

		boolean b2 = opcUaObjectTypeMap.containsKey(key);
		if (b2)
			return b2;

		boolean b3 = opcUavariableMap.containsKey(key);
		if (b3)
			return b3;

		return false;
	}

	public BrowseResult getBrowseActionsMap(NodeId key) {
		return browseActionsMap.get(key);
	}

	public boolean browseActionsMapContainsKey(NodeId key) {
		return browseActionsMap.containsKey(key);
	}

}
