package pt.inesctec.opcua.server.model.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OpcUaVariableDeclaration {
	String browseName();
}