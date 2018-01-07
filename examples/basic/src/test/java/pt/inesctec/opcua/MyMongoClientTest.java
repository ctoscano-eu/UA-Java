package pt.inesctec.opcua;

import static org.junit.Assert.assertNotNull;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pt.inesctec.opcua.model.MongoProperties;

public class MyMongoClientTest {

	private MyMongoClient myMongoClient = new MyMongoClient();

	@Before
	public void setUp() {
		MongoProperties mongoProperties = new MongoProperties("localhost", "27017", "testdb", "test-collection", null, null);
		myMongoClient.create(mongoProperties);

		assertNotNull(myMongoClient.mongoTemplate);
		assertNotNull(myMongoClient.mongoTemplate.getDb());

		myMongoClient.dropCollection();
	}

	@After
	public void shutdown() {
		myMongoClient.dropCollection();
	}

	@Test
	public void testSomeOperations() {
		try {
			assertNotNull(myMongoClient.createCollection());

			Document document = myMongoClient.insertItemInCollection("{\r\n" + "  \"varName\" : \"varValue\"\r\n" + "}");
			assertNotNull(document);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
