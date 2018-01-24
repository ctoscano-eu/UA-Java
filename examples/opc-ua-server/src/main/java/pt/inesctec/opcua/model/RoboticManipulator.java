package pt.inesctec.opcua.model;

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
}
