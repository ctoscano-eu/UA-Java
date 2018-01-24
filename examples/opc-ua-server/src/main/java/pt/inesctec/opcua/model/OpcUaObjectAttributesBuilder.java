package pt.inesctec.opcua.model;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaObjectAttributesBuilder {

	private final Logger logger = LoggerFactory.getLogger(MyAddressSpace.class);

	private OpcUaObject opcUaObject;
	private DateTime serverTimeStamp;

	public OpcUaObjectAttributesBuilder(OpcUaObject opcUaObject, DateTime serverTimeStamp) {
		this.opcUaObject = opcUaObject;
		this.serverTimeStamp = serverTimeStamp;
	}

	public void buildAttributes() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		buildAttributesForObject();
		buildAttributesForObjectType();

		for (Field field : opcUaObject.obj.getClass().getFields()) {
			buildAttributesForVariable(field);
		}
	}

	private void buildAttributesForObject() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		OpcUaObjectDeclaration decl = opcUaObject.obj.getClass().getAnnotation(OpcUaObjectDeclaration.class);
		if (decl == null)
			logger.warn("OpcUaObjectDeclaration not found on " + opcUaObject.obj.toString());

		NodeId nodeId = new NodeId(Integer.valueOf(decl.nodeIdNamespaceIndex()), UUID.randomUUID());
		String browseName = decl.browseName();
		Map<UnsignedInteger, DataValue> attributes = MapAttributesFactory.buildMapAttributesForObject(nodeId, browseName, serverTimeStamp);

		opcUaObject.objectAttributes = new AttributesMap(nodeId, browseName, attributes);
	}

	private void buildAttributesForObjectType() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		OpcUaObjectTypeDeclaration decl = opcUaObject.obj.getClass().getAnnotation(OpcUaObjectTypeDeclaration.class);
		if (decl == null)
			logger.warn("OpcUaObjectTypeDeclaration not found on " + opcUaObject.obj.toString());

		NodeId nodeId = new NodeId(Integer.valueOf(decl.nodeIdNamespaceIndex()), UUID.randomUUID());
		String browseName = decl.browseName();
		Map<UnsignedInteger, DataValue> attributes = MapAttributesFactory.buildMapAttributesForObjectType(nodeId, browseName, serverTimeStamp);

		opcUaObject.objectTypeAttributes = new AttributesMap(nodeId, browseName, attributes);
	}

	private void buildAttributesForVariable(Field field) throws IllegalArgumentException, IllegalAccessException {
		OpcUaVariableDeclaration decl = field.getAnnotation(OpcUaVariableDeclaration.class);
		if (decl == null)
			return;

		NodeId nodeId = new NodeId(Integer.valueOf(decl.nodeIdNamespaceIndex()), UUID.randomUUID());
		String browseName = decl.browseName();
		Object value = field.get(opcUaObject.obj);
		NodeId nodeIdForVariableType = Identifiers.String;
		Map<UnsignedInteger, DataValue> attributes = MapAttributesFactory.buildMapAttributesForVariable(nodeId, browseName, value, nodeIdForVariableType, serverTimeStamp);

		opcUaObject.variableAttributes.add(new AttributesMap(nodeId, browseName, attributes));
	}

}
