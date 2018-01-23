package pt.inesctec.opcua.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReferenceDescription;

public class RoboticManipulator {

	public String robotId;
	public String robotName;
	public String robotStatus;

	public NodeId nodeId_RoboticManipulatorType;
	public NodeId nodeId_this;
	public NodeId nodeId_robotName;
	public NodeId nodeId_robotStatus;

	public RoboticManipulator() {
		super();
	}

	public RoboticManipulator(String robotId, String robotName, String robotStatus) {
		this.robotId = robotId;
		this.robotName = robotName;
		this.robotStatus = robotStatus;

		nodeId_RoboticManipulatorType = new NodeId(2, getClass().getName());
		nodeId_this = new NodeId(2, 1);
		nodeId_robotName = new NodeId(2, 2);
		nodeId_robotStatus = new NodeId(2, 3);
	}

	public ReferenceDescription getReferenceDescription() {
		return new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(nodeId_this), qualifiedName("RoboticManipulator"), new LocalizedText("RoboticManipulator"), NodeClass.Object,
		    new ExpandedNodeId(nodeId_RoboticManipulatorType));
	}

	public ReferenceDescription[] getReferenceDescriptionForRoboticManipulatorType() {
		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();

		list.add(new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(nodeId_this), qualifiedName("RoboticManipulator"), new LocalizedText("RoboticManipulator"),
		    NodeClass.Variable, new ExpandedNodeId(nodeId_this)));

		return list.toArray(new ReferenceDescription[list.size()]);
	}

	public ReferenceDescription[] getReferenceDescriptionForRobotName() {
		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();

		list.add(new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(nodeId_this), qualifiedName("RoboticManipulator"), new LocalizedText("RoboticManipulator"),
		    NodeClass.Variable, new ExpandedNodeId(nodeId_this)));
		list.add(new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.BaseDataVariableType), new QualifiedName("BaseDataVariableType"),
		    new LocalizedText("BaseDataVariableType"), NodeClass.VariableType, new ExpandedNodeId(Identifiers.BaseDataVariableType)));

		return list.toArray(new ReferenceDescription[list.size()]);
	}

	public ReferenceDescription[] getReferenceDescriptionForRobotStatus() {
		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();

		list.add(new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(nodeId_this), qualifiedName("RoboticManipulator"), new LocalizedText("RoboticManipulator"),
		    NodeClass.Variable, new ExpandedNodeId(nodeId_this)));
		list.add(new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.BaseDataVariableType), new QualifiedName("BaseDataVariableType"),
		    new LocalizedText("BaseDataVariableType"), NodeClass.VariableType, new ExpandedNodeId(Identifiers.BaseDataVariableType)));

		return list.toArray(new ReferenceDescription[list.size()]);
	}

	public ReferenceDescription[] getReferenceDescriptions() {
		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();

		// ReferenceDescription for type of this instance 
		list.add(new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(nodeId_RoboticManipulatorType), qualifiedName("RoboticManipulatorType"),
		    new LocalizedText("RoboticManipulatorType"), NodeClass.ObjectType, new ExpandedNodeId(nodeId_RoboticManipulatorType)));

		// ReferenceDescription for member robotName
		list.add(new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(nodeId_robotName), qualifiedName("RobotName"), new LocalizedText("RobotName"), NodeClass.Variable,
		    new ExpandedNodeId(nodeId_robotName)));
		// ReferenceDescription for member robotStatus
		list.add(new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(nodeId_robotStatus), qualifiedName("RobotStatus"), new LocalizedText("RobotStatus"), NodeClass.Variable,
		    new ExpandedNodeId(nodeId_robotStatus)));

		return list.toArray(new ReferenceDescription[list.size()]);
	}

	public QualifiedName qualifiedName(String name) {
		return new QualifiedName(1, name);
	}

	public Map<UnsignedInteger, DataValue> getAttributes(DateTime serverTimeStamp) {
		return MapAttributesFactory.buildMapAttributesForObject(nodeId_this, "RoboticManipulator", serverTimeStamp);
	}

	public Map<UnsignedInteger, DataValue> getAttributesForType(DateTime serverTimeStamp) {
		return MapAttributesFactory.buildMapAttributesForObjectType(nodeId_RoboticManipulatorType, "RoboticManipulatorType", serverTimeStamp);
	}

	public Map<UnsignedInteger, DataValue> getAttributesForRobotName(DateTime serverTimeStamp) {
		return MapAttributesFactory.buildMapAttributesForVariable(nodeId_robotName, "RobotName", robotName, serverTimeStamp);
	}

	public Map<UnsignedInteger, DataValue> getAttributesForRobotStatus(DateTime serverTimeStamp) {
		return MapAttributesFactory.buildMapAttributesForVariable(nodeId_robotStatus, "RobotStatus", robotStatus, serverTimeStamp);
	}

}
