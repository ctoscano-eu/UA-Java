package pt.inesctec.opcua.model;

import java.util.ArrayList;
import java.util.List;

import org.opcfoundation.ua.core.ReferenceDescription;

public class OpcUaObject {

	public Object obj;
	public AttributesMap objectAttributes;
	public AttributesMap objectTypeAttributes;
	public List<AttributesMap> variableAttributes = new ArrayList<AttributesMap>();
	
	public ReferenceDescription referenceToObject;
	public ReferenceDescription[] objectReferences;
	public ReferenceDescription[] objectTypeReferences;
	public ReferencesMap[] variableReferences;

	public OpcUaObject() {
		super();
	}

	public OpcUaObject(Object obj) {
		this.obj = obj;
	}
}
