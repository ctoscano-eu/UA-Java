package pt.inesctec.opcua;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.cert.DefaultCertificateValidator;
import org.opcfoundation.ua.cert.PkiDirectoryCertificateStore;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.BrowsePath;
import org.opcfoundation.ua.core.BrowsePathResult;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.CreateSubscriptionResponse;
import org.opcfoundation.ua.core.MonitoredItemCreateResult;
import org.opcfoundation.ua.core.PublishResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.WriteValue;
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.utils.CertificateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.inesctec.opcua.certs.ExampleKeys;
import pt.inesctec.opcua.model.OpcUaProperties;

/*
 * Nodes and References.
 * Each Node has a NodeClass: Object, Variable, Method, ObjectType, VariableType, ReferenceType, DataType, View.
 * Each Node is described by Attributes: NodeId, NodeClass, BrowseName, DisplayName, Description, WriteMask, UserWriteMask, 
 *                                       IsAbstract, Symmetric, InverseName, ContainsNoLoops, EventNotifier, 
 *                                       Value, DataType, ValueRank, ArrayDimensions, AccessLevel, UserAccessLevel, 
 *                                       MinimumSamplingInterval, Historizing, Executable, UserExecutable
 * There are some Attributes common to every Node.
 * Properties allow to define additional information describing a Node. 
 * References do not contain any Attributes or Properties.
 * Nodes of the NodeClass Variable represent a value. Nodes of the NodeClass Object are used to structure the Address Space 
 * (they contain only Attributes, Properties, Variables and Methods.
 */
public class OpcUaClient {

	private Logger logger = LoggerFactory.getLogger(OpcUaClient.class);

	private KeyPair keyPair;
	private PkiDirectoryCertificateStore myCertStore;
	private DefaultCertificateValidator myCertValidator;
	private MyCertValidationListener myCertValidationListener;
	private KeyPair myHttpsCertificate;

	public String appName;
	public Client client;

	private OpcUaSessionList opcUaSessionList = new OpcUaSessionList();

	public OpcUaClient() {
		super();
	}

	// Use only on Test cases
	public OpcUaSession getOpcUaSession(String serverUrl) {
		return opcUaSessionList.getOpcUaSession(serverUrl);
	}

	public void create(String appName) throws ServiceResultException {

		this.appName = appName;

		// Set default key size for created certificates. The default value is also 2048,
		// but in some cases you may want to specify a different size.
		CertificateUtils.setKeySize(2048);

		// Try to load an application certificate with the specified application name.
		// In case it is not found, a new certificate is created.
		keyPair = ExampleKeys.getCert(appName);

		// Create the client using information provided by the created certificate
		client = Client.createClientApplication(keyPair);

		client.getApplication().addLocale(Locale.ENGLISH);
		client.getApplication().setApplicationName(new LocalizedText(appName, Locale.ENGLISH));
		client.getApplication().setProductUri("urn:" + appName);

		// Create a certificate store for handling server certificates.
		// The constructor uses relative path "SampleClientPKI/CA" as the base directory, storing
		// rejected certificates in folder "rejected" and trusted certificates in folder "trusted".
		// To accept a server certificate, a rejected certificate needs to be moved from rejected to
		// trusted folder. This can be performed by moving the certificate manually, using method
		// addTrustedCertificate of PkiDirectoryCertificateStore or, as in this example, using a
		// custom implementation of DefaultCertificateValidatorListener.
		myCertStore = new PkiDirectoryCertificateStore(appName + "PKI/CA");

		// Create a default certificate validator for validating server certificates in the certificate
		// store.
		myCertValidator = new DefaultCertificateValidator(myCertStore);

		// Set MyValidationListener instance as the ValidatorListener. In case a certificate is not
		// automatically accepted, user can choose to reject or accept the certificate.
		myCertValidationListener = new MyCertValidationListener();
		myCertValidator.setValidationListener(myCertValidationListener);

		// Set myValidator as the validator for OpcTcp and Https
		client.getApplication().getOpctcpSettings().setCertificateValidator(myCertValidator);
		client.getApplication().getHttpsSettings().setCertificateValidator(myCertValidator);

		// The HTTPS SecurityPolicies are defined separate from the endpoint securities
		client.getApplication().getHttpsSettings().setHttpsSecurityPolicies(HttpsSecurityPolicy.ALL);

		// The certificate to use for HTTPS
		myHttpsCertificate = ExampleKeys.getHttpsCert(appName);
		client.getApplication().getHttpsSettings().setKeyPair(myHttpsCertificate);

		logger.info("OpcUaClient created for " + appName);
	}

	synchronized public OpcUaSession createOpcUaSession(OpcUaProperties opcUaProperties) throws ServiceResultException {
		if (opcUaSessionList.containsKey(opcUaProperties.serverUrl))
			return opcUaSessionList.get(opcUaProperties.serverUrl); // return existing SessionChannel

		OpcUaSession opcUaSession = new OpcUaSession(this);
		opcUaSession.create(opcUaProperties);

		opcUaSessionList.put(opcUaProperties.serverUrl, opcUaSession);

		logger.info("OpcUaSession created for " + opcUaProperties.toString());

		return opcUaSession;
	}

