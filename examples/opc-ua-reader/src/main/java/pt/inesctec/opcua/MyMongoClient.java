package pt.inesctec.opcua;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;

import pt.inesctec.opcua.model.MongoProperties;

public class MyMongoClient {

	private Logger logger = LoggerFactory.getLogger(MyMongoClient.class);

	private MongoProperties mongoProperties;
	private MongoClientURI mongoClientURI;
	private MongoClient MongoClient;

	public MongoTemplate mongoTemplate;

	public MyMongoClient() {
	}

	public MongoTemplate create(MongoProperties mongoProperties) {
		this.mongoProperties = mongoProperties;

		mongoClientURI();

		MongoClient = new MongoClient(mongoClientURI);
		mongoTemplate = new MongoTemplate(MongoClient, mongoProperties.database);

		return mongoTemplate;
	}

	private void mongoClientURI() {
		// mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database][?options]]
		StringBuffer buf = new StringBuffer();
		buf.append("mongodb://");
		if (mongoProperties.userName != null && mongoProperties.userName.length() > 0) {
			buf.append(mongoProperties.userName);
			buf.append(":");
			buf.append(mongoProperties.password);
			buf.append("@");
		}
		buf.append(mongoProperties.host);
		buf.append(":");
		buf.append(mongoProperties.port);
		buf.append("/");
		buf.append(mongoProperties.database);

		mongoClientURI = new MongoClientURI(buf.toString());
	}

	public MongoCollection createCollection() {
		if (!mongoTemplate.collectionExists(mongoProperties.collection))
			return mongoTemplate.createCollection(mongoProperties.collection);
		else
			return mongoTemplate.getCollection(mongoProperties.collection);
	}

	public void dropCollection() {
		if (mongoTemplate.collectionExists(mongoProperties.collection))
			mongoTemplate.dropCollection(mongoProperties.collection);
	}

	public Document insertItemInCollection(String collection, String jsonItem) {
		StringBuffer mongoCommand = new StringBuffer();
		mongoCommand.append("{ \"insert\" : \"");
		mongoCommand.append(collection);
		mongoCommand.append("\",");
		mongoCommand.append("  \"documents\" : [ ");
		mongoCommand.append(jsonItem);
		mongoCommand.append("]}");

		return executeJsonCommand(mongoCommand.toString());
	}

	public Document insertItemInCollection(String jsonItem) {
		StringBuffer mongoCommand = new StringBuffer();
		mongoCommand.append("{ \"insert\" : \"test-collection\",");
		mongoCommand.append("  \"documents\" : [ ");
		mongoCommand.append(jsonItem);
		mongoCommand.append("]}");

		return executeJsonCommand(mongoCommand.toString());
	}

	public Document executeJsonCommand(String jsonCommand) {
		Document doc = mongoTemplate.executeCommand(jsonCommand);
		logger.info("Mongo command executed: " + "OK: " + doc.getDouble("ok").doubleValue() + " " + jsonCommand );
		return doc;
	}
}
