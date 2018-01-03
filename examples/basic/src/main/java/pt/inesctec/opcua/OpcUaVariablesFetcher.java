package pt.inesctec.opcua;

import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.inesctec.opcua.model.OpcUaVariablesToFetch;

public class OpcUaVariablesFetcher extends Thread {

	private Logger logger = LoggerFactory.getLogger(OpcUaVariablesFetcher.class);

	private OpcUaClient opcUaClient;
	private OpcUaVariablesToFetch opcUaVariablesToFetch;
	private boolean shutdown = false;

	public OpcUaVariablesFetcher(OpcUaClient opcUaClient, OpcUaVariablesToFetch opcUaVariablesToFetch) {
		this.opcUaClient = opcUaClient;
		this.opcUaVariablesToFetch = opcUaVariablesToFetch;
	}

	@Override
	public void run() {
		try {
			while (getShutdown() == false) {
				opcUaVariablesToFetch.dataValues = opcUaClient.readVariableValue(opcUaVariablesToFetch.opcUaProperties.serverUrl, opcUaVariablesToFetch.getOpcUaVariableNames().toArray(new String[0]));

				logger.info(opcUaVariablesToFetch.dataValuesToString());

				Thread.sleep(opcUaVariablesToFetch.fetchCycle);
			}
		}
		catch (ServiceFaultException e) {
			logger.warn(e.getMessage());
		}
		catch (ServiceResultException e) {
			logger.warn(e.getMessage());
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
