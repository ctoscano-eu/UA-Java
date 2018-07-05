package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.Variant;

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
			String jsonInString = "{\r\n" + "		\"opcUaProperties\": {\r\n" + "			\"serverUrl\": \"opc.tcp://localhost:4334/UA/teste\",\r\n" + "			\"userName\": \"\",\r\n"
			    + "			\"password\": \"\"\r\n" + "		},\r\n" + "		\"mongoProperties\": {\r\n" + "			\"host\": \"localhost\",\r\n" + "			\"port\": \"27017\",\r\n"
			    + "			\"database\": \"sensor_data_saver\",\r\n" + "			\"collection\": \"xpto\",\r\n" + "			\"userName\": \"\",\r\n" + "			\"password\": \"\"\r\n" + "		},\r\n"
			    + "		\"fetchCycle\": \"1000\",\r\n" + "		\"opcUaVariables\": [\r\n" + "			{\r\n" + "				\"name\": \"/0/Objects/0/Server/0/ServerArray\",\r\n" + "				\"type\": \"String\",\r\n"
			    + "				\"mongoFieldName\": \"ObjectsServerArray\"\r\n" + "			},\r\n" + "			{\r\n" + "				\"name\": \"/0/Objects/0/Server/0/NamespaceArray\",\r\n"
			    + "				\"type\": \"String\",\r\n" + "				\"mongoFieldName\": \"ObjectsServerNamespaceArray\"\r\n" + "			}\r\n" + "		]\r\n" + "	}";

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
			String jsonInString = "[\r\n" + "	{\r\n" + "		\"opcUaProperties\": {\r\n" + "			\"serverUrl\": \"opc.tcp://localhost:4334/UA/teste\",\r\n" + "			\"userName\": \"\",\r\n"
			    + "			\"password\": \"\"\r\n" + "		},\r\n" + "		\"mongoProperties\": {\r\n" + "			\"host\": \"localhost\",\r\n" + "			\"port\": \"27017\",\r\n"
			    + "			\"database\": \"sensor_data_saver\",\r\n" + "			\"collection\": \"xpto\",\r\n" + "			\"userName\": \"\",\r\n" + "			\"password\": \"\"\r\n" + "		},\r\n"
			    + "		\"fetchCycle\": \"1000\",\r\n" + "		\"opcUaVariables\": [\r\n" + "			{\r\n" + "				\"name\": \"/0/Objects/0/Server/0/ServerArray\",\r\n" + "				\"type\": \"String\",\r\n"
			    + "				\"mongoFieldName\": \"ObjectsServerArray\"\r\n" + "			},\r\n" + "			{\r\n" + "				\"name\": \"/0/Objects/0/Server/0/NamespaceArray\",\r\n"
			    + "				\"type\": \"String\",\r\n" + "				\"mongoFieldName\": \"ObjectsServerNamespaceArray\"\r\n" + "			}\r\n" + "		]\r\n" + "	},\r\n" + "	{\r\n"
			    + "		\"opcUaProperties\": {\r\n" + "			\"serverUrl\": \"opc.tcp://localhost:4334/UA/teste\",\r\n" + "			\"userName\": \"\",\r\n" + "			\"password\": \"\"\r\n" + "		},\r\n"
			    + "		\"mongoProperties\": {\r\n" + "			\"host\": \"localhost\",\r\n" + "			\"port\": \"27017\",\r\n" + "			\"database\": \"sensor_data_saver\",\r\n"
			    + "			\"collection\": \"xpto\",\r\n" + "			\"userName\": \"\",\r\n" + "			\"password\": \"\"\r\n" + "		},\r\n" + "		\"fetchCycle\": \"1000\",\r\n" + "		\"opcUaVariables\": [\r\n"
			    + "			{\r\n" + "				\"name\": \"/0/Objects/0/Server/0/ServiceLevel\",\r\n" + "				\"type\": \"String\",\r\n" + "				\"mongoFieldName\": \"ObjectsServerServiceLevel\"\r\n"
			    + "			},\r\n" + "			{\r\n" + "				\"name\": \"/0/Objects/0/Server/0/Auditing\",\r\n" + "				\"type\": \"String\",\r\n"
			    + "				\"mongoFieldName\": \"ObjectsServerAuditing\"\r\n" + "			}\r\n" + "		]\r\n" + "	}\r\n" + "]\r\n" + "";

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

	@Test
	public void testOpcUaVariablesToJsonConvertion() {
		try {
			String jsonInString = "{\r\n" + "		\"opcUaProperties\": {\r\n" + "			\"serverUrl\": \"opc.tcp://localhost:4334/UA/teste\",\r\n" + "			\"userName\": \"\",\r\n"
			    + "			\"password\": \"\"\r\n" + "		},\r\n" + "		\"mongoProperties\": {\r\n" + "			\"host\": \"localhost\",\r\n" + "			\"port\": \"27017\",\r\n"
			    + "			\"database\": \"sensor_data_saver\",\r\n" + "			\"collection\": \"xpto\",\r\n" + "			\"userName\": \"\",\r\n" + "			\"password\": \"\"\r\n" + "		},\r\n"
			    + "		\"fetchCycle\": \"1000\",\r\n" + "		\"opcUaVariables\": [\r\n" + "			{\r\n" + "				\"name\": \"/0/Objects/0/Server/0/ServerArray\",\r\n" + "				\"type\": \"String\",\r\n"
			    + "				\"mongoFieldName\": \"ObjectsServerArray\"\r\n" + "			},\r\n" + "			{\r\n" + "				\"name\": \"/0/Objects/0/Server/0/NamespaceArray\",\r\n"
			    + "				\"type\": \"String\",\r\n" + "				\"mongoFieldName\": \"ObjectsServerNamespaceArray\"\r\n" + "			}\r\n" + "		]\r\n" + "	}";

			OpcUaVariablesToFetch obj = jsonConverterService.json2OpcUaVariableToFetch(jsonInString);

			// Build dataValue for each OpcUaVariable
			obj.dataValues = new DataValue[obj.opcUaVariables.size()];
			for (int i = 0; i < obj.opcUaVariables.size(); ++i) {
				obj.dataValues[i] = new DataValue(new Variant(obj.opcUaVariables.get(i).name + "Value"));
			}

			String opcUaVariables = jsonConverterService.convertOpcUaVariablesToJson(obj);
			assertNotNull(opcUaVariables);
			assertTrue(opcUaVariables.length() > 0);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
