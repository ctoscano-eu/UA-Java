package pt.inesctec.opcua.server.model;

import pt.inesctec.opcua.server.model.annotations.OpcUaObjectDeclaration;
import pt.inesctec.opcua.server.model.annotations.OpcUaObjectTypeDeclaration;
import pt.inesctec.opcua.server.model.annotations.OpcUaVariableDeclaration;

@OpcUaObjectDeclaration(browseName = "RoboticManipulator", namespaceIndex = "2")
@OpcUaObjectTypeDeclaration(browseName = "RoboticManipulatorType", namespaceIndex = "2")
public class RoboticManipulator {

	public String robotId;

	@OpcUaVariableDeclaration(browseName = "RobotName")
	public String robotName;
	@OpcUaVariableDeclaration(browseName = "RobotStatus")
	public String robotStatus;

	public RoboticManipulator() {
		super();
	}

	public RoboticManipulator(String robotId, String robotName, String robotStatus) {
		this.robotId = robotId;
		this.robotName = robotName;
		this.robotStatus = robotStatus;
	}
}
