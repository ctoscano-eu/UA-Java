package pt.inesctec.opcua;

public class OpcUaVariableToRetrieve {

	public String serverUrl;
	public String variableBrowsePath;
	
	public OpcUaVariableToRetrieve() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public OpcUaVariableToRetrieve(String serverUrl, String variableBrowsePath) {
		super();
		this.serverUrl = serverUrl;
		this.variableBrowsePath = variableBrowsePath;
	}

}
