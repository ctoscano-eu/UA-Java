package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedByte;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.cert.DefaultCertificateValidator;
import org.opcfoundation.ua.cert.PkiDirectoryCertificateStore;
import org.opcfoundation.ua.common.NamespaceTable;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.ActivateSessionResponse;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowsePath;
import org.opcfoundation.ua.core.BrowsePathResult;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.CreateMonitoredItemsResponse;
import org.opcfoundation.ua.core.CreateSubscriptionResponse;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MonitoredItemCreateRequest;
import org.opcfoundation.ua.core.MonitoredItemCreateResult;
import org.opcfoundation.ua.core.MonitoringMode;
import org.opcfoundation.ua.core.MonitoringParameters;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.PublishResponse;
import org.opcfoundation.ua.core.ReadResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.RelativePath;
import org.opcfoundation.ua.core.RelativePathElement;
import org.opcfoundation.ua.core.SubscriptionAcknowledgement;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.core.TranslateBrowsePathsToNodeIdsResponse;
import org.opcfoundation.ua.core.WriteResponse;
import org.opcfoundation.ua.core.WriteValue;
import org.opcfoundation.ua.examples.certs.ExampleKeys;
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.utils.CertificateUtils;

import com.google.common.collect.Lists;

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
public class MyCLient {

	private KeyPair keyPair;
	private PkiDirectoryCertificateStore myCertStore;
	private DefaultCertificateValidator myCertValidator;
	private MyCertValidationListener myCertValidationListener;
	private KeyPair myHttpsCertificate;
	private NamespaceTable namespaceTable;

	public String appName;
	public Client client;
	public SessionChannel sessionChannel;

	public MyCLient() {
		super();
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

		// Not required to create the Client but usefull for later service invocations
		namespaceTable = new NamespaceTable();
	}

	public SessionChannel createSession(String url) throws ServiceResultException {
		sessionChannel = client.createSessionChannel(url);
		// mySession.activate("username", "123");
		ActivateSessionResponse res = sessionChannel.activate();
		
		assertEquals(0, res.getDiagnosticInfos().length);
		assertEquals(0, res.getResults().length);
		assertEquals(StatusCode.GOOD, res.getResponseHeader().getServiceResult());

		return sessionChannel;
	}

	public void shutdownSession() throws ServiceFaultException, ServiceResultException {
		sessionChannel.close();
		sessionChannel.closeAsync();
	}

	/*
	 * Takes a list of starting Nodes and returns a list of connected Nodes for each starting Node.
	 */
	public ReferenceDescription[] browseNodeOjectsAndVariables(NodeId... nodeIdArray) throws ServiceFaultException, ServiceResultException {
		BrowseDescription[] nodesToBrowse = new BrowseDescription[nodeIdArray.length];
		for (int i = 0; i < nodeIdArray.length; ++i) {
			nodesToBrowse[i] = new BrowseDescription();
			nodesToBrowse[i].setNodeId(nodeIdArray[i]);
			nodesToBrowse[i].setBrowseDirection(BrowseDirection.Forward);
			nodesToBrowse[i].setIncludeSubtypes(true);
			nodesToBrowse[i].setNodeClassMask(NodeClass.Object, NodeClass.Variable);
			nodesToBrowse[i].setResultMask(BrowseResultMask.All);
			nodesToBrowse[i].setReferenceTypeId(Identifiers.HierarchicalReferences);
		}

		BrowseResponse res = sessionChannel.Browse(null, null, null, nodesToBrowse);

		assertEquals(0, res.getDiagnosticInfos().length);
		assertEquals(StatusCode.GOOD, res.getResponseHeader().getServiceResult());
		assertEquals(StatusCode.GOOD, res.getResults()[0].getStatusCode());

		return res.getResults()[0].getReferences();
	}

	public ReferenceDescription[] browseNodeOjectsAndVariables(ExpandedNodeId... expandedNodeIdArray) throws ServiceFaultException, ServiceResultException {
		return browseNodeOjectsAndVariables(toNodeId(expandedNodeIdArray));
	}

	public List<ReferenceDescription> browseHierarchyOfNodeVariables(NodeId... nodeIdArray) throws ServiceFaultException, ServiceResultException {
		List<ReferenceDescription> output = Lists.newArrayList();
		ReferenceDescription[] resp = browseNodeOjectsAndVariables(nodeIdArray);
		for (int i = 0; i < resp.length; ++i) {
			if (resp[i].getNodeClass() == NodeClass.Object)
				output.addAll(browseHierarchyOfNodeVariables(toNodeId(resp[i].getNodeId())));
			else if (resp[i].getNodeClass() == NodeClass.Variable)
				output.add(resp[i]);
		}

		return output;
	}

