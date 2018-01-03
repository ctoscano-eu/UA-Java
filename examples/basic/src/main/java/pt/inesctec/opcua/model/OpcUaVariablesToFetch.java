package pt.inesctec.opcua.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opcfoundation.ua.builtintypes.DataValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class OpcUaVariablesToFetch {

	public OpcUaProperties opcUaProperties;
	public MongoProperties mongoProperties;
	public List<OpcUaVariable> opcUaVariables;
	public DataValue[] dataValues; // not to be serialized/deserialized on/from Json

	public OpcUaVariablesToFetch() {
		super();
	}

	public List<String> getOpcUaVariableNames() {
		List<String> list = new ArrayList<String>();
		for (OpcUaVariable opcUaVariable : opcUaVariables)
			list.add(opcUaVariable.name);
		return list;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(opcUaProperties.toString());
		buf.append(mongoProperties.toString());
		for (OpcUaVariable opcUaVariable : opcUaVariables) {
			buf.append(' ');
			buf.append(opcUaVariable.toString());
		}
		for (DataValue dataValue : dataValues) {
			buf.append(' ');
			buf.append(dataValue.toString());
		}
		return buf.toString();
	}

	public static OpcUaVariablesToFetch jsonToJava(JsonNode node) {
		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			OpcUaVariablesToFetch obj = new OpcUaVariablesToFetch();
			obj.opcUaProperties = OpcUaProperties.jsonToJava(node.get("opcUaProperties"));
			obj.mongoProperties = MongoProperties.jsonToJava(node.get("mongoProperties"));
			obj.opcUaVariables = OpcUaVariable.jsonArrayToJava(node.get("opcUaVariables"));

			return obj;
		}
		else
			return null;
	}

	public static List<OpcUaVariablesToFetch> jsonArrayToJava(JsonNode node) {
		List<OpcUaVariablesToFetch> list = new ArrayList<OpcUaVariablesToFetch>();

		if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
			Iterator<JsonNode> it = node.iterator();
			while (it.hasNext()) {
				list.add(jsonToJava(it.next()));
			}
		}

		return list;
	}

}
