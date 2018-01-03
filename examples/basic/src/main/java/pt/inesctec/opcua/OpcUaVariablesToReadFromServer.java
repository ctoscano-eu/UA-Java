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
	public List<OpcUaVariable> opcUaVariableArray;
	public DataValue[] dataValueArray; // not to be serialized/deserialized on/from Json

	public OpcUaVariablesToReadFromServer() {
		super();
		// TODO Auto-generated constructor stub
	}

	public List<String> getOpcUaVariableNames() {
		List<String> list = new ArrayList<String>();
		for (OpcUaVariable opcUaVariable : opcUaVariableArray)
			list.add(opcUaVariable.opcUaVariable);
		return list;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("opcUaServerUrl: ");
		buf.append(opcUaServerUrl);
		buf.append("mongoDbCollection: ");
		buf.append(mongoDbCollection);
		for (OpcUaVariable opcUaVariable : opcUaVariableArray) {
			buf.append(' ');
			buf.append(opcUaVariableArray.toString());
		}
		for (DataValue dataValue : dataValueArray) {
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
			obj.opcUaVariableArray = OpcUaVariable.jsonArrayToJava(node.get("opcUaVariables"));

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
