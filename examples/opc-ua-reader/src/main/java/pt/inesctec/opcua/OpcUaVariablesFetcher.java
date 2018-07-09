package pt.inesctec.opcua;

import java.io.IOException;

import org.opcfoundation.ua.common.ServiceResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.inesctec.opcua.model.OpcUaVariablesToFetch;

public class OpcUaVariablesFetcher extends Thread {

	private Logger logger = LoggerFactory.getLogger(OpcUaVariablesFetcher.class);

	private OpcUaClient opcUaClient;
	private MyMongoClient myMongoClient;
	private OpcUaVariablesToFetch opcUaVariablesToFetch;
	private JsonConverterService jsonConverterService = new JsonConverterService();
	private boolean shutdown = false;

	public OpcUaVariablesFetcher(OpcUaClient opcUaClient, MyMongoClient myMongoClient, OpcUaVariablesToFetch opcUaVariablesToFetch) {
		this.opcUaClient = opcUaClient;
		this.myMongoClient = myMongoClient;
		this.opcUaVariablesToFetch = opcUaVariablesToFetch;
	}

	@Override
	public void run() {
		try {
			while (getShutdown() == false) {
				try {
					fetchVariables();
					saveVariablesOnMongoDb();
				}
				catch (Throwable e) {
					logger.warn(e.getMessage());
				}

				Thread.sleep(opcUaVariablesToFetch.fetchCycle);
			}
		}
		catch (InterruptedException e) {
			logger.warn(e.getMessage());
		}
	}

	private void fetchVariables() throws ServiceResultException {
		opcUaClient.createOpcUaSessionIfNotAvailable(opcUaVariablesToFetch.opcUaProperties);
		opcUaVariablesToFetch.dataValues = opcUaClient.readVariableValue(opcUaVariablesToFetch.opcUaProperties.serverUrl, opcUaVariablesToFetch.getOpcUaVariableNames().toArray(new String[0]));
		logger.info(opcUaVariablesToFetch.dataValuesToString());
	}

	private void saveVariablesOnMongoDb() throws IOException {
		String jsonOpcVariables = jsonConverterService.convertOpcUaVariablesToJson(opcUaVariablesToFetch);

		myMongoClient.insertItemInCollection(opcUaVariablesToFetch.mongoProperties.collection, jsonOpcVariables);
	}

	synchronized public void setShutdown(boolean flag) {
		shutdown = flag;
	}

	synchronized private boolean getShutdown() {
		return shutdown;
	}

}
