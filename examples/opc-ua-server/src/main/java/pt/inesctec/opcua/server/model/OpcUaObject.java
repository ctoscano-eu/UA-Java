package pt.inesctec.opcua.server.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.core.ReferenceDescription;

/*
 * A Java class instance is represented as an OPC UA Object.
 * The data members of the Java class instance are represented as OPC UA Variables inside the OPC UA Object.
 * The OPC UA Object has a type (ObjectType) and each Variable also has a type (OPC UA built in primitive).
 */
public class OpcUaObject {

	public Object javaObj; // the instance of the Java Object
	public AttributesMap objectAttributes; // the attributes of the OPC UA Object
	//public AttributesMap objectTypeAttributes; // the attributes of the OPC UA ObjectType
	public OpcUaObjectType opcUaObjectType;
	public Map<NodeId, OpcUaVariable> opcUaVariables = new HashMap<NodeId, OpcUaVariable>();

	public ReferenceDescription referenceToObject;
	public ReferenceDescription[] objectReferences;
	public ReferenceDescription[] objectTypeReferences;
	public ReferencesMap[] variableReferences;

	public OpcUaObject() {
		super();
	}

	public OpcUaObject(Object obj) {
		this.javaObj = obj;
	}

	public Collection<OpcUaVariable> getOpcUaVariables() {
		return opcUaVariables.values();
	}
}
