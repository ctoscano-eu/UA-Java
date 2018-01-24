package pt.inesctec.opcua.model;

import java.util.ArrayList;
import java.util.List;

public class OpcUaObject {

	public Object obj;
	public AttributesMap objectAttributes = new AttributesMap();
	public AttributesMap objectTypeAttributes = new AttributesMap();
	public List<AttributesMap> variableAttributes = new ArrayList<AttributesMap>();

	public OpcUaObject() {
		super();
	}

	public OpcUaObject(Object obj) {
		this.obj = obj;
	}
}
