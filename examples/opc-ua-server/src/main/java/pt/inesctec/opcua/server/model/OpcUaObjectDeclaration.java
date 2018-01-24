package pt.inesctec.opcua.server.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OpcUaObjectDeclaration {

	String nodeIdNamespaceIndex();

	String browseName();

}
