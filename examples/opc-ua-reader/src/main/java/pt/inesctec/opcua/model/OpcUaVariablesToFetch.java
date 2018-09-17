package pt.inesctec.opcua.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.StatusCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OpcUaVariablesToFetch {

	public OpcUaProperties opcUaProperties;
	public MongoProperties mongoProperties;
	public long fetchCycle; // milliseconds
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
		buf.append(" fetchCycle: ");
		buf.append("fetchCycle");
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

	public String dataValuesToString() {
		StringBuffer buf = new StringBuffer();
		for (DataValue dataValue : dataValues) {
			buf.append(dataValue.toString());
			buf.append("\r\n");
		}
		return buf.toString();
	}

	public static OpcUaVariablesToFetch jsonToJava(JsonNode node) {
		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			OpcUaVariablesToFetch obj = new OpcUaVariablesToFetch();
			obj.opcUaProperties = OpcUaProperties.jsonToJava(node.get("opcUaProperties"));
			obj.mongoProperties = MongoProperties.jsonToJava(node.get("mongoProperties"));
			obj.opcUaVariables = OpcUaVariable.jsonArrayToJava(node.get("opcUaVariables"));
			obj.fetchCycle = node.get("fetchCycle").longValue();

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

	public ObjectNode getOpcUaVariablesAsJsonNode() {
		JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);

		ObjectNode jsonNode = jsonNodeFactory.objectNode();
		for (int i = 0; i < opcUaVariables.size(); ++i) {
			if (dataValues[i] != null && dataValues[i].getValue() != null && dataValues[i].getStatusCode().equals(StatusCode.GOOD)) {
				jsonNode.put(opcUaVariables.get(i).mongoFieldName, dataValues[i].getValue().toString());
				if (opcUaVariables.get(i).mongoFieldNameTimeStamp != null)
					jsonNode.put(opcUaVariables.get(i).mongoFieldNameTimeStamp, dataValues[i].getSourceTimestamp().toString());
			}
			else {
				jsonNode.put(opcUaVariables.get(i).mongoFieldName, ""); // TODO ctoscano if we don't have any value .....
				if (opcUaVariables.get(i).mongoFieldNameTimeStamp != null)
					jsonNode.put(opcUaVariables.get(i).mongoFieldNameTimeStamp, ""); // TODO ctoscano if we don't have any value .....
			}
		}

		return jsonNode;
	}

}
