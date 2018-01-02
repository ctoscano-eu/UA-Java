package pt.inesctec.opcua;

import java.util.List;

import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedByte;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
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
import org.opcfoundation.ua.core.BrowseResult;
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
import org.opcfoundation.ua.core.RelativePath;
import org.opcfoundation.ua.core.RelativePathElement;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.SubscriptionAcknowledgement;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.core.TranslateBrowsePathsToNodeIdsResponse;
import org.opcfoundation.ua.core.WriteResponse;
import org.opcfoundation.ua.core.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class OpcUaSession {

	private Logger logger = LoggerFactory.getLogger(OpcUaSession.class);

	public OpcUaClient opcUaCLient;
	public String serverUrl;
	public SessionChannel sessionChannel;
	private NamespaceTable namespaceTable;

	public OpcUaSession() {
		super();
	}

	public OpcUaSession(OpcUaClient myCLient) {
		super();
		this.opcUaCLient = myCLient;
	}

	private void logDiagnosticInfos(DiagnosticInfo[] diagnosticInfoAray) {
		if (diagnosticInfoAray.length != 0)
			logger.warn("DiagnosticInfos.length: " + diagnosticInfoAray.length);
	}

	private void logServiceResult(StatusCode serviceResult) {
		if (!serviceResult.equals(StatusCode.GOOD))
			logger.warn("ServiceResult: " + serviceResult);
	}

	private void logBrowseResults(BrowseResult[] results) {
		for (int i = 0; i < results.length; ++i)
			if (!results[i].getStatusCode().equals(StatusCode.GOOD))
				logger.warn("BrowseResult.StatusCode: " + results[i].getStatusCode());
	}

	private void logDataValues(DataValue[] results) {
		for (int i = 0; i < results.length; ++i)
			if (!results[i].getStatusCode().equals(StatusCode.GOOD))
				logger.warn("DataValue.StatusCode: " + results[i].getStatusCode());
	}

	private void logWriteStatusCode(StatusCode[] results) {
		for (int i = 0; i < results.length; ++i)
			if (!results[i].equals(StatusCode.GOOD))
				logger.warn("WriteStatusCode: " + results[i]);
	}

	private void logBrowsePathResults(BrowsePathResult[] results) {
		for (int i = 0; i < results.length; ++i)
			if (!results[i].getStatusCode().equals(StatusCode.GOOD))
				logger.warn("BrowsePathResult.StatusCode: " + results[i]);
	}

	private void logMonitoredItemCreateResults(MonitoredItemCreateResult[] results) {
		for (int i = 0; i < results.length; ++i)
			if (!results[i].getStatusCode().equals(StatusCode.GOOD))
				logger.warn("MonitoredItemCreateResult.StatusCode: " + results[i]);
	}

	private void logPublishResponse(StatusCode[] results) {
		for (int i = 0; i < results.length; ++i)
			if (!results[i].equals(StatusCode.GOOD))
				logger.warn("PublishResponse.StatusCode: " + results[i]);
	}

	public SessionChannel create(String url) throws ServiceResultException {
		this.serverUrl = url;

		// Not required to create the Client but usefull for later service invocations
		this.namespaceTable = new NamespaceTable();

		sessionChannel = opcUaCLient.client.createSessionChannel(url);
		// mySession.activate("username", "123");
		ActivateSessionResponse res = sessionChannel.activate();

		logDiagnosticInfos(res.getDiagnosticInfos());
		logServiceResult(res.getResponseHeader().getServiceResult());

		sessionChannel.getSecureChannel().setOperationTimeout(5000);

		return sessionChannel;
	}

	public void shutdown() {
		try {
			sessionChannel.close();
		}
		catch (Throwable t) {
			// ignore
		}
		try {
			sessionChannel.closeAsync();
		}
		catch (Throwable t) {
			// ignore
		}
	}

	/*
	 * Takes a list of starting Nodes and returns a list of connected Nodes for each starting Node.
	 */
	public BrowseResult[] browseNodeOjectsAndVariables(NodeId... nodeIdArray) throws ServiceFaultException, ServiceResultException {
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

		logDiagnosticInfos(res.getDiagnosticInfos());
		logServiceResult(res.getResponseHeader().getServiceResult());
		logBrowseResults(res.getResults());

		return res.getResults();
	}

	public BrowseResult[] browseNodeOjectsAndVariables(ExpandedNodeId... expandedNodeIdArray) throws ServiceFaultException, ServiceResultException {
		return browseNodeOjectsAndVariables(toNodeId(expandedNodeIdArray));
	}

	public List<BrowseResult> browseHierarchyOfNodeVariables(NodeId... nodeIdArray) throws ServiceFaultException, ServiceResultException {
		List<BrowseResult> output = Lists.newArrayList();

		BrowseResult[] resp = browseNodeOjectsAndVariables(nodeIdArray);
		for (int i = 0; i < resp.length; ++i) {
			for (int j = 0; j < resp[i].getReferences().length; ++j) {
				if (resp[i].getReferences()[j].getNodeClass() == NodeClass.Object)
					output.addAll(browseHierarchyOfNodeVariables(toNodeId(resp[i].getReferences()[j].getNodeId())));
				else if (resp[i].getReferences()[j].getNodeClass() == NodeClass.Variable)
					output.add(resp[i]); // TOD ctoscano ??????
			}
		}

		return output;
	}

	public List<BrowseResult> browseHierarchyOfNodeVariables(ExpandedNodeId... expandedNodeIdArray) throws ServiceFaultException, ServiceResultException {
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

		logDiagnosticInfos(res.getDiagnosticInfos());
		logServiceResult(res.getResponseHeader().getServiceResult());
		logDataValues(res.getResults());

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

		logDiagnosticInfos(res.getDiagnosticInfos());
		logServiceResult(res.getResponseHeader().getServiceResult());
		logWriteStatusCode(res.getResults());

		return res.getResults();
	}

	public BrowsePathResult[] translateBrowsePathsToNodeIds(BrowsePath... pathToTranslate) throws ServiceFaultException, ServiceResultException {
		TranslateBrowsePathsToNodeIdsResponse res = sessionChannel.TranslateBrowsePathsToNodeIds(null, pathToTranslate);

		logDiagnosticInfos(res.getDiagnosticInfos());
		logServiceResult(res.getResponseHeader().getServiceResult());
		logBrowsePathResults(res.getResults());

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
		double requestedPublishingInterval = 1000.0; // the interval when the server clears the queues and delivers the notifications to the client.
		                                             //The Publish enabled setting defines whether the data gets delivered to the client. 
		UnsignedInteger requestedLifetimeCount = UnsignedInteger.valueOf(60);
		UnsignedInteger requestedMaxKeepAliveCount = UnsignedInteger.valueOf(20);
		UnsignedInteger maxNotificationsPerPublish = UnsignedInteger.valueOf(10);
		UnsignedByte priority = UnsignedByte.valueOf(10);
		CreateSubscriptionResponse res = sessionChannel.CreateSubscription(null, requestedPublishingInterval, requestedLifetimeCount, requestedMaxKeepAliveCount, maxNotificationsPerPublish, true,
		    priority);

		logServiceResult(res.getResponseHeader().getServiceResult());

		return res;
	}

	/*
	 * SamplingInterval:
	 *   The rate in milliseconds the server checks the underlying data source for changes. The type of change 
	 *   that triggers a notification is defined by the filter.  
	 * 	 If –1 is used for this interval, the publishing interval of the Subscription is used as for this setting. 
	 *	 A client can over sample the Value by setting the SamplingInterval to a smaller value than the publishing 
	 *	interval and the queue size to 1 
	 *
	 * QueueSize:
	 *   Maximum number of values stored for the Monitored Item during a publishing interval. After each publishing 
	 *   interval the server will send the values in the queue to the client
	 *    
	 */
	public MonitoredItemCreateResult[] createMonitoredItems(UnsignedInteger subscriptionId, ReadValueId... itemToMonitor) throws ServiceFaultException, ServiceResultException {
		MonitoringParameters requestedParameters = new MonitoringParameters();
		requestedParameters.setSamplingInterval(-1.0);
		requestedParameters.setQueueSize(UnsignedInteger.valueOf(10));// defines how many notifications can be queued for delivery (default value for data changes is one)
		requestedParameters.setDiscardOldest(true);
		//requestedParameters.setFilter();
		//requestedParameters.setClientHandle();

		MonitoredItemCreateRequest[] itemToCreateArray = new MonitoredItemCreateRequest[itemToMonitor.length];
		for (int i = 0; i < itemToCreateArray.length; ++i) {
			itemToCreateArray[i] = new MonitoredItemCreateRequest();
			itemToCreateArray[i].setItemToMonitor(itemToMonitor[i]);
			itemToCreateArray[i].setMonitoringMode(MonitoringMode.Reporting);// defines if the Monitored Item is active or inactive, defines if data gets sampled and delivered
			// The monitoring mode defines the states disabled, sampling, and reporting.
			itemToCreateArray[i].setRequestedParameters(requestedParameters);
		}

		CreateMonitoredItemsResponse res = sessionChannel.CreateMonitoredItems(null, subscriptionId, TimestampsToReturn.Both, itemToCreateArray);

		logDiagnosticInfos(res.getDiagnosticInfos());
		logServiceResult(res.getResponseHeader().getServiceResult());
		logMonitoredItemCreateResults(res.getResults());

		return res.getResults();
	}

	public PublishResponse publish(UnsignedInteger subscriptionId, long sequenceNumber) throws ServiceFaultException, ServiceResultException {

		try {
			SubscriptionAcknowledgement subscriptionAcknowledgement = new SubscriptionAcknowledgement();
			subscriptionAcknowledgement.setSequenceNumber(UnsignedInteger.valueOf(sequenceNumber));
			subscriptionAcknowledgement.setSubscriptionId(subscriptionId);

			PublishResponse res = sessionChannel.Publish(null, subscriptionAcknowledgement);

			logDiagnosticInfos(res.getDiagnosticInfos());
			logServiceResult(res.getResponseHeader().getServiceResult());
			logPublishResponse(res.getResults());

			res.getResults();
			res.getAvailableSequenceNumbers();
			res.getNotificationMessage();
			res.getMoreNotifications();

			return res;
		}
		catch (ServiceResultException e) {
			if (e.getStatusCode().getValue().equals(StatusCodes.Bad_Timeout))
				return null;
			else
				throw e;
		}
	}

}
