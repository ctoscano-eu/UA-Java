package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
			String jsonInString = "{" + "		\"opcUaServerUrl\": \"opc.tcp://localhost:4334/UA/teste\"," + "		\"mongoDbCollection\": \"xpto\"," + "		\"opcUaVariables\": [" + "			{"
			    + "				\"opcUaVariable\": \"/Objects/Server/ServerArray\"," + "				\"opcUaVariableType\": \"String\"" + "			}," + "			{"
			    + "				\"opcUaVariable\": \"/Objects/Server/ServerStatus\"," + "				\"opcUaVariableType\": \"String\"" + "			}" + "		]" + "	}";

			OpcUaVariablesToReadFromServer obj = jsonConverterService.json2OpcUaVariableToRetrieve(jsonInString);
			assertEquals("opc.tcp://localhost:4334/UA/teste", obj.opcUaServerUrl);
			assertEquals("xpto", obj.mongoDbCollection);
			assertEquals(2, obj.opcUaVariableArray.size());
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testConvertSeveralJsonStrings() {
		try {
			JsonConverterService jsonConverterService = new JsonConverterService();
			String jsonInString = "[" + "	{" + "		\"opcUaServerUrl\": \"opc.tcp://localhost:4334/UA/teste\"," + "		\"mongoDbCollection\": \"xpto\"," + "		\"opcUaVariables\": [" + "			{"
			    + "				\"opcUaVariable\": \"/Objects/Server/ServerArray\"," + "				\"opcUaVariableType\": \"String\"" + "			}," + "			{"
			    + "				\"opcUaVariable\": \"/Objects/Server/ServerStatus\"," + "				\"opcUaVariableType\": \"String\"" + "			}" + "		]" + "	}," + "  {"
			    + "    \"opcUaServerUrl\": \"opc.tcp://localhost:4334/UA/teste\"," + "    \"mongoDbCollection\": \"xpto\"," + "    \"opcUaVariables\": [" + "      {"
			    + "        \"opcUaVariable\": \"/Objects/Server/ServerArray\"," + "        \"opcUaVariableType\": \"String\"" + "      }," + "      {"
			    + "        \"opcUaVariable\": \"/Objects/Server/ServerStatus\"," + "        \"opcUaVariableType\": \"String\"" + "      }" + "    ]" + "  }" + "]" + "";

			List<OpcUaVariablesToReadFromServer> list = jsonConverterService.json2OpcUaVariableToRetrieveList(jsonInString);
			assertEquals(2, list.size());
			for (OpcUaVariablesToReadFromServer opcUaVariablesToReadFromServer : list) {
				assertEquals("opc.tcp://localhost:4334/UA/teste", opcUaVariablesToReadFromServer.opcUaServerUrl);
				assertEquals("xpto", opcUaVariablesToReadFromServer.mongoDbCollection);
				assertEquals(2, opcUaVariablesToReadFromServer.opcUaVariableArray.size());
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
