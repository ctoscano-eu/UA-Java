package pt.inesctec.opcua;

import java.io.File;
import java.util.List;

import org.opcfoundation.ua.builtintypes.DataValue;

public class Application {

	public static void main(String[] args) throws Exception {

		JsonConverterService jsonConverterService = new JsonConverterService();

		List<OpcUaVariableToRetrieve> list = jsonConverterService.json2OpcUaVariableToRetrieveList(new File("opc-variables-to-retrieve.json"));

		// Create OpcUaClient
		OpcUaClient opcUaClient = new OpcUaClient();
		opcUaClient.create("SampleClient");

		// Create one OpcUaSession for each variable (duplicates are ignored)
		for (int i = 0; i < list.size(); ++i) {
			opcUaClient.createOpcUaSession(list.get(i).serverUrl);
		}

		// now retrieve each variable value
		for (int i = 0; i < list.size(); ++i) {
			OpcUaVariableToRetrieve opcUaVariable = list.get(i);

			DataValue[] dataValueArray = opcUaClient.readVariableValue(opcUaVariable.serverUrl, opcUaVariable.variableBrowsePath);

		}

		// Shutdown all OpcUaSession
		opcUaClient.shutdownAllOpcUaSession();
	}

}
