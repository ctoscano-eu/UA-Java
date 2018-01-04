package pt.inesctec.opcua;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Maps;

public class OpcUaSessionList {

	private HashMap<String, OpcUaSession> opcUaSessionList = Maps.newHashMap();

	/*
	 * Get the OpcUaSession to use. 
	 * If serverUrl == null, get the first one from the List of sessions.
	 */
	public OpcUaSession getOpcUaSession(String serverUrl) {
		if (opcUaSessionList.isEmpty()) // there is no session  
			throw new RuntimeException("There is no OpcUaSession.");

		if (serverUrl == null) // use the first Session in the Map
			serverUrl = opcUaSessionList.keySet().iterator().next();

		return opcUaSessionList.get(serverUrl);
	}

	public boolean containsKey(String serverUrl) {
		return opcUaSessionList.containsKey(serverUrl);
	}

	public OpcUaSession get(String serverUrl) {
		return opcUaSessionList.get(serverUrl);
	}

	public OpcUaSession put(String serverUrl, OpcUaSession opcUaSession) {
		return opcUaSessionList.put(serverUrl, opcUaSession);
	}

	public OpcUaSession remove(String serverUrl) {
		return opcUaSessionList.remove(serverUrl);
	}

	public boolean isEmpty() {
		return opcUaSessionList.isEmpty();
	}

	public Set<String> keySet() {
		return opcUaSessionList.keySet();
	}
}
