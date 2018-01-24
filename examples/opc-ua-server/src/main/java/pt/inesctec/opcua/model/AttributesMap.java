package pt.inesctec.opcua.model;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;

public class AttributesMap {
	public NodeId nodeId;
	public Map<UnsignedInteger, DataValue> attributes = new HashMap<UnsignedInteger, DataValue>();

	public AttributesMap() {
	}

	public AttributesMap(NodeId nodeId, Map<UnsignedInteger, DataValue> attributes) {
		this.nodeId = nodeId;
		this.attributes = attributes;
	}
}
