package pt.inesctec.opcua;

import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;

public class MySession {
	private MyCLient myCLient;
	public SessionChannel sessionChannel;

	public MySession(MyCLient myCLient) {
		this.myCLient = myCLient;
	}

	public void createSession(String url) throws ServiceResultException {
		sessionChannel = myCLient.client.createSessionChannel(url);
		// mySession.activate("username", "123");
		sessionChannel.activate();
	}

	public void shutdown() throws ServiceFaultException, ServiceResultException {
		sessionChannel.close();
		sessionChannel.closeAsync();
	}
}
