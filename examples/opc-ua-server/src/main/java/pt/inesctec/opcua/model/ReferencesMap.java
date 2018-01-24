package pt.inesctec.opcua.model;

import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.core.ReferenceDescription;

public class ReferencesMap {
	public NodeId nodeId;
	public String browseName;
	public ReferenceDescription[] references;

	public ReferencesMap() {
	}

	public ReferencesMap(NodeId nodeId, String browseName, ReferenceDescription[] references) {
		this.nodeId = nodeId;
		this.browseName = browseName;
		this.references = references;
	}
}
