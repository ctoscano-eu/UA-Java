package pt.inesctec.opcua;

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

	public String java2Json(OpcUaVariableToRetrieve opcUaVariableToRetrieve) throws JsonProcessingException {
		return mapper.writeValueAsString(opcUaVariableToRetrieve);
	}

	public OpcUaVariableToRetrieve json2OpcUaVariableToRetrieve(String jsonInString) throws IOException {
		JsonNode node = mapper.readTree(jsonInString);
		return OpcUaVariableToRetrieve.jsonToJava(node);
		
		// Following line is another way of getting same result
		// return mapper.readValue(jsonInString, OpcUaVariableToRetrieve.class);
	}

	public List<OpcUaVariableToRetrieve> json2OpcUaVariableToRetrieveList(String jsonInString) throws IOException {

		JsonNode node = mapper.readTree(jsonInString);

		if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
			return OpcUaVariableToRetrieve.jsonArrayToJava(node);
		}
		else if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			List<OpcUaVariableToRetrieve> list = new ArrayList<OpcUaVariableToRetrieve>();
			list.add(OpcUaVariableToRetrieve.jsonToJava(node));
			return list;
		}
		else
			return null;
	}

}
