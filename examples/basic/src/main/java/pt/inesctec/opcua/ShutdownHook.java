package pt.inesctec.opcua;

import java.util.List;

public class ShutdownHook extends Thread {

	private OpcUaClient opcUaClient;
	private List<OpcUaVariablesFetcher> opcUaVariablesFetcherList;

	public ShutdownHook(OpcUaClient opcUaClient, List<OpcUaVariablesFetcher> opcUaVariablesFetcherList) {
		this.opcUaClient = opcUaClient;
		this.opcUaVariablesFetcherList = opcUaVariablesFetcherList;
	}

	@Override
	public void run() {
		System.out.println("Shutdown hook ran!");

		for (OpcUaVariablesFetcher opcUaVariablesFetcher : opcUaVariablesFetcherList) {
			opcUaVariablesFetcher.setShutdown(true);
			while (opcUaVariablesFetcher.isAlive())
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					// ignore
				}
		}

		// Shutdown all OpcUaSession
		opcUaClient.shutdownAllOpcUaSession();

	}
}