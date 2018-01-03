package pt.inesctec.opcua;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeType;

/*
 * standard Data forms: "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS", "EEE, dd MMM yyyy HH:mm:ss zzz", "yyyy-MM-dd"
 */
public class JsonConverterService {

	private ObjectMapper mapper;

	public JsonConverterService() {
		super();
		mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
		SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		mapper.setDateFormat(outputFormat);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
	}

	public String java2Json(OpcUaVariablesToReadFromServer opcUaVariableToRetrieve) throws JsonProcessingException {
		return mapper.writeValueAsString(opcUaVariableToRetrieve);
	}

	public OpcUaVariablesToReadFromServer json2OpcUaVariableToRetrieve(String jsonInString) throws IOException {
		JsonNode node = mapper.readTree(jsonInString);
		return OpcUaVariablesToReadFromServer.jsonToJava(node);

		// Following line is another way of getting same result
		// return mapper.readValue(jsonInString, OpcUaVariableToRetrieve.class);
	}

	public List<OpcUaVariablesToReadFromServer> json2OpcUaVariableToRetrieveList(String jsonInString) throws IOException {
		JsonNode node = mapper.readTree(jsonInString);
		return jsonNode2OpcUaVariableToRetrieveList(node);
	}

	public List<OpcUaVariablesToReadFromServer> json2OpcUaVariableToRetrieveList(File file) throws IOException {
		JsonNode node = mapper.readTree(file);
		return jsonNode2OpcUaVariableToRetrieveList(node);
	}

	private List<OpcUaVariablesToReadFromServer> jsonNode2OpcUaVariableToRetrieveList(JsonNode node) throws IOException {
		if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
			return OpcUaVariablesToReadFromServer.jsonArrayToJava(node);
		}
		else if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			List<OpcUaVariablesToReadFromServer> list = new ArrayList<OpcUaVariablesToReadFromServer>();
			list.add(OpcUaVariablesToReadFromServer.jsonToJava(node));
			return list;
		}
		else
			return null;
	}

}
