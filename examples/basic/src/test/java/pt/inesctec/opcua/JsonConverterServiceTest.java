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
			String jsonInString = "{ \"serverUrl\" : \"opc.tcp://localhost:4334/UA/teste\", \"variableBrowsePath\" : \"/Objects/Server/ServerArray\"}";

			OpcUaVariableToRetrieve obj = jsonConverterService.json2OpcUaVariableToRetrieve(jsonInString);
			assertEquals("opc.tcp://localhost:4334/UA/teste", obj.serverUrl);
			assertEquals("/Objects/Server/ServerArray", obj.variableBrowsePath);

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testConvertSeveralJsonStrings() {
		try {
			JsonConverterService jsonConverterService = new JsonConverterService();
			String jsonInString = "[" + "{ \"serverUrl\" : \"opc.tcp://localhost:4334/UA/teste\", \"variableBrowsePath\" : \"/Objects/Server/ServerArray\"},"
			    + "{ \"serverUrl\" : \"opc.tcp://localhost:4334/UA/teste\", \"variableBrowsePath\" : \"/Objects/Server/ServerArray\"}" + "]";

			List<OpcUaVariableToRetrieve> list = jsonConverterService.json2OpcUaVariableToRetrieveList(jsonInString);
			assertEquals(2, list.size());
			assertEquals("opc.tcp://localhost:4334/UA/teste", list.get(0).serverUrl);
			assertEquals("/Objects/Server/ServerArray", list.get(0).variableBrowsePath);
			assertEquals("opc.tcp://localhost:4334/UA/teste", list.get(1).serverUrl);
			assertEquals("/Objects/Server/ServerArray", list.get(1).variableBrowsePath);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
