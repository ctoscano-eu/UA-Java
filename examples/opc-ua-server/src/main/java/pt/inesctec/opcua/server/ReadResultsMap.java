package pt.inesctec.opcua.server;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;

import pt.inesctec.opcua.server.model.OpcUaAddressSpace;

public class ReadResultsMap {

	// The map built by the Server's initialization
	private Map<NodeId, Map<UnsignedInteger, DataValue>> readResultsMap = new HashMap<NodeId, Map<UnsignedInteger, DataValue>>();

	// our address space
	private OpcUaAddressSpace opcUaAddressSpace;

	public void put(NodeId key, Map<UnsignedInteger, DataValue> value) {
		readResultsMap.put(key, value);
	}

	public Map<UnsignedInteger, DataValue> get(NodeId key) {
		// search in the Server's map
		Map<UnsignedInteger, DataValue> map = readResultsMap.get(key);
		if (map != null)
			return map;

		// search on our address space
		return opcUaAddressSpace.getResultsMap(key);
	}

	public boolean containsKey(NodeId key) {
		// search in the Server's map
		boolean f = readResultsMap.containsKey(key);
		if (f)
			return f;

		// search on our address space
		return opcUaAddressSpace.resultsMapContainsKey(key);
	}

	public void setOpcUaAddressSpace(OpcUaAddressSpace opcUaAddressSpace) {
		this.opcUaAddressSpace = opcUaAddressSpace;
	}

}
