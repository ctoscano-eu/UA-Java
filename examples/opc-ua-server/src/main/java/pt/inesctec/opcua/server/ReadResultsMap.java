package pt.inesctec.opcua.server;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;

public class ReadResultsMap {
	Map<NodeId, Map<UnsignedInteger, DataValue>> onReadResultsMap = new HashMap<NodeId, Map<UnsignedInteger, DataValue>>();

	public void put(NodeId key, Map<UnsignedInteger, DataValue> value) {
		onReadResultsMap.put(key, value);
	}

	public void putAll(Map<NodeId, Map<UnsignedInteger, DataValue>> all) {
		onReadResultsMap.putAll(all);
	}

	public Map<UnsignedInteger, DataValue> get(NodeId key) {
		return onReadResultsMap.get(key);
	}
	
	public boolean containsKey(NodeId key) {
		return onReadResultsMap.containsKey(key);
	}
}
