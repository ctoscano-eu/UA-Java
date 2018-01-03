package pt.inesctec.opcua;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class OpcUaVariable {

	public String opcUaVariable;
	public String opcUaVariableType;

	public OpcUaVariable() {
		super();
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("opcUaVariable: ");
		buf.append(opcUaVariable);
		buf.append(" opcUaVariableType: ");
		buf.append(opcUaVariableType);
		return buf.toString();
	}

	public static OpcUaVariable jsonToJava(JsonNode node) {
		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			OpcUaVariable obj = new OpcUaVariable();
			obj.opcUaVariable = node.get("opcUaVariable").textValue();
			obj.opcUaVariableType = node.get("opcUaVariableType").textValue();

			return obj;
		}
		else
			return null;
	}

	public static List<OpcUaVariable> jsonArrayToJava(JsonNode node) {
		List<OpcUaVariable> list = new ArrayList();

		if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
			Iterator<JsonNode> it = node.iterator();
			while (it.hasNext()) {
				list.add(jsonToJava(it.next()));
			}
		}

		return list;
	}

}
