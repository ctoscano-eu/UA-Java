package pt.inesctec.opcua.model;

import java.util.ArrayList;
import java.util.List;

import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReferenceDescription;

public class OpcUaObjectReferencesBuilder {

	private OpcUaObject opcUaObject;

	public OpcUaObjectReferencesBuilder(OpcUaObject opcUaObject) {
		this.opcUaObject = opcUaObject;
	}

	public void buildReferences() {
		opcUaObject.referenceToObject = buildReferenceToObject();
		opcUaObject.objectReferences = buildReferencesForObject();
		opcUaObject.objectTypeReferences = buildReferencesForObjectType();
		opcUaObject.variableReferences = buildReferencesForVariables();
	}

	private ReferenceDescription buildReferenceToObject() {
		return new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(opcUaObject.objectAttributes.nodeId), qualifiedName(opcUaObject.objectAttributes.browseName),
		    new LocalizedText(opcUaObject.objectAttributes.browseName), NodeClass.Object, new ExpandedNodeId(opcUaObject.objectTypeAttributes.nodeId));
	}

	private ReferenceDescription[] buildReferencesForObject() {
		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();

		list.add(new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(opcUaObject.objectTypeAttributes.nodeId), qualifiedName("RoboticManipulatorType"),
		    new LocalizedText("RoboticManipulatorType"), NodeClass.ObjectType, new ExpandedNodeId(opcUaObject.objectTypeAttributes.nodeId)));

		for (AttributesMap map : opcUaObject.variableAttributes) {
			NodeId nodeId = map.nodeId;
			String browseName = map.browseName;
			list.add(new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(nodeId), qualifiedName(browseName), new LocalizedText(browseName), NodeClass.Variable,
			    new ExpandedNodeId(nodeId)));
		}

		return list.toArray(new ReferenceDescription[list.size()]);
	}

	private ReferenceDescription[] buildReferencesForObjectType() {
		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();

		list.add(new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(opcUaObject.objectAttributes.nodeId), qualifiedName(opcUaObject.objectAttributes.browseName),
		    new LocalizedText(opcUaObject.objectAttributes.browseName), NodeClass.Variable, new ExpandedNodeId(opcUaObject.objectAttributes.nodeId)));

		return list.toArray(new ReferenceDescription[list.size()]);
	}

	private ReferencesMap[] buildReferencesForVariables() {
		List<ReferencesMap> list = new ArrayList<ReferencesMap>();

		for (AttributesMap map : opcUaObject.variableAttributes) {
			list.add(buildReferencesForVariable(map.nodeId, map.browseName));
		}

		return list.toArray(new ReferencesMap[list.size()]);
	}

	private ReferencesMap buildReferencesForVariable(NodeId nodeId, String browseName) {
		List<ReferenceDescription> list = new ArrayList<ReferenceDescription>();

		list.add(new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(opcUaObject.objectAttributes.nodeId), qualifiedName(opcUaObject.objectAttributes.browseName),
		    new LocalizedText("RoboticManipulator"), NodeClass.Variable, new ExpandedNodeId(opcUaObject.objectAttributes.nodeId)));
		list.add(new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.BaseDataVariableType), new QualifiedName("BaseDataVariableType"),
		    new LocalizedText("BaseDataVariableType"), NodeClass.VariableType, new ExpandedNodeId(Identifiers.BaseDataVariableType)));

		return new ReferencesMap(nodeId, browseName, list.toArray(new ReferenceDescription[list.size()]));
	}

	static private QualifiedName qualifiedName(String name) {
		return new QualifiedName(1, name);
	}

}
