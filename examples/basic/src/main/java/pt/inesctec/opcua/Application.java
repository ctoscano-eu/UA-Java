package pt.inesctec.opcua;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.inesctec.opcua.model.OpcUaVariablesToFetch;

public class Application {

	private Logger logger = LoggerFactory.getLogger(Application.class);

	Application() {
		try {
			// read OpcUaVariablesToFetch file
			JsonConverterService jsonConverterService = new JsonConverterService();
			List<OpcUaVariablesToFetch> list = jsonConverterService.json2OpcUaVariableToFetchList(new File("opc-variables-to-fetch.json"));

			// Create OpcUaClient
			OpcUaClient opcUaClient = new OpcUaClient();
			opcUaClient.create("OpcUaApplication");

			// Create one OpcUaSession for each OpcUaVariablesToFetch (duplicates are ignored)
			for (OpcUaVariablesToFetch opcUaVariablesToFetch : list)
				opcUaClient.createOpcUaSession(opcUaVariablesToFetch.opcUaProperties);

			// Now create and start a Fetcher thread for each OpcUaVariablesToFetch
			List<OpcUaVariablesFetcher> opcUaVariablesFetcherList = new ArrayList<OpcUaVariablesFetcher>();
			for (OpcUaVariablesToFetch opcUaVariablesToFetch : list) {
				OpcUaVariablesFetcher opcUaVariablesFetcher = new OpcUaVariablesFetcher(opcUaClient, opcUaVariablesToFetch);
				opcUaVariablesFetcherList.add(opcUaVariablesFetcher);

				opcUaVariablesFetcher.start();
			}

			// Create a ShutdownHook to shutdown all opcUaVariablesFetchers when the Application initiates shutdown
			ShutdownHook shutdownHook = new ShutdownHook(opcUaClient, opcUaVariablesFetcherList);
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
		catch (Throwable t) {
			logger.error(t.getMessage());
		}
	}

	public static void main(String[] args) {
		new Application();
	}
}
