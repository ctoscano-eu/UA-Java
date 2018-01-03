package pt.inesctec.opcua;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pt.inesctec.opcua.model.OpcUaVariablesToFetch;

public class Application {

	public static void main(String[] args) throws Exception {

		JsonConverterService jsonConverterService = new JsonConverterService();

		List<OpcUaVariablesToFetch> list = jsonConverterService.json2OpcUaVariableToFetchList(new File("opc-variables-to-fetch.json"));

		// Create OpcUaClient
		OpcUaClient opcUaClient = new OpcUaClient();
		opcUaClient.create("SampleClient");

		// Create one OpcUaSession for each OpcUaVariablesToFetch (duplicates are ignored)
		for (OpcUaVariablesToFetch opcUaVariablesToFetch : list)
			opcUaClient.createOpcUaSession(opcUaVariablesToFetch.opcUaProperties);

		// Now create and start a Fetcher thread for each OpcUaVariablesToFetch
		List<OpcUaVariablesFetcher> opcUaVariablesFetcherList = new ArrayList<OpcUaVariablesFetcher>();
		for (OpcUaVariablesToFetch opcUaVariablesToFetch : list) {
			OpcUaVariablesFetcher opcUaVariablesFetcher = new OpcUaVariablesFetcher(opcUaClient, opcUaVariablesToFetch);
			opcUaVariablesFetcherList.add(opcUaVariablesFetcher);

			opcUaVariablesFetcher.run();
		}

		// Create a ShutdownHook to shutdown all opcUaVariablesFetchers when the Application initiates shutdown
		ShutdownHook shutdownHook = new ShutdownHook(opcUaClient, opcUaVariablesFetcherList);
		Runtime.getRuntime().addShutdownHook(shutdownHook);

	}

}
