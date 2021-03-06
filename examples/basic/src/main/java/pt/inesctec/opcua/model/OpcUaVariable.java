package pt.inesctec.opcua.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class OpcUaVariable {

	public String name;
	public String type;

	public OpcUaVariable() {
		super();
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("name: ");
		buf.append(name);
		buf.append(" type: ");
		buf.append(type);
		return buf.toString();
	}

	public static OpcUaVariable jsonToJava(JsonNode node) {
		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			OpcUaVariable obj = new OpcUaVariable();
			obj.name = node.get("name").textValue();
			obj.type = node.get("type").textValue();

			return obj;
		}
		else
			return null;
	}

	public static List<OpcUaVariable> jsonArrayToJava(JsonNode node) {
		List<OpcUaVariable> list = new ArrayList<OpcUaVariable>();

		if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
			Iterator<JsonNode> it = node.iterator();
			while (it.hasNext()) {
				list.add(jsonToJava(it.next()));
			}
		}

		return list;
	}

}