	public List<ReferenceDescription> browseHierarchyOfNodeVariables(ExpandedNodeId... expandedNodeIdArray) throws ServiceFaultException, ServiceResultException {
		return browseHierarchyOfNodeVariables(toNodeId(expandedNodeIdArray));
	}

	/*
	 * Used to read the value of Variables of one or more Nodes. 
	 * Other Attributes can be read.
	 */
	public DataValue[] readVariableValue(NodeId... nodeIdArray) throws ServiceFaultException, ServiceResultException {
		ReadValueId[] nodesToRead = new ReadValueId[nodeIdArray.length];
		for (int i = 0; i < nodeIdArray.length; ++i)
			nodesToRead[i] = new ReadValueId(nodeIdArray[i], Attributes.Value, null, null);
		ReadResponse res = sessionChannel.Read(null, null, TimestampsToReturn.Neither, nodesToRead);

		assertEquals(0, res.getDiagnosticInfos().length);
		assertEquals(StatusCode.GOOD, res.getResponseHeader().getServiceResult());
		assertEquals(StatusCode.GOOD, res.getResults()[0].getStatusCode());

		return res.getResults();
	}

	public DataValue[] readVariableValue(ExpandedNodeId... expandedNodeIdArray) throws ServiceFaultException, ServiceResultException {
		return readVariableValue(toNodeId(expandedNodeIdArray));
	}

	public DataValue[] readVariableValue(String... pathArray) throws ServiceFaultException, ServiceResultException {
		BrowsePathResult[] var = translateRootBrowsePathsToNodeIds(pathArray);
		ExpandedNodeId[] varNodeIdArray = new ExpandedNodeId[var.length];
		for (int i = 0; i < var.length; ++i)
			varNodeIdArray[i] = var[i].getTargets()[0].getTargetId();

		return readVariableValue(varNodeIdArray);
	}

	/*
	 * Used to write one or more Attributes of one or more Nodes
	 */
	public StatusCode[] write(WriteValue... nodesToWrite) throws ServiceFaultException, ServiceResultException {
		WriteResponse res = sessionChannel.Write(null, nodesToWrite);

		assertEquals(0, res.getDiagnosticInfos().length);
		assertEquals(StatusCode.GOOD, res.getResponseHeader().getServiceResult());

		return res.getResults();
	}

	public BrowsePathResult[] translateBrowsePathsToNodeIds(BrowsePath... pathToTranslate) throws ServiceFaultException, ServiceResultException {

		TranslateBrowsePathsToNodeIdsResponse res = sessionChannel.TranslateBrowsePathsToNodeIds(null, pathToTranslate);

		assertEquals(0, res.getDiagnosticInfos().length);
		assertEquals(StatusCode.GOOD, res.getResponseHeader().getServiceResult());
		assertEquals(StatusCode.GOOD, res.getResults()[0].getStatusCode());

		return res.getResults();
	}

	// path must be something like "11111/222222/333333"
	public BrowsePathResult[] translateBrowsePathsToNodeIds(NodeId startingNode, String... pathArray) throws ServiceFaultException, ServiceResultException {
		//String[] terms = "/11111/222222/333333".split("/"); // "", "1111", .... 
		//String[] terms2 = "11111/222222/333333".split("/"); // "1111", .... 
		//String[] terms3 = "11111/222222/333333/".split("/"); // "1111", .... 
		String[][] termsArray = new String[pathArray.length][];
		for (int i = 0; i < pathArray.length; ++i) {
			String[] terms = pathArray[i].split("/");
			if (terms.length == 0)
				return null;
			termsArray[i] = terms;
		}

		BrowsePath[] browsePathArray = new BrowsePath[pathArray.length];
		for (int i = 0; i < pathArray.length; ++i)
			browsePathArray[i] = createBrowsePath(startingNode, termsArray[i]);

		return translateBrowsePathsToNodeIds(browsePathArray);
	}

	// path must be something like "/11111/222222/333333"
	public BrowsePathResult[] translateRootBrowsePathsToNodeIds(String path) throws ServiceFaultException, ServiceResultException {
		String[] terms = path.split("/");
		if (terms.length == 0)
			return null;
		if (terms[0].length() != 0)
			return null;

		return translateBrowsePathsToNodeIds(Identifiers.RootFolder, path.substring(1));
	}

