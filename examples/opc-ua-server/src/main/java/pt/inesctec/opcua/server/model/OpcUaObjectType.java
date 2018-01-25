package pt.inesctec.opcua.server.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;

import pt.inesctec.opcua.server.model.annotations.OpcUaObjectTypeDeclaration;

/*
 * A Java class instance is represented as an OPC UA Object.
 * The data members of the Java class instance are represented as OPC UA Variables inside the OPC UA Object.
 * The OPC UA Object has a type (ObjectType) and each Variable also has a type (OPC UA built in primitive).
 */
public class OpcUaObjectType {

	public OpcUaObject opcUaObject;
	public NodeId nodeId;
	public String browseName;
	public Map<UnsignedInteger, DataValue> attributes = new HashMap<UnsignedInteger, DataValue>(); // the attributes of the OPC UA Object Type
	private DateTime serverTimeStamp;

	private OpcUaObjectType(OpcUaObject opcUaObject, NodeId nodeId, String browseName) {
		this.opcUaObject = opcUaObject;
		this.nodeId = nodeId;
		this.browseName = browseName;
		this.serverTimeStamp = DateTime.currentTime();

		attributes = AttributesMapFactory.buildMapAttributesForObjectType(nodeId, browseName, serverTimeStamp);
	}

	static public OpcUaObjectType build(OpcUaObject opcUaObject, OpcUaObjectTypeDeclaration decl) throws IllegalArgumentException, IllegalAccessException {
		NodeId nodeId = new NodeId(Integer.valueOf(decl.namespaceIndex()), UUID.randomUUID());
		String browseName = decl.browseName();

		return new OpcUaObjectType(opcUaObject, nodeId, browseName);
	}

}
