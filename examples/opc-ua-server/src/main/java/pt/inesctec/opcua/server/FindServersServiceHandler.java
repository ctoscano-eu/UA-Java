package pt.inesctec.opcua.server;

import org.opcfoundation.ua.application.Application;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.FindServersRequest;
import org.opcfoundation.ua.core.FindServersResponse;
import org.opcfoundation.ua.core.ResponseHeader;
import org.opcfoundation.ua.transport.Endpoint;
import org.opcfoundation.ua.transport.endpoint.EndpointServiceRequest;

/**
 * This service handler contains only one method: onFindServers
 */
public class FindServersServiceHandler {

	private NanoServer nanoServer;

	public FindServersServiceHandler(NanoServer nanoServer) {
		this.nanoServer = nanoServer;
	}

	/**
	 * FindServers Service
	 * 
	 * @param req
	 *          EndpointServiceRequest
	 * @throws ServiceFaultException
	 */
	public void onFindServers(EndpointServiceRequest<FindServersRequest, FindServersResponse> req) throws ServiceFaultException {

		FindServersRequest request = req.getRequest();

		ApplicationDescription[] servers = new ApplicationDescription[1];

		Application application = nanoServer.getApplication();
		String applicationUri = application.getApplicationUri();
		String productUri = application.getProductUri();
		ApplicationDescription applicationDescription = application.getApplicationDescription();
		LocalizedText applicationName = applicationDescription.getApplicationName();
		ApplicationType applicationType = applicationDescription.getApplicationType();
		String gatewayServerUri = null;
		String discoveryProfileUri = null;
		String[] discoveryUrls = applicationDescription.getDiscoveryUrls();
		if (discoveryUrls == null) {
			// Specify default URLs for the DiscoveryServer if
			// getDiscoveryUrls() returned null.
			Endpoint[] discoveryEndpoints = nanoServer.getEndpoints();
			discoveryUrls = new String[discoveryEndpoints.length];
			for (int i = 0; i < discoveryEndpoints.length; i++) {
				discoveryUrls[i] = discoveryEndpoints[i].getEndpointUrl();
			}
		}
		servers[0] = new ApplicationDescription(applicationUri, productUri, applicationName, applicationType, gatewayServerUri, discoveryProfileUri, discoveryUrls);

		ResponseHeader header = new ResponseHeader(DateTime.currentTime(), request.getRequestHeader().getRequestHandle(), StatusCode.GOOD, null, null, null);

		FindServersResponse response = new FindServersResponse(header, servers);

		req.sendResponse(response);
	}
}
