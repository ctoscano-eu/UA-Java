package pt.inesctec.opcua;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.inesctec.opcua.model.OpcUaVariablesToFetch;

public class OpcUaVariablesFetcher extends Thread {

	private Logger logger = LoggerFactory.getLogger(OpcUaVariablesFetcher.class);

	private OpcUaClient opcUaClient;
	private OpcUaVariablesToFetch opcUaVariablesToFetch;
	private JsonConverterService jsonConverterService = new JsonConverterService();
	private boolean shutdown = false;

	public OpcUaVariablesFetcher(OpcUaClient opcUaClient, OpcUaVariablesToFetch opcUaVariablesToFetch) {
		this.opcUaClient = opcUaClient;
		this.opcUaVariablesToFetch = opcUaVariablesToFetch;
	}

	@Override
	public void run() {
		try {
			while (getShutdown() == false) {
				try {
					opcUaClient.createOpcUaSessionIfNotAvailable(opcUaVariablesToFetch.opcUaProperties);
					opcUaVariablesToFetch.dataValues = opcUaClient.readVariableValue(opcUaVariablesToFetch.opcUaProperties.serverUrl, opcUaVariablesToFetch.getOpcUaVariableNames().toArray(new String[0]));
					logger.info(opcUaVariablesToFetch.dataValuesToString());

					String mongo = jsonConverterService.convertOpcUaVariablesToJson(opcUaVariablesToFetch);
					logger.info(mongo);
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

	synchronized public void setShutdown(boolean flag) {
		shutdown = flag;
	}

	synchronized private boolean getShutdown() {
		return shutdown;
	}

}
