package pt.inesctec.opcua.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class MongoProperties {

	public String host;
	public String port;
	public String database;
	public String collection;
	public String userName; // may be null
	public String password; // may be null

	public MongoProperties() {
		super();
	}

	public MongoProperties(String host, String port, String database, String collection, String userName, String password) {
		super();
		this.host = host;
		this.port = port;
		this.database = database;
		this.collection = collection;
		this.userName = userName;
		this.password = password;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("host: ");
		buf.append(host);
		buf.append(" port: ");
		buf.append(port);
		buf.append(" database: ");
		buf.append(database);
		buf.append(" collection: ");
		buf.append(collection);
		buf.append(" userName: ");
		buf.append(userName);
		buf.append(" password: ");
		buf.append(password);
		return buf.toString();
	}

	public static MongoProperties jsonToJava(JsonNode node) {
		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			MongoProperties obj = new MongoProperties();
			obj.host = node.get("host").textValue();
			obj.port = node.get("port").textValue();
			obj.database = node.get("database").textValue();
			obj.collection = node.get("collection").textValue();
			obj.userName = (node.get("userName") != null && node.get("userName").textValue().length() > 0) ? node.get("userName").textValue() : null;
			obj.password = (node.get("password") != null && node.get("password").textValue().length() > 0) ? node.get("password").textValue() : null;

			return obj;
		}
		else
			return null;
	}

	public static List<MongoProperties> jsonArrayToJava(JsonNode node) {
		List<MongoProperties> list = new ArrayList<MongoProperties>();

		if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
			Iterator<JsonNode> it = node.iterator();
			while (it.hasNext()) {
				list.add(jsonToJava(it.next()));
			}
		}

		return list;
	}

}
