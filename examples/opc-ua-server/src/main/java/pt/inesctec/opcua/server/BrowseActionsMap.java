package pt.inesctec.opcua.server;

import java.util.HashMap;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.core.BrowseResult;

import pt.inesctec.opcua.server.model.OpcUaAddressSpace;

public class BrowseActionsMap {

	//The map built by the Server's initialization
	private Map<NodeId, BrowseResult> browseActionsMap = new HashMap<NodeId, BrowseResult>();

	// our address space
	private OpcUaAddressSpace opcUaAddressSpace;

	public void put(NodeId key, BrowseResult value) {
		browseActionsMap.put(key, value);
	}

	public BrowseResult get(NodeId key) {
		// search in the Server's map
		BrowseResult res = browseActionsMap.get(key);
		if (res != null)
			return res;

		// search on our address space
		return opcUaAddressSpace.getBrowseActionsMap(key);
	}

	public boolean containsKey(NodeId key) {
		// search in the Server's map
		boolean f = browseActionsMap.containsKey(key);
		if (f)
			return f;

		// search on our address space
		return opcUaAddressSpace.browseActionsMapContainsKey(key);
	}

	public void setOpcUaAddressSpace(OpcUaAddressSpace opcUaAddressSpace) {
		this.opcUaAddressSpace = opcUaAddressSpace;
	}

}
