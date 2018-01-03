package pt.inesctec.opcua;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opcfoundation.ua.builtintypes.DataValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class OpcUaVariablesToReadFromServer {

	public String opcUaServerUrl;
	public String mongoDbCollection;
	public List<OpcUaVariable> opcUaVariables;
	public DataValue[] dataValues; // not to be serialized/deserialized on/from Json

	public OpcUaVariablesToReadFromServer() {
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
		buf.append("opcUaServerUrl: ");
		buf.append(opcUaServerUrl);
		buf.append("mongoDbCollection: ");
		buf.append(mongoDbCollection);
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

	public static OpcUaVariablesToReadFromServer jsonToJava(JsonNode node) {
		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			OpcUaVariablesToReadFromServer obj = new OpcUaVariablesToReadFromServer();
			obj.opcUaServerUrl = node.get("opcUaServerUrl").textValue();
			obj.mongoDbCollection = node.get("mongoDbCollection").textValue();
			obj.opcUaVariables = OpcUaVariable.jsonArrayToJava(node.get("opcUaVariables"));

			return obj;
		}
		else
			return null;
	}

	public static List<OpcUaVariablesToReadFromServer> jsonArrayToJava(JsonNode node) {
		List<OpcUaVariablesToReadFromServer> list = new ArrayList();

		if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
			Iterator<JsonNode> it = node.iterator();
			while (it.hasNext()) {
				list.add(jsonToJava(it.next()));
			}
		}

		return list;
	}

}
