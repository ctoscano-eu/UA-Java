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

import pt.inesctec.opcua.model.OpcUaVariableDeclaration;

@OpcUaObjectDeclaration(browseName = "RoboticManipulator", nodeIdNamespaceIndex = "2")
@OpcUaObjectTypeDeclaration(browseName = "RoboticManipulatorType", nodeIdNamespaceIndex = "2")
public class RoboticManipulator {

	public String robotId;

	@OpcUaVariableDeclaration(browseName = "RobotName", nodeIdNamespaceIndex = "2")
	public String robotName;
	@OpcUaVariableDeclaration(browseName = "RobotStatus", nodeIdNamespaceIndex = "2")
	public String robotStatus;

	public RoboticManipulator() {
		super();
	}

	public RoboticManipulator(String robotId, String robotName, String robotStatus) {
		this.robotId = robotId;
		this.robotName = robotName;
		this.robotStatus = robotStatus;

	}

//	public ReferenceDescription getReferenceDescription() {
//		return new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(nodeId_this), qualifiedName("RoboticManipulator"), new LocalizedText("RoboticManipulator"), NodeClass.Object,
//		    new ExpandedNodeId(nodeId_RoboticManipulatorType));
//	}
//
//	public ReferenceDescription[] getReferenceDescriptionForRoboticManipulatorType() {
//		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();
//
//		list.add(new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(nodeId_this), qualifiedName("RoboticManipulator"), new LocalizedText("RoboticManipulator"),
//		    NodeClass.Variable, new ExpandedNodeId(nodeId_this)));
//
//		return list.toArray(new ReferenceDescription[list.size()]);
//	}
//
//	public ReferenceDescription[] getReferenceDescriptionForRobotName() {
//		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();
//
//		list.add(new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(nodeId_this), qualifiedName("RoboticManipulator"), new LocalizedText("RoboticManipulator"),
//		    NodeClass.Variable, new ExpandedNodeId(nodeId_this)));
//		list.add(new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.BaseDataVariableType), new QualifiedName("BaseDataVariableType"),
//		    new LocalizedText("BaseDataVariableType"), NodeClass.VariableType, new ExpandedNodeId(Identifiers.BaseDataVariableType)));
//
//		return list.toArray(new ReferenceDescription[list.size()]);
//	}
//
//	public ReferenceDescription[] getReferenceDescriptionForRobotStatus() {
//		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();
//
//		list.add(new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(nodeId_this), qualifiedName("RoboticManipulator"), new LocalizedText("RoboticManipulator"),
//		    NodeClass.Variable, new ExpandedNodeId(nodeId_this)));
//		list.add(new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.BaseDataVariableType), new QualifiedName("BaseDataVariableType"),
//		    new LocalizedText("BaseDataVariableType"), NodeClass.VariableType, new ExpandedNodeId(Identifiers.BaseDataVariableType)));
//
//		return list.toArray(new ReferenceDescription[list.size()]);
//	}
//
//	public ReferenceDescription[] getReferenceDescriptions() {
//		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();
//
//		// ReferenceDescription for type of this instance 
//		list.add(new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(nodeId_RoboticManipulatorType), qualifiedName("RoboticManipulatorType"),
//		    new LocalizedText("RoboticManipulatorType"), NodeClass.ObjectType, new ExpandedNodeId(nodeId_RoboticManipulatorType)));
//
//		// ReferenceDescription for member robotName
//		list.add(new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(nodeId_robotName), qualifiedName("RobotName"), new LocalizedText("RobotName"), NodeClass.Variable,
//		    new ExpandedNodeId(nodeId_robotName)));
//		// ReferenceDescription for member robotStatus
//		list.add(new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(nodeId_robotStatus), qualifiedName("RobotStatus"), new LocalizedText("RobotStatus"), NodeClass.Variable,
//		    new ExpandedNodeId(nodeId_robotStatus)));
//
//		return list.toArray(new ReferenceDescription[list.size()]);
//	}

	public QualifiedName qualifiedName(String name) {
		return new QualifiedName(1, name);
	}

}
