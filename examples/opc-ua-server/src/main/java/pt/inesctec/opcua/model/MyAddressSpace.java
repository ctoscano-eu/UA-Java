package pt.inesctec.opcua.model;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.BrowseResult;

public class MyAddressSpace {

	public Map<NodeId, BrowseResult> browseActionsMap = new HashMap<NodeId, BrowseResult>();
	public Map<NodeId, Map<UnsignedInteger, DataValue>> readResultsMap = new HashMap<NodeId, Map<UnsignedInteger, DataValue>>();

	public MyAddressSpace() {
	}

	public void init() {
		final RoboticManipulator roboticManipulator = new RoboticManipulator("ROBOT_ID", "ROBOT_NAME", "ROBOT_STATUS");

		try {
			buildReadResultsMap(roboticManipulator, DateTime.currentTime());
		}
		catch (Throwable e) {
			e.printStackTrace();
		}

		browseActionsMap.put(roboticManipulator.nodeId_RoboticManipulatorType, new BrowseResult(StatusCode.GOOD, null, null));
		browseActionsMap.put(roboticManipulator.nodeId_this, new BrowseResult(StatusCode.GOOD, null, roboticManipulator.getReferenceDescriptions()));
		browseActionsMap.put(roboticManipulator.nodeId_robotName, new BrowseResult(StatusCode.GOOD, null, roboticManipulator.getReferenceDescriptionForRobotName()));
		browseActionsMap.put(roboticManipulator.nodeId_robotStatus, new BrowseResult(StatusCode.GOOD, null, roboticManipulator.getReferenceDescriptionForRobotStatus()));
	}

	public void buildReadResultsMap(OpcUaMetadata obj, DateTime serverTimeStamp) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		buildAttributesForObject(obj, serverTimeStamp);
		buildAttributesForObjectType(obj, serverTimeStamp);
		for (int i = 0; i < obj.opcUa.fieldForVariable.length; ++i) {
			buildAttributesForVariable(obj, i, serverTimeStamp);
		}
	}

	private void buildAttributesForObject(OpcUaMetadata obj, DateTime serverTimeStamp) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		NodeId nodeId = (NodeId) obj.getClass().getField(obj.opcUa.nodeIdForObject).get(obj);
		String browseName = obj.opcUa.browseNameForObject;
		Map<UnsignedInteger, DataValue> attributes = MapAttributesFactory.buildMapAttributesForObject(nodeId, browseName, serverTimeStamp);

		readResultsMap.put(nodeId, attributes);
	}

	private void buildAttributesForObjectType(OpcUaMetadata obj, DateTime serverTimeStamp) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		NodeId nodeId = (NodeId) obj.getClass().getField(obj.opcUa.nodeIdForType).get(obj);
		String browseName = obj.opcUa.browseNameForType;
		Map<UnsignedInteger, DataValue> attributes = MapAttributesFactory.buildMapAttributesForObjectType(nodeId, browseName, serverTimeStamp);

		readResultsMap.put(nodeId, attributes);
	}

	public void buildAttributesForVariable(OpcUaMetadata obj, int variableIndex, DateTime serverTimeStamp)
	    throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		NodeId nodeId = (NodeId) obj.getClass().getField(obj.opcUa.nodeIdForVariable[variableIndex]).get(obj);
		String browseName = obj.opcUa.browseNameForVariable[variableIndex];
		Object variableValue = obj.getClass().getField(obj.opcUa.fieldForVariable[variableIndex]).get(obj);
		NodeId nodeIdForVariableType = obj.opcUa.nodeIdForVariableType[variableIndex];
		Map<UnsignedInteger, DataValue> attributes = MapAttributesFactory.buildMapAttributesForVariable(nodeId, browseName, variableValue, nodeIdForVariableType, serverTimeStamp);

		readResultsMap.put(nodeId, attributes);
	}

}
