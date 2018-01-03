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

import pt.inesctec.opcua.model.OpcUaVariablesToFetch;

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

	public String java2Json(OpcUaVariablesToFetch opcUaVariableToFetch) throws JsonProcessingException {
		return mapper.writeValueAsString(opcUaVariableToFetch);
	}

	public OpcUaVariablesToFetch json2OpcUaVariableToFetch(String jsonInString) throws IOException {
		JsonNode node = mapper.readTree(jsonInString);
		return OpcUaVariablesToFetch.jsonToJava(node);

		// Following line is another way of getting same result
		// return mapper.readValue(jsonInString, OpcUaVariableToFetch.class);
	}

	public List<OpcUaVariablesToFetch> json2OpcUaVariableToFetchList(String jsonInString) throws IOException {
		JsonNode node = mapper.readTree(jsonInString);
		return jsonNode2OpcUaVariableToFetchList(node);
	}

	public List<OpcUaVariablesToFetch> json2OpcUaVariableToFetchList(File file) throws IOException {
		JsonNode node = mapper.readTree(file);
		return jsonNode2OpcUaVariableToFetchList(node);
	}

	private List<OpcUaVariablesToFetch> jsonNode2OpcUaVariableToFetchList(JsonNode node) throws IOException {
		if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
			return OpcUaVariablesToFetch.jsonArrayToJava(node);
		}
		else if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			List<OpcUaVariablesToFetch> list = new ArrayList<OpcUaVariablesToFetch>();
			list.add(OpcUaVariablesToFetch.jsonToJava(node));
			return list;
		}
		else
			return null;
	}

}
