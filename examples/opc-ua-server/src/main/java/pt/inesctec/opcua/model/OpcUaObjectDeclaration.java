package pt.inesctec.opcua.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OpcUaObjectDeclaration {

	String nodeIdNamespaceIndex();

	String browseName();

}
