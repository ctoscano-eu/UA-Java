package pt.inesctec.opcua;

import java.io.File;
import java.util.List;

import pt.inesctec.opcua.model.OpcUaVariablesToFetch;

public class Application {

	public static void main(String[] args) throws Exception {

		JsonConverterService jsonConverterService = new JsonConverterService();

		List<OpcUaVariablesToFetch> list = jsonConverterService.json2OpcUaVariableToFetchList(new File("opc-variables-to-fetch.json"));

		// Create OpcUaClient
		OpcUaClient opcUaClient = new OpcUaClient();
		opcUaClient.create("SampleClient");

		// Create one OpcUaSession for each variable (duplicates are ignored)
		for (int i = 0; i < list.size(); ++i) {
			opcUaClient.createOpcUaSession(list.get(i).opcUaProperties);
		}

		// now retrieve each variable value
		for (int i = 0; i < list.size(); ++i) {
			OpcUaVariablesToFetch opcUaVariablesToReadFromServer = list.get(i);

			opcUaVariablesToReadFromServer.dataValues = opcUaClient.readVariableValue(opcUaVariablesToReadFromServer.opcUaProperties.serverUrl,
			    opcUaVariablesToReadFromServer.getOpcUaVariableNames().toArray(new String[0]));
			System.out.println(opcUaVariablesToReadFromServer);
		}

		// Shutdown all OpcUaSession
		opcUaClient.shutdownAllOpcUaSession();
	}

}
