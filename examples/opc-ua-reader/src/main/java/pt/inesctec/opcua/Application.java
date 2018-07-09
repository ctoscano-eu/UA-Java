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

			// Now create and start a Fetcher thread for each OpcUaVariablesToFetch
			List<OpcUaVariablesFetcher> opcUaVariablesFetcherList = new ArrayList<OpcUaVariablesFetcher>();
			for (OpcUaVariablesToFetch opcUaVariablesToFetch : list) {
				// Create MongoClient
				MyMongoClient myMongoClient = new MyMongoClient();
				myMongoClient.create(opcUaVariablesToFetch.mongoProperties);

				OpcUaVariablesFetcher opcUaVariablesFetcher = new OpcUaVariablesFetcher(opcUaClient, myMongoClient, opcUaVariablesToFetch);
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