	// path must be something like "/11111/222222/333333"
	public BrowsePathResult[] translateRootBrowsePathsToNodeIds(String... pathArray) throws ServiceFaultException, ServiceResultException {
		for (int i = 0; i < pathArray.length; ++i) {
			String[] terms = pathArray[i].split("/");
			if (terms.length == 0)
				return null;
			if (terms[0].length() != 0)
				return null;
		}

		String[] newPathArray = new String[pathArray.length];
		for (int i = 0; i < pathArray.length; ++i) {
			newPathArray[i] = pathArray[i].substring(1);
		}

		return translateBrowsePathsToNodeIds(Identifiers.RootFolder, newPathArray);
	}

	private BrowsePath createBrowsePath(NodeId startingNode, String[] terms) {
		BrowsePath browsePath = new BrowsePath();
		browsePath.setStartingNode(startingNode);
		browsePath.setRelativePath(new RelativePath());

		// build each RelativePathElement
		RelativePathElement[] elements = new RelativePathElement[terms.length];
		browsePath.getRelativePath().setElements(elements);
		for (int i = 0; i < terms.length; ++i) {
			RelativePathElement elem = new RelativePathElement();
			elem.setTargetName(new QualifiedName(terms[i]));
			elements[i] = elem;
		}

		return browsePath;
	}

	public NodeId[] toNodeId(ExpandedNodeId... expandedNodeIdArray) throws ServiceResultException {
		NodeId[] nodeIdArray = new NodeId[expandedNodeIdArray.length];
		for (int i = 0; i < nodeIdArray.length; ++i)
			nodeIdArray[i] = namespaceTable.toNodeId(expandedNodeIdArray[i]);
		return nodeIdArray;
	}

	public NodeId toNodeId(ExpandedNodeId expandedNodeId) throws ServiceResultException {
		return namespaceTable.toNodeId(expandedNodeId);
	}

	public CreateSubscriptionResponse createSubscription() throws ServiceFaultException, ServiceResultException {
		double requestedPublishingInterval = 1000.0;
		UnsignedInteger requestedLifetimeCount = UnsignedInteger.valueOf(1000);
		UnsignedInteger requestedMaxKeepAliveCount = UnsignedInteger.valueOf(1000);
		UnsignedInteger maxNotificationsPerPublish = UnsignedInteger.valueOf(100);
		UnsignedByte priority = UnsignedByte.valueOf(1);
		CreateSubscriptionResponse res = sessionChannel.CreateSubscription(null, requestedPublishingInterval, requestedLifetimeCount, requestedMaxKeepAliveCount, maxNotificationsPerPublish, true,
		    priority);

		assertEquals(StatusCode.GOOD, res.getResponseHeader().getServiceResult());

		return res;
	}

	public MonitoredItemCreateResult[] createMonitoredItems(UnsignedInteger subscriptionId, ReadValueId itemToMonitor) throws ServiceFaultException, ServiceResultException {
		MonitoringParameters requestedParameters = new MonitoringParameters();
		requestedParameters.setSamplingInterval(1000.0);
		requestedParameters.setQueueSize(UnsignedInteger.valueOf(1000));

		MonitoredItemCreateRequest itemToCreate = new MonitoredItemCreateRequest();
		itemToCreate.setItemToMonitor(itemToMonitor);
		itemToCreate.setMonitoringMode(MonitoringMode.Sampling);
		itemToCreate.setRequestedParameters(requestedParameters);
		CreateMonitoredItemsResponse res = sessionChannel.CreateMonitoredItems(null, subscriptionId, TimestampsToReturn.Both, itemToCreate);

		assertEquals(0, res.getDiagnosticInfos().length);
		assertEquals(StatusCode.GOOD, res.getResponseHeader().getServiceResult());
		assertEquals(StatusCode.GOOD, res.getResults()[0].getStatusCode());

		return res.getResults();
	}

	public PublishResponse publish(UnsignedInteger subscriptionId, long sequenceNumber) throws ServiceFaultException, ServiceResultException {
		SubscriptionAcknowledgement subscriptionAcknowledgement = new SubscriptionAcknowledgement();
		subscriptionAcknowledgement.setSequenceNumber(UnsignedInteger.valueOf(sequenceNumber));
		subscriptionAcknowledgement.setSubscriptionId(subscriptionId);
		PublishResponse res = sessionChannel.Publish(null, subscriptionAcknowledgement);

		assertEquals(0, res.getDiagnosticInfos().length);
		assertEquals(StatusCode.GOOD, res.getResponseHeader().getServiceResult());
		res.getResults();
		res.getAvailableSequenceNumbers();
		res.getNotificationMessage();
		res.getMoreNotifications();

		return res;
	}
}
