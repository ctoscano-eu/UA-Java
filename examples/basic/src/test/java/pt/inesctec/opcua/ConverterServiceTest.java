package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConverterServiceTest {

	private JsonConverterService jsonConverterService;

	@Before
	public void setUp() {
		jsonConverterService = new JsonConverterService();
	}

	@After
	public void shutdown() {
	}

	@Test
	public void testConvertSimpleString() {
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
	public void testConvertString() {
		try {
			JsonConverterService jsonConverterService = new JsonConverterService();
			String jsonInString = "{ \"serverUrl\" : \"opc.tcp://localhost:4334/UA/teste\", \"variableBrowsePath\" : \"/Objects/Server/ServerArray\"}";

			OpcUaVariableToRetrieve res = jsonConverterService.json2OpcUaVariableToRetrieve(jsonInString);

			List<OpcUaVariableToRetrieve> list = jsonConverterService.json2OpcUaVariableToRetrieveList(jsonInString);
			assertEquals(1, list.size());

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
