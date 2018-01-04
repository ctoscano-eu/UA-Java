package pt.inesctec.opcua;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;

import pt.inesctec.opcua.model.OpcUaVariablesToFetch;

public class ApplicationTest {

	@Before
	public void setUp() throws ServiceResultException {
	}

	@After
	public void shutdown() throws ServiceFaultException, ServiceResultException {
	}

	@Test
	public void testApplication() {
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
			
			// Wait for 5 seconds
			Thread.sleep(5000);
			
			// Shutdown all threads
			ShutdownHook shutdownHook = new ShutdownHook(opcUaClient, opcUaVariablesFetcherList);
			shutdownHook.start();
			while (shutdownHook.isAlive())
				Thread.sleep(100);
			
		}
		catch (Throwable t) {
			fail(t.getMessage());
		}

	}

}
