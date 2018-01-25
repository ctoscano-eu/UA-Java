package pt.inesctec.opcua.server.model;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;

import pt.inesctec.opcua.server.model.annotations.OpcUaObjectDeclaration;
import pt.inesctec.opcua.server.model.annotations.OpcUaObjectTypeDeclaration;
import pt.inesctec.opcua.server.model.annotations.OpcUaVariableDeclaration;

public class OpcUaObjectAttributesBuilder {

	private OpcUaObject opcUaObject;
	private DateTime serverTimeStamp;

	public OpcUaObjectAttributesBuilder(OpcUaObject opcUaObject, DateTime serverTimeStamp) {
		this.opcUaObject = opcUaObject;
		this.serverTimeStamp = serverTimeStamp;
	}

	public void buildAttributes() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		buildAttributesForObject();
		buildAttributesForObjectType();

		for (Field field : opcUaObject.javaObj.getClass().getFields()) {
			buildAttributesForVariable(field);
		}
	}

	private void buildAttributesForObject() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		OpcUaObjectDeclaration decl = opcUaObject.javaObj.getClass().getAnnotation(OpcUaObjectDeclaration.class);
		if (decl == null)
			throw new RuntimeException("OpcUaObjectDeclaration not found on " + opcUaObject.javaObj.toString());

		NodeId nodeId = new NodeId(Integer.valueOf(decl.namespaceIndex()), UUID.randomUUID());
		String browseName = decl.browseName();
		Map<UnsignedInteger, DataValue> attributes = AttributesMapFactory.buildMapAttributesForObject(nodeId, browseName, serverTimeStamp);

		opcUaObject.objectAttributes = new AttributesMap(nodeId, browseName, attributes);
	}

	private void buildAttributesForObjectType() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		OpcUaObjectTypeDeclaration decl = opcUaObject.javaObj.getClass().getAnnotation(OpcUaObjectTypeDeclaration.class);
		if (decl == null)
			throw new RuntimeException("OpcUaObjectTypeDeclaration not found on " + opcUaObject.javaObj.toString());

		NodeId nodeId = new NodeId(Integer.valueOf(decl.namespaceIndex()), UUID.randomUUID());
		String browseName = decl.browseName();
		Map<UnsignedInteger, DataValue> attributes = AttributesMapFactory.buildMapAttributesForObjectType(nodeId, browseName, serverTimeStamp);

		opcUaObject.objectTypeAttributes = new AttributesMap(nodeId, browseName, attributes);
	}

	private void buildAttributesForVariable(Field field) throws IllegalArgumentException, IllegalAccessException {
		OpcUaVariableDeclaration decl = field.getAnnotation(OpcUaVariableDeclaration.class);
		if (decl == null)
			return;

		// Create OpcUaVariable
		OpcUaVariable opcUaVariable = OpcUaVariable.build(opcUaObject, decl, field);
		// Add OpcUaVariable to OpcUaObject
		opcUaObject.opcUaVariables.put(opcUaVariable.nodeId, opcUaVariable);
	}

}
