package pt.inesctec.opcua;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHook extends Thread {

	private Logger logger = LoggerFactory.getLogger(ShutdownHook.class);

	private OpcUaClient opcUaClient;
	private List<OpcUaVariablesFetcher> opcUaVariablesFetcherList;

	public ShutdownHook(OpcUaClient opcUaClient, List<OpcUaVariablesFetcher> opcUaVariablesFetcherList) {
		this.opcUaClient = opcUaClient;
		this.opcUaVariablesFetcherList = opcUaVariablesFetcherList;
	}

	@Override
	public void run() {
		logger.info("Shutdown in progress ...");

		for (OpcUaVariablesFetcher opcUaVariablesFetcher : opcUaVariablesFetcherList) {
			opcUaVariablesFetcher.setShutdown(true);
			while (opcUaVariablesFetcher.isAlive())
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					// ignore
				}
			logger.info("OpcUaVariablesFetcher shutdown.");
		}

		// Shutdown all OpcUaSession
		opcUaClient.shutdownAllOpcUaSession();

	}
}