package pt.inesctec.opcua.server.model;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;

public class AttributesMap {
	public NodeId nodeId;
	public String browseName;
	public Map<UnsignedInteger, DataValue> attributes = new HashMap<UnsignedInteger, DataValue>();

	public AttributesMap(NodeId nodeId, String browseName, Map<UnsignedInteger, DataValue> attributes) {
		this.nodeId = nodeId;
		this.browseName = browseName;
		this.attributes = attributes;
	}
}
