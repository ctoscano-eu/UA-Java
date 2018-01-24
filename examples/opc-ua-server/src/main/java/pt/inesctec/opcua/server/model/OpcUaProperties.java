package pt.inesctec.opcua.server.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class OpcUaProperties {

	public String serverUrl;
	public String userName; // may be null
	public String password; // may be null

	public OpcUaProperties() {
		super();
	}

	public OpcUaProperties(String serverUrl, String userName, String password) {
		super();
		this.serverUrl = serverUrl;
		this.userName = userName;
		this.password = password;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("serverUrl: ");
		buf.append(serverUrl);
		buf.append(" userName: ");
		buf.append(userName);
		buf.append(" password: ");
		buf.append(password);
		return buf.toString();
	}

	public static OpcUaProperties jsonToJava(JsonNode node) {
		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			OpcUaProperties obj = new OpcUaProperties();
			obj.serverUrl = node.get("serverUrl").textValue();
			obj.userName = (node.get("userName") != null && node.get("userName").textValue().length() > 0) ? node.get("userName").textValue() : null;
			obj.password = (node.get("password") != null && node.get("password").textValue().length() > 0) ? node.get("password").textValue() : null;

			return obj;
		}
		else
			return null;
	}

	public static List<OpcUaProperties> jsonArrayToJava(JsonNode node) {
		List<OpcUaProperties> list = new ArrayList<OpcUaProperties>();

		if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
			Iterator<JsonNode> it = node.iterator();
			while (it.hasNext()) {
				list.add(jsonToJava(it.next()));
			}
		}

		return list;
	}

}
