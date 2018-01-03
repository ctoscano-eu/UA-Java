package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pt.inesctec.opcua.model.OpcUaVariablesToFetch;

public class JsonConverterServiceTest {

	private JsonConverterService jsonConverterService;

	@Before
	public void setUp() {
		jsonConverterService = new JsonConverterService();
	}

	@After
	public void shutdown() {
	}

	@Test
	public void testConvertOneJsonString() {
		try {
			String jsonInString = "	{\r\n" + 
					"	  \"opcUaProperties\" : {\r\n" + 
					"	    \"serverUrl\": \"opc.tcp://localhost:4334/UA/teste\",\r\n" + 
					"      \"userName\" : \"\",\r\n" + 
					"      \"password\" : \"\"\r\n" + 
					"	  },\r\n" + 
					"		\"mongoProperties\" : {\r\n" + 
					"		  \"host\" : \"localhost\",\r\n" + 
					"		  \"port\" : \"27017\",\r\n" + 
					"		  \"database\" : \"sensor_data_saver\",\r\n" + 
					"      \"collection\" : \"xpto\",\r\n" + 
					"		  \"userName\" : \"\",\r\n" + 
					"		  \"password\" : \"\"\r\n" + 
					"		},\r\n" + 
					"		\"opcUaVariables\": [\r\n" + 
					"			{\r\n" + 
					"				\"name\": \"/Objects/Server/ServerArray\",\r\n" + 
					"				\"type\": \"String\"\r\n" + 
					"			},\r\n" + 
					"			{\r\n" + 
					"				\"name\": \"/Objects/Server/ServerStatus\",\r\n" + 
					"				\"type\": \"String\"\r\n" + 
					"			}\r\n" + 
					"		]\r\n" + 
					"	}\r\n" + 
					"";

			OpcUaVariablesToFetch obj = jsonConverterService.json2OpcUaVariableToFetch(jsonInString);
			assertEquals("opc.tcp://localhost:4334/UA/teste", obj.opcUaProperties.serverUrl);
			assertNull(obj.opcUaProperties.userName);
			assertNull(obj.opcUaProperties.password);
			assertEquals("localhost", obj.mongoProperties.host);
			assertEquals("27017", obj.mongoProperties.port);
			assertEquals("sensor_data_saver", obj.mongoProperties.database);
			assertEquals("xpto", obj.mongoProperties.collection);
			assertNull(obj.mongoProperties.userName);
			assertNull(obj.mongoProperties.password);
			assertEquals(2, obj.opcUaVariables.size());
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testConvertSeveralJsonStrings() {
		try {
			JsonConverterService jsonConverterService = new JsonConverterService();
			String jsonInString = "[\r\n" + 
					"	{\r\n" + 
					"	  \"opcUaProperties\" : {\r\n" + 
					"	    \"serverUrl\": \"opc.tcp://localhost:4334/UA/teste\",\r\n" + 
					"      \"userName\" : \"\",\r\n" + 
					"      \"password\" : \"\"\r\n" + 
					"	  },\r\n" + 
					"		\"mongoProperties\" : {\r\n" + 
					"		  \"host\" : \"localhost\",\r\n" + 
					"		  \"port\" : \"27017\",\r\n" + 
					"		  \"database\" : \"sensor_data_saver\",\r\n" + 
					"      \"collection\" : \"xpto\",\r\n" + 
					"		  \"userName\" : \"\",\r\n" + 
					"		  \"password\" : \"\"\r\n" + 
					"		},\r\n" + 
					"		\"opcUaVariables\": [\r\n" + 
					"			{\r\n" + 
					"				\"name\": \"/Objects/Server/ServerArray\",\r\n" + 
					"				\"type\": \"String\"\r\n" + 
					"			},\r\n" + 
					"			{\r\n" + 
					"				\"name\": \"/Objects/Server/ServerStatus\",\r\n" + 
					"				\"type\": \"String\"\r\n" + 
					"			}\r\n" + 
					"		]\r\n" + 
					"	},\r\n" + 
					"  {\r\n" + 
					"    \"opcUaProperties\" : {\r\n" + 
					"      \"serverUrl\": \"opc.tcp://localhost:4334/UA/teste\",\r\n" + 
					"      \"userName\" : \"\",\r\n" + 
					"      \"password\" : \"\"\r\n" + 
					"    },\r\n" + 
					"    \"mongoProperties\" : {\r\n" + 
					"      \"host\" : \"localhost\",\r\n" + 
					"      \"port\" : \"27017\",\r\n" + 
					"      \"database\" : \"sensor_data_saver\",\r\n" + 
					"      \"collection\" : \"xpto\",\r\n" + 
					"      \"userName\" : \"\",\r\n" + 
					"      \"password\" : \"\"\r\n" + 
					"    },\r\n" + 
					"    \"opcUaVariables\": [\r\n" + 
					"      {\r\n" + 
					"        \"name\": \"/Objects/Server/ServerArray\",\r\n" + 
					"        \"type\": \"String\"\r\n" + 
					"      },\r\n" + 
					"      {\r\n" + 
					"        \"name\": \"/Objects/Server/ServerStatus\",\r\n" + 
					"        \"type\": \"String\"\r\n" + 
					"      }\r\n" + 
					"    ]\r\n" + 
					"  }\r\n" + 
					"]\r\n" + 
					"";

			List<OpcUaVariablesToFetch> list = jsonConverterService.json2OpcUaVariableToFetchList(jsonInString);
			assertEquals(2, list.size());
			for (OpcUaVariablesToFetch obj : list) {
				assertEquals("opc.tcp://localhost:4334/UA/teste", obj.opcUaProperties.serverUrl);
				assertNull(obj.opcUaProperties.userName);
				assertNull(obj.opcUaProperties.password);
				assertEquals("localhost", obj.mongoProperties.host);
				assertEquals("27017", obj.mongoProperties.port);
				assertEquals("sensor_data_saver", obj.mongoProperties.database);
				assertEquals("xpto", obj.mongoProperties.collection);
				assertNull(obj.mongoProperties.userName);
				assertNull(obj.mongoProperties.password);
				assertEquals(2, obj.opcUaVariables.size());
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
