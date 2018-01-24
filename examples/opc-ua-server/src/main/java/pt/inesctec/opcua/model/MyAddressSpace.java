package pt.inesctec.opcua.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyAddressSpace {

	private final Logger logger = LoggerFactory.getLogger(MyAddressSpace.class);

	public List<OpcUaObject> opcUaObjectList = new ArrayList<OpcUaObject>();

	public Map<NodeId, BrowseResult> browseActionsMap = new HashMap<NodeId, BrowseResult>();
	public Map<NodeId, Map<UnsignedInteger, DataValue>> readResultsMap = new HashMap<NodeId, Map<UnsignedInteger, DataValue>>();

	public MyAddressSpace() {
	}

	public void init() {
		final RoboticManipulator roboticManipulator = new RoboticManipulator("ROBOT_ID", "ROBOT_NAME", "ROBOT_STATUS");
		OpcUaObject opcUaObject = new OpcUaObject(roboticManipulator);
		opcUaObjectList.add(opcUaObject);

		for (OpcUaObject obj : opcUaObjectList)
			try {
				buildReadResultsMap(obj, DateTime.currentTime());
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
	}

	public void buildReadResultsMap(OpcUaObject opcUaObject, DateTime serverTimeStamp) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		buildAttributesForObject(opcUaObject, serverTimeStamp);
		buildAttributesForObjectType(opcUaObject, serverTimeStamp);

		for (Field field : opcUaObject.obj.getClass().getFields()) {
			buildAttributesForVariable(opcUaObject, field, serverTimeStamp);
		}
	}

	private void buildAttributesForVariable(OpcUaObject opcUaObject, Field field, DateTime serverTimeStamp) throws IllegalArgumentException, IllegalAccessException {
		OpcUaVariableDeclaration decl = field.getAnnotation(OpcUaVariableDeclaration.class);
		if (decl == null)
			return;

		NodeId nodeId = new NodeId(Integer.valueOf(decl.nodeIdNamespaceIndex()), UUID.randomUUID());
		String browseName = decl.browseName();
		Object value = field.get(opcUaObject.obj);
		NodeId nodeIdForVariableType = Identifiers.String;
		Map<UnsignedInteger, DataValue> attributes = MapAttributesFactory.buildMapAttributesForVariable(nodeId, browseName, value, nodeIdForVariableType, serverTimeStamp);
		readResultsMap.put(nodeId, attributes);

		opcUaObject.variableAttributes.add(new AttributesMap(nodeId, attributes));
	}

	private void buildAttributesForObject(OpcUaObject opcUaObject, DateTime serverTimeStamp) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		OpcUaObjectDeclaration decl = opcUaObject.obj.getClass().getAnnotation(OpcUaObjectDeclaration.class);
		if (decl == null)
			logger.warn("OpcUaObjectDeclaration not found on " + opcUaObject.obj.toString());

		NodeId nodeId = new NodeId(Integer.valueOf(decl.nodeIdNamespaceIndex()), UUID.randomUUID());
		String browseName = decl.browseName();
		Map<UnsignedInteger, DataValue> attributes = MapAttributesFactory.buildMapAttributesForObject(nodeId, browseName, serverTimeStamp);
		readResultsMap.put(nodeId, attributes);

		opcUaObject.objectAttributes.nodeId = nodeId;
		opcUaObject.objectAttributes.attributes = attributes;
	}

	private void buildAttributesForObjectType(OpcUaObject opcUaObject, DateTime serverTimeStamp) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		OpcUaObjectTypeDeclaration decl = opcUaObject.obj.getClass().getAnnotation(OpcUaObjectTypeDeclaration.class);
		if (decl == null)
			logger.warn("OpcUaObjectTypeDeclaration not found on " + opcUaObject.obj.toString());

		NodeId nodeId = new NodeId(Integer.valueOf(decl.nodeIdNamespaceIndex()), UUID.randomUUID());
		String browseName = decl.browseName();
		Map<UnsignedInteger, DataValue> attributes = MapAttributesFactory.buildMapAttributesForObjectType(nodeId, browseName, serverTimeStamp);
		readResultsMap.put(nodeId, attributes);

		opcUaObject.objectTypeAttributes.nodeId = nodeId;
		opcUaObject.objectTypeAttributes.attributes = attributes;
	}

}