	synchronized public OpcUaSession createOpcUaSessionIfNotAvailable(OpcUaProperties opcUaProperties) throws ServiceResultException {
		if (opcUaSessionList.containsKey(opcUaProperties.serverUrl))
			return opcUaSessionList.get(opcUaProperties.serverUrl); // return existing SessionChannel
		else
			return createOpcUaSession(opcUaProperties);
	}

	synchronized public void shutdownOpcUaSession(String serverUrl) {
		if (opcUaSessionList.isEmpty()) // no Session to shutdown 
			return;

		OpcUaSession opcUaSession = opcUaSessionList.getOpcUaSession(serverUrl);
		opcUaSession.shutdown();

		logger.info("OpcUaSession shutdown on " + opcUaSession.opcUaProperties.toString());

		opcUaSessionList.remove(serverUrl);
	}

	synchronized public void shutdownAllOpcUaSession() {
		if (opcUaSessionList.isEmpty()) // no Session to shutdown 
			return;

		Iterator<String> it = this.opcUaSessionList.keySet().iterator();
		while (it.hasNext()) {
			String opcUaSessionUrl = it.next();
			shutdownOpcUaSession(opcUaSessionUrl);
		}
	}

	private void processServiceResultException(String serverUrl, ServiceResultException e) throws ServiceResultException {
		if (e.getStatusCode().getValue().equals(StatusCodes.Bad_Timeout)) {
			shutdownOpcUaSession(serverUrl);
			createOpcUaSession(opcUaSessionList.getOpcUaSession(serverUrl).opcUaProperties);
		}
	}

	/*
	 * Takes a list of starting Nodes and returns a list of connected Nodes for each starting Node.
	 */
	synchronized public BrowseResult[] browseNodeOjectsAndVariables(String serverUrl, NodeId... nodeIdArray) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).browseNodeOjectsAndVariables(nodeIdArray);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public BrowseResult[] browseNodeOjectsAndVariables(String serverUrl, ExpandedNodeId... expandedNodeIdArray) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).browseNodeOjectsAndVariables(expandedNodeIdArray);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public List<BrowseResult> browseHierarchyOfNodeVariables(String serverUrl, NodeId... nodeIdArray) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).browseHierarchyOfNodeVariables(nodeIdArray);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public List<BrowseResult> browseHierarchyOfNodeVariables(String serverUrl, ExpandedNodeId... expandedNodeIdArray) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).browseHierarchyOfNodeVariables(expandedNodeIdArray);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	/*
	 * Used to read the value of Variables of one or more Nodes. 
	 * Other Attributes can be read.
	 */
	synchronized public DataValue[] readVariableValue(String serverUrl, NodeId... nodeIdArray) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).readVariableValue(nodeIdArray);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public DataValue[] readVariableValue(String serverUrl, ExpandedNodeId... expandedNodeIdArray) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).readVariableValue(expandedNodeIdArray);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public DataValue[] readVariableValue(String serverUrl, String... pathArray) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).readVariableValue(pathArray);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	/*
	 * Used to write one or more Attributes of one or more Nodes
	 */
	synchronized public StatusCode[] write(String serverUrl, WriteValue... nodesToWrite) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).write(nodesToWrite);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public BrowsePathResult[] translateBrowsePathsToNodeIds(String serverUrl, BrowsePath... pathToTranslate) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).translateBrowsePathsToNodeIds(pathToTranslate);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	// path must be something like "11111/222222/333333"
	synchronized public BrowsePathResult[] translateBrowsePathsToNodeIds(String serverUrl, MyBrowsePath myBrowsePath) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).translateBrowsePathsToNodeIds(myBrowsePath);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	// path must be something like "/11111/222222/333333"
	synchronized public BrowsePathResult[] translateRootBrowsePathsToNodeIds(String serverUrl, String path) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).translateRootBrowsePathsToNodeIds(path);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	// path must be something like "/11111/222222/333333"
	synchronized public BrowsePathResult[] translateRootBrowsePathsToNodeIds(String serverUrl, String... pathArray) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).translateRootBrowsePathsToNodeIds(pathArray);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public CreateSubscriptionResponse createSubscription(String serverUrl) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).createSubscription();
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public MonitoredItemCreateResult[] createMonitoredItems(String serverUrl, UnsignedInteger subscriptionId, ReadValueId itemToMonitor)
	    throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).createMonitoredItems(subscriptionId, itemToMonitor);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public PublishResponse publish(String serverUrl, UnsignedInteger subscriptionId, long sequenceNumber) throws ServiceFaultException, ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).publish(subscriptionId, sequenceNumber);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public NodeId[] toNodeId(String serverUrl, ExpandedNodeId... expandedNodeIdArray) throws ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).toNodeId(expandedNodeIdArray);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

	synchronized public NodeId toNodeId(String serverUrl, ExpandedNodeId expandedNodeId) throws ServiceResultException {
		try {
			return opcUaSessionList.getOpcUaSession(serverUrl).toNodeId(expandedNodeId);
		}
		catch (ServiceResultException e) {
			processServiceResultException(serverUrl, e);
			throw e;
		}
	}

}
