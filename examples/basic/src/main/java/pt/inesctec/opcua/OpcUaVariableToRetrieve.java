package pt.inesctec.opcua;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class OpcUaVariableToRetrieve {

	public String serverUrl;
	public String variableBrowsePath;

	public OpcUaVariableToRetrieve() {
		super();
		// TODO Auto-generated constructor stub
	}

	public OpcUaVariableToRetrieve(String serverUrl, String variableBrowsePath) {
		super();
		this.serverUrl = serverUrl;
		this.variableBrowsePath = variableBrowsePath;
	}

	public static OpcUaVariableToRetrieve jsonToJava(JsonNode node) {
		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			OpcUaVariableToRetrieve obj = new OpcUaVariableToRetrieve();
			obj.serverUrl = node.get("serverUrl").textValue();
			obj.variableBrowsePath = node.get("variableBrowsePath").textValue();

			return obj;
		}
		else
			return null;
	}

	public static List<OpcUaVariableToRetrieve> jsonArrayToJava(JsonNode node) {
		List<OpcUaVariableToRetrieve> list = new ArrayList();

		if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
			Iterator<JsonNode> it = node.iterator();
			while (it.hasNext()) {
				list.add(jsonToJava(it.next()));
			}
		}

		return list;
	}
}
