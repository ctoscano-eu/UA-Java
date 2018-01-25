package pt.inesctec.opcua.server.model;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.Identifiers;

import pt.inesctec.opcua.server.model.annotations.OpcUaVariableDeclaration;

/*
 * A Java class instance is represented as an OPC UA Object.
 * The data members of the Java class instance are represented as OPC UA Variables inside the OPC UA Object.
 * The OPC UA Object has a type (ObjectType) and each Variable also has a type (OPC UA built in primitive).
 */
public class OpcUaVariable {

	public OpcUaObject opcUaObject;
	public String javafield;
	public NodeId nodeId;
	public String browseName;
	private NodeId nodeIdForVariableType;
	private Object value;
	private DateTime serverTimeStamp;
	public Map<UnsignedInteger, DataValue> attributes = new HashMap<UnsignedInteger, DataValue>(); // the attributes of the OPC UA Variable

	/*	public ReferenceDescription referenceToObject;
		public ReferenceDescription[] objectReferences;
		public ReferenceDescription[] objectTypeReferences;
		public ReferencesMap[] variableReferences;
	*/

	private OpcUaVariable(OpcUaObject opcUaObject, String javafield, NodeId nodeId, String browseName, NodeId nodeIdForVariableType, Object value) {
		this.opcUaObject = opcUaObject;
		this.javafield = javafield;
		this.nodeId = nodeId;
		this.browseName = browseName;
		this.nodeIdForVariableType = nodeIdForVariableType;
		this.value = value;
		this.serverTimeStamp = DateTime.currentTime();

		attributes = AttributesMapFactory.buildMapAttributesForVariable(nodeId, browseName, value, nodeIdForVariableType, serverTimeStamp);

	}

	private Object getJavaFieldValue() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field f = opcUaObject.javaObj.getClass().getField(javafield);
		return f.get(opcUaObject.javaObj);
	}

	static public OpcUaVariable build(OpcUaObject opcUaObject, OpcUaVariableDeclaration declaration, Field field) throws IllegalArgumentException, IllegalAccessException {
		NodeId nodeId = new NodeId(Integer.valueOf(opcUaObject.objectAttributes.nodeId.getNamespaceIndex()), UUID.randomUUID());
		String browseName = declaration.browseName();
		NodeId nodeIdForVariableType = getNodeIdForDataType(field);
		Object value = field.get(opcUaObject.javaObj);

		return new OpcUaVariable(opcUaObject, field.getName(), nodeId, browseName, nodeIdForVariableType, value);
	}

	static private NodeId getNodeIdForDataType(Field field) {
		if (field.getType().getName().equals("java.lang.String"))
			return Identifiers.String;
		else if (field.getType().getName().equals("int"))
			return Identifiers.Integer;
		else if (field.getType().getName().equals("long"))
			return Identifiers.Integer;
		else if (field.getType().getName().equals("double"))
			return Identifiers.Double;
		else if (field.getType().getName().equals("float"))
			return Identifiers.Float;
		else
			throw new RuntimeException("Unknown DataType: " + field.getType().getName());
	}

}
