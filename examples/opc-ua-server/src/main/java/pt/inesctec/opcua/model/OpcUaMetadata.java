package pt.inesctec.opcua.model;

import org.opcfoundation.ua.builtintypes.NodeId;

public class OpcUaMetadata {

	class OpcUa {
		String nodeIdForObject = null;
		String browseNameForObject = null;
		String nodeIdForType = null;
		String browseNameForType = null;
		String[] fieldForVariable = null;
		NodeId[] nodeIdForVariableType = null;
		String[] browseNameForVariable = null;
		String[] nodeIdForVariable = null;
	};

	public OpcUa opcUa = new OpcUa();

}
