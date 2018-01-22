package pt.inesctec.opcua.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.opcfoundation.ua.application.Application;
import org.opcfoundation.ua.application.Server;
import org.opcfoundation.ua.builtintypes.ByteString;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.ExtensionObject;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.ActivateSessionRequest;
import org.opcfoundation.ua.core.ActivateSessionResponse;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowseResult;
import org.opcfoundation.ua.core.CancelRequest;
import org.opcfoundation.ua.core.CancelResponse;
import org.opcfoundation.ua.core.CloseSessionRequest;
import org.opcfoundation.ua.core.CloseSessionResponse;
import org.opcfoundation.ua.core.CreateSessionRequest;
import org.opcfoundation.ua.core.CreateSessionResponse;
import org.opcfoundation.ua.core.EndpointConfiguration;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.RequestHeader;
import org.opcfoundation.ua.core.ResponseHeader;
import org.opcfoundation.ua.core.ServiceFault;
import org.opcfoundation.ua.core.SessionServiceSetHandler;
import org.opcfoundation.ua.core.SignatureData;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.UserIdentityToken;
import org.opcfoundation.ua.core.UserNameIdentityToken;
import org.opcfoundation.ua.core.UserTokenPolicy;
import org.opcfoundation.ua.encoding.DecodingException;
import org.opcfoundation.ua.encoding.IEncodeable;
import org.opcfoundation.ua.transport.endpoint.EndpointServiceRequest;
import org.opcfoundation.ua.transport.security.BcCryptoProvider;
import org.opcfoundation.ua.transport.security.CertificateValidator;
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.SecurityAlgorithm;
import org.opcfoundation.ua.transport.security.SecurityMode;
import org.opcfoundation.ua.transport.security.SecurityPolicy;
import org.opcfoundation.ua.utils.CertificateUtils;
import org.opcfoundation.ua.utils.CryptoUtil;
import org.opcfoundation.ua.utils.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.inesctec.opcua.certs.ExampleKeys;

public class NanoServer extends Server implements SessionServiceSetHandler {

	private final Logger logger = LoggerFactory.getLogger(NanoServer.class);

	private int complianceNamespaceIndex = 1;

	public Map<NodeId, BrowseResult> onBrowseActions;
	public Map<NodeId, Map<UnsignedInteger, DataValue>> onReadResultsMap;
	public Map<NodeId, Class<?>> datatypeMap;

	public ArrayList<NodeId> sessions;
	public ArrayList<NodeId> validAuthenticationTokens;
	private Map<NodeId, Long> timeoutPeriods;
	public ContinuationPoint continuationPoint;

	public NanoServer(Application application) throws Exception {

		super(application);

		addServiceHandler(this);

		// Add Client Application Instance Certificate validator - Accept
		// them all (for now)
		application.getOpctcpSettings().setCertificateValidator(CertificateValidator.ALLOW_ALL);
		application.getHttpsSettings().setCertificateValidator(CertificateValidator.ALLOW_ALL);

		// The HTTPS SecurityPolicies are defined separate from the endpoint
		// securities
		application.getHttpsSettings().setHttpsSecurityPolicies(HttpsSecurityPolicy.ALL);

		// Peer verifier
		application.getHttpsSettings().setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		// Load Servers's Application Instance Certificate...
		KeyPair myServerApplicationInstanceCertificate = ExampleKeys.getCert("NanoServer");
		application.addApplicationInstanceCertificate(myServerApplicationInstanceCertificate);
		// ...and HTTPS certificate
		KeyPair myHttpsCertificate = ExampleKeys.getHttpsCert("NanoServer");
		application.getHttpsSettings().setKeyPair(myHttpsCertificate);

		// Add User Token Policies
		addUserTokenPolicy(UserTokenPolicy.ANONYMOUS);
		addUserTokenPolicy(UserTokenPolicy.SECURE_USERNAME_PASSWORD);

		// Create an endpoint for each network interface
		String hostname = EndpointUtil.getHostname();
		String bindAddress, endpointAddress;
		for (String addr : EndpointUtil.getInetAddressNames()) {
			bindAddress = "https://" + addr + ":8443";
			endpointAddress = "https://" + hostname + ":8443";
			logger.info("{} bound at {}", endpointAddress, bindAddress);
			// The HTTPS ports are using NONE OPC security
			bind(bindAddress, endpointAddress, SecurityMode.NONE);

			bindAddress = "opc.tcp://" + addr + ":8666";
			endpointAddress = "opc.tcp://" + hostname + ":8666";
			logger.info("{} bound at {}", endpointAddress, bindAddress);
			bind(bindAddress, endpointAddress, SecurityMode.ALL_101);
		}

		// Make ArrayList for authentication tokens
		validAuthenticationTokens = new ArrayList<NodeId>();
		sessions = new ArrayList<NodeId>();
		timeoutPeriods = new HashMap<NodeId, Long>();

		// Set continuationPoint to null at start-up
		continuationPoint = null;

		init();

		addServiceHandler(new MyNodeManagementServiceHandler(this));
		addServiceHandler(new MyAttributeServiceHandler(this));
		addServiceHandler(new FindServersServiceHandler(this));
	}

	public void shutdown() {
		// Close the server by unbinding all endpoints
		getApplication().close();
	}

	private void init() throws ParseException {

		// *******************************************************************************
		// Put all browse references in one HashMap for better readability and performance
		// *******************************************************************************

		onBrowseActions = new HashMap<NodeId, BrowseResult>();
		// root node
		onBrowseActions.put(Identifiers.RootFolder, new BrowseResult(StatusCode.GOOD, null, new ReferenceDescription[] {
		    // Parameters: ReferenceTypeId, IsForward, NodeId, BrowseName, DisplayName,
		    // NodeClass, TypeDefinition
		    new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(Identifiers.ObjectsFolder), new QualifiedName("Objects"), new LocalizedText("Objects"), NodeClass.Object,
		        new ExpandedNodeId(Identifiers.FolderType)),
		    new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(Identifiers.TypesFolder), new QualifiedName("Types"), new LocalizedText("Types"), NodeClass.Object,
		        new ExpandedNodeId(Identifiers.FolderType)),
		    new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(Identifiers.ViewsFolder), new QualifiedName("Views"), new LocalizedText("Views"), NodeClass.Object,
		        new ExpandedNodeId(Identifiers.FolderType)),
		    new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.FolderType), new QualifiedName("FolderType"), new LocalizedText("FolderType"),
		        NodeClass.ObjectType, null) }));
		// types folder
		onBrowseActions.put(Identifiers.TypesFolder,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(Identifiers.DataTypesFolder), new QualifiedName("DataTypes"), new LocalizedText("DataTypes"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.FolderType)),
		            new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(Identifiers.ReferenceTypesFolder), new QualifiedName("ReferenceTypes"), new LocalizedText("ReferenceTypes"),
		                NodeClass.Object, new ExpandedNodeId(Identifiers.FolderType)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.FolderType), new QualifiedName("FolderType"), new LocalizedText("FolderType"),
		                NodeClass.ObjectType, null),
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(Identifiers.RootFolder), new QualifiedName("Root"), new LocalizedText("Root"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.FolderType)) }));
		// Views folder
		onBrowseActions.put(Identifiers.ViewsFolder,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.FolderType), new QualifiedName("FolderType"), new LocalizedText("FolderType"),
		                NodeClass.ObjectType, null),
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(Identifiers.RootFolder), new QualifiedName("Root"), new LocalizedText("Root"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.FolderType)) }));
		// etc...
		onBrowseActions.put(Identifiers.DataTypesFolder,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(Identifiers.BaseDataType), new QualifiedName("BaseDataType"), new LocalizedText("BaseDataType"),
		                NodeClass.DataType, null),
		            new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(Identifiers.XmlSchema_TypeSystem), new QualifiedName("XML Schema"), new LocalizedText("XML Schema"),
		                NodeClass.Object, new ExpandedNodeId(Identifiers.DataTypeSystemType)),
		            new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(Identifiers.OPCBinarySchema_TypeSystem), new QualifiedName("OPC Binary"), new LocalizedText("OPC Binary"),
		                NodeClass.Object, new ExpandedNodeId(Identifiers.DataTypeSystemType)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.FolderType), new QualifiedName("FolderType"), new LocalizedText("FolderType"),
		                NodeClass.ObjectType, null),
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(Identifiers.TypesFolder), new QualifiedName("Types"), new LocalizedText("Types"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.FolderType)) }));
		onBrowseActions.put(Identifiers.ReferenceTypesFolder,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(Identifiers.References), new QualifiedName("References"), new LocalizedText("References"),
		                NodeClass.ReferenceType, null),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.FolderType), new QualifiedName("FolderType"), new LocalizedText("FolderType"),
		                NodeClass.ObjectType, null),
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(Identifiers.TypesFolder), new QualifiedName("Types"), new LocalizedText("Types"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.FolderType)) }));
		onBrowseActions.put(Identifiers.References,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasSubtype, true, new ExpandedNodeId(Identifiers.NonHierarchicalReferences), new QualifiedName("NonHierarchicalReferences"),
		                new LocalizedText("NonHierarchicalReferences"), NodeClass.ReferenceType, null),
		            new ReferenceDescription(Identifiers.HasSubtype, true, new ExpandedNodeId(Identifiers.HierarchicalReferences), new QualifiedName("HierarchicalReferences"),
		                new LocalizedText("HierarchicalReferences"), NodeClass.ReferenceType, null),
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(Identifiers.ReferenceTypesFolder), new QualifiedName("ReferenceTypes"), new LocalizedText("ReferenceTypes"),
		                NodeClass.Object, new ExpandedNodeId(Identifiers.FolderType)) }));

		onBrowseActions.put(Identifiers.NonHierarchicalReferences,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasSubtype, false, new ExpandedNodeId(Identifiers.References), new QualifiedName("References"), new LocalizedText("References"),
		                NodeClass.ReferenceType, null),
		            new ReferenceDescription(Identifiers.HasSubtype, true, new ExpandedNodeId(Identifiers.HasTypeDefinition), new QualifiedName("HasTypeDefinition"),
		                new LocalizedText("HasTypeDefinition"), NodeClass.ReferenceType, null)

						}));

		onBrowseActions.put(Identifiers.HierarchicalReferences, new BrowseResult(StatusCode.GOOD, null, new ReferenceDescription[] {
		    new ReferenceDescription(Identifiers.HasSubtype, false, new ExpandedNodeId(Identifiers.References), new QualifiedName("References"), new LocalizedText("References"), NodeClass.ReferenceType,
		        null),
		    new ReferenceDescription(Identifiers.HasSubtype, true, new ExpandedNodeId(Identifiers.HasChild), new QualifiedName("HasChild"), new LocalizedText("HasChild"), NodeClass.ReferenceType, null),
		    new ReferenceDescription(Identifiers.HasSubtype, true, new ExpandedNodeId(Identifiers.Organizes), new QualifiedName("Organizes"), new LocalizedText("Organizes"), NodeClass.ReferenceType, null)

		}));

		onBrowseActions.put(Identifiers.BaseDataType, new BrowseResult(StatusCode.GOOD, null, new ReferenceDescription[] {
		    new ReferenceDescription(Identifiers.HasSubtype, true, new ExpandedNodeId(Identifiers.Boolean), new QualifiedName("Boolean"), new LocalizedText("Boolean"), NodeClass.DataType, null) }));
		onBrowseActions.put(Identifiers.ObjectsFolder,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(Identifiers.RootFolder), new QualifiedName("Root"), new LocalizedText("Root"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.FolderType)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.FolderType), new QualifiedName("FolderType"), new LocalizedText("FolderType"),
		                NodeClass.ObjectType, null),
		            new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(Identifiers.Server), new QualifiedName("Server"), new LocalizedText("Server"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.ServerType)),
		            new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(new NodeId(complianceNamespaceIndex, "StaticData")), new QualifiedName("StaticData"),
		                new LocalizedText("StaticData"), NodeClass.Object, new ExpandedNodeId(Identifiers.FolderType)) }));
		onBrowseActions.put(Identifiers.Server,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(Identifiers.ObjectsFolder), new QualifiedName("Objects"), new LocalizedText("Objects"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.ObjectsFolder)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.ServerType), new QualifiedName("ServerType"), new LocalizedText("ServerType"),
		                NodeClass.ObjectType, new ExpandedNodeId(Identifiers.ServerType)),
		            new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(Identifiers.Server_ServerStatus), new QualifiedName("ServerStatus"), new LocalizedText("ServerStatus"),
		                NodeClass.Variable, new ExpandedNodeId(Identifiers.Server_ServerStatus)),
		            new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(Identifiers.Server_ServerCapabilities), new QualifiedName("ServerCapabilities"),
		                new LocalizedText("ServerCapabilities"), NodeClass.Object, new ExpandedNodeId(Identifiers.FolderType)),
		            new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(Identifiers.Server_ServerArray), new QualifiedName("ServerArray"), new LocalizedText("ServerArray"),
		                NodeClass.Variable, new ExpandedNodeId(Identifiers.PropertyType)),
		            new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(Identifiers.Server_NamespaceArray), new QualifiedName("NamespaceArray"),
		                new LocalizedText("NamespaceArray"), NodeClass.Variable, new ExpandedNodeId(Identifiers.PropertyType)) }));
		onBrowseActions.put(Identifiers.Server_ServerCapabilities,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(Identifiers.Server), new QualifiedName("Server"), new LocalizedText("Server"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.FolderType)),
		            new ReferenceDescription(Identifiers.HasProperty, true, new ExpandedNodeId(Identifiers.Server_ServerCapabilities_MaxBrowseContinuationPoints),
		                new QualifiedName("MaxBrowseContinuationPoints"), new LocalizedText("MaxBrowseContinuationPoints"), NodeClass.Variable, new ExpandedNodeId(Identifiers.UInt16)) }));
		onBrowseActions.put(Identifiers.Server_ServerCapabilities_MaxBrowseContinuationPoints,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasProperty, false, new ExpandedNodeId(Identifiers.Server_ServerCapabilities), new QualifiedName("ServerCapabilities"),
		                new LocalizedText("ServerCapabilities"), NodeClass.Object, new ExpandedNodeId(Identifiers.FolderType)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.PropertyType), new QualifiedName("PropertyType"), new LocalizedText("PropertyType"),
		                NodeClass.DataType, null) }));

		onBrowseActions.put(Identifiers.Server_ServerArray,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasProperty, false, new ExpandedNodeId(Identifiers.Server), new QualifiedName("Server"), new LocalizedText("Server"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.ServerType)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.PropertyType), new QualifiedName("PropertyType"), new LocalizedText("PropertyType"),
		                NodeClass.DataType, null) }));
		onBrowseActions.put(Identifiers.Server_NamespaceArray,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasProperty, false, new ExpandedNodeId(Identifiers.Server), new QualifiedName("Server"), new LocalizedText("Server"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.ServerType)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.PropertyType), new QualifiedName("PropertyType"), new LocalizedText("PropertyType"),
		                NodeClass.DataType, null) }));
		onBrowseActions.put(Identifiers.FolderType, new BrowseResult(StatusCode.GOOD, null, new ReferenceDescription[] { new ReferenceDescription(Identifiers.HasSubtype, false,
		    new ExpandedNodeId(Identifiers.BaseObjectType), new QualifiedName("BaseObjectType"), new LocalizedText("BaseObjectType"), NodeClass.ObjectType, null) }));

		onBrowseActions.put(Identifiers.OPCBinarySchema_TypeSystem,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(Identifiers.DataTypesFolder), new QualifiedName("DataTypes"), new LocalizedText("DataTypes"),
		                NodeClass.Object, new ExpandedNodeId(Identifiers.FolderType)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.DataTypeSystemType), new QualifiedName("DataTypeSystemType"),
		                new LocalizedText("DataTypeSystemType"), NodeClass.ObjectType, new ExpandedNodeId(Identifiers.DataTypeSystemType)) }));
		onBrowseActions.put(Identifiers.XmlSchema_TypeSystem,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(Identifiers.DataTypesFolder), new QualifiedName("DataTypes"), new LocalizedText("DataTypes"),
		                NodeClass.Object, new ExpandedNodeId(Identifiers.FolderType)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.DataTypeSystemType), new QualifiedName("DataTypeSystemType"),
		                new LocalizedText("DataTypeSystemType"), NodeClass.ObjectType, new ExpandedNodeId(Identifiers.DataTypeSystemType)) }));
		onBrowseActions.put(Identifiers.DataTypeSystemType, new BrowseResult(StatusCode.GOOD, null, null));

		onBrowseActions.put(Identifiers.ServerType, new BrowseResult(StatusCode.GOOD, null, null));

		onBrowseActions.put(Identifiers.PropertyType,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasSubtype, false, new ExpandedNodeId(Identifiers.BaseVariableType), new QualifiedName("BaseVariableType"), new LocalizedText("BaseVariableType"),
		                NodeClass.VariableType, null),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, false, new ExpandedNodeId(Identifiers.Server_ServerCapabilities_MaxBrowseContinuationPoints),
		                new QualifiedName("MaxBrowseContinuationPoints"), new LocalizedText("MaxBrowseContinuationPoints"), NodeClass.Variable, null),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, false, new ExpandedNodeId(Identifiers.Server_ServerArray), new QualifiedName("ServerArray"), new LocalizedText("ServerArray"),
		                NodeClass.Variable, new ExpandedNodeId(Identifiers.PropertyType)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, false, new ExpandedNodeId(Identifiers.Server_NamespaceArray), new QualifiedName("NamespaceArray"),
		                new LocalizedText("NamespaceArray"), NodeClass.Variable, new ExpandedNodeId(Identifiers.PropertyType)) }));

		onBrowseActions.put(Identifiers.BaseDataVariableType,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasSubtype, false, new ExpandedNodeId(Identifiers.BaseVariableType), new QualifiedName("BaseVariableType"), new LocalizedText("BaseVariableType"),
		                NodeClass.VariableType, null),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, false, new ExpandedNodeId(Identifiers.Server_ServerStatus_State), new QualifiedName("State"), new LocalizedText("State"),
		                NodeClass.Variable, null) }));

		onBrowseActions.put(Identifiers.Server_ServerStatus,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(Identifiers.Server), new QualifiedName("Server"), new LocalizedText("Server"), NodeClass.Variable,
		                new ExpandedNodeId(Identifiers.Server)),
		            new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(Identifiers.Server_ServerStatus_State), new QualifiedName("State"), new LocalizedText("State"),
		                NodeClass.Variable, new ExpandedNodeId(Identifiers.ServerState)) }));
		onBrowseActions.put(Identifiers.Server_ServerStatus_State,
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(Identifiers.Server_ServerStatus), new QualifiedName("ServerStatus"), new LocalizedText("ServerStatus"),
		                NodeClass.Variable, new ExpandedNodeId(Identifiers.ServerStatusDataType)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.BaseDataVariableType), new QualifiedName("BaseDataVariableType"),
		                new LocalizedText("BaseDataVariableType"), NodeClass.VariableType, new ExpandedNodeId(Identifiers.BaseVariableType)) }));

		onBrowseActions.put(Identifiers.Server_GetMonitoredItems, new BrowseResult(StatusCode.GOOD, null, null));

		onBrowseActions.put(Identifiers.BaseObjectType, new BrowseResult(StatusCode.GOOD, null, null));

		onBrowseActions.put(Identifiers.BaseVariableType, new BrowseResult(StatusCode.GOOD, null, new ReferenceDescription[] {
		    // Parameters: ReferenceTypeId, IsForward, NodeId, BrowseName, DisplayName,
		    // NodeClass, TypeDefinition
		    new ReferenceDescription(Identifiers.HasSubtype, true, new ExpandedNodeId(Identifiers.BaseDataVariableType), new QualifiedName("BaseDataVariableType"),
		        new LocalizedText("BaseDataVariableType"), NodeClass.VariableType, null),
		    new ReferenceDescription(Identifiers.HasSubtype, true, new ExpandedNodeId(Identifiers.PropertyType), new QualifiedName("PropertyType"), new LocalizedText("PropertyType"),
		        NodeClass.VariableType, null) }));

		onBrowseActions.put(Identifiers.HasChild, new BrowseResult(StatusCode.GOOD, null, new ReferenceDescription[] {
		    // Parameters: ReferenceTypeId, IsForward, NodeId, BrowseName, DisplayName, NodeClass,
		    // TypeDefinition
		    new ReferenceDescription(Identifiers.HasSubtype, false, new ExpandedNodeId(Identifiers.HierarchicalReferences), new QualifiedName("HierarchicalReferences"),
		        new LocalizedText("HierarchicalReferences"), NodeClass.ReferenceType, null),
		    new ReferenceDescription(Identifiers.HasSubtype, true, new ExpandedNodeId(Identifiers.HasSubtype), new QualifiedName("HasSubtype"), new LocalizedText("HasSubtype"), NodeClass.ReferenceType,
		        null) }));
		onBrowseActions.put(Identifiers.HasSubtype, new BrowseResult(StatusCode.GOOD, null, new ReferenceDescription[] {
		    // Parameters: ReferenceTypeId, IsForward, NodeId, BrowseName, DisplayName,
		    // NodeClass, TypeDefinition
		    new ReferenceDescription(Identifiers.HasSubtype, false, new ExpandedNodeId(Identifiers.HasChild), new QualifiedName("HasChild"), new LocalizedText("HasChild"), NodeClass.ReferenceType,
		        null) }));

		onBrowseActions.put(Identifiers.Organizes, new BrowseResult(StatusCode.GOOD, null, new ReferenceDescription[] {
		    // Parameters: ReferenceTypeId, IsForward, NodeId, BrowseName, DisplayName, NodeClass,
		    // TypeDefinition
		    new ReferenceDescription(Identifiers.HasSubtype, false, new ExpandedNodeId(Identifiers.HierarchicalReferences), new QualifiedName("HierarchicalReferences"),
		        new LocalizedText("HierarchicalReferences"), NodeClass.ReferenceType, null) }));
		onBrowseActions.put(Identifiers.HasTypeDefinition, new BrowseResult(StatusCode.GOOD, null, new ReferenceDescription[] {
		    // Parameters: ReferenceTypeId, IsForward, NodeId, BrowseName, DisplayName,
		    // NodeClass, TypeDefinition
		    new ReferenceDescription(Identifiers.HasSubtype, false, new ExpandedNodeId(Identifiers.NonHierarchicalReferences), new QualifiedName("NonHierarchicalReferences"),
		        new LocalizedText("NonHierarchicalReferences"), NodeClass.ReferenceType, null) }));

		// Compliance namespace StaticData folder
		onBrowseActions.put(new NodeId(complianceNamespaceIndex, "StaticData"),
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(Identifiers.ObjectsFolder), new QualifiedName("Objects"), new LocalizedText("Objects"), NodeClass.Object,
		                new ExpandedNodeId(Identifiers.ObjectsFolder)),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.FolderType), new QualifiedName("FolderType"), new LocalizedText("FolderType"),
		                NodeClass.ObjectType, new ExpandedNodeId(Identifiers.FolderType)),
		            new ReferenceDescription(Identifiers.Organizes, true, new ExpandedNodeId(new NodeId(complianceNamespaceIndex, "StaticVariablesFolder")), new QualifiedName("StaticVariables"),
		                new LocalizedText("StaticVariables"), NodeClass.Object, new ExpandedNodeId(Identifiers.FolderType)) }));
		// Static Variables folder
		onBrowseActions.put(new NodeId(complianceNamespaceIndex, "StaticVariablesFolder"),
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] {
		            new ReferenceDescription(Identifiers.HasComponent, true, new ExpandedNodeId(new NodeId(complianceNamespaceIndex, "Boolean")), new QualifiedName("Boolean"),
		                new LocalizedText("Boolean"), NodeClass.Variable, new ExpandedNodeId(Identifiers.BaseDataVariableType)),
		            new ReferenceDescription(Identifiers.Organizes, false, new ExpandedNodeId(new NodeId(complianceNamespaceIndex, "StaticData")), new QualifiedName("StaticData"),
		                new LocalizedText("StaticData"), NodeClass.Object, new ExpandedNodeId(new NodeId(complianceNamespaceIndex, "StaticData"))),
		            new ReferenceDescription(Identifiers.HasTypeDefinition, true, new ExpandedNodeId(Identifiers.FolderType), new QualifiedName("FolderType"), new LocalizedText("FolderType"),
		                NodeClass.ObjectType, new ExpandedNodeId(Identifiers.FolderType)) }));

		onBrowseActions.put(Identifiers.Boolean, new BrowseResult(StatusCode.GOOD, null, new ReferenceDescription[] {
		    // Parameters: ReferenceTypeId, IsForward, NodeId, BrowseName, DisplayName, NodeClass,
		    // TypeDefinition
		    new ReferenceDescription(Identifiers.HasSubtype, false, new ExpandedNodeId(Identifiers.BaseDataType), new QualifiedName("BaseDataType"), new LocalizedText("BaseDataType"), NodeClass.DataType,
		        null) }));

		onBrowseActions.put(new NodeId(complianceNamespaceIndex, "Boolean"),
		    new BrowseResult(StatusCode.GOOD, null,
		        new ReferenceDescription[] { new ReferenceDescription(Identifiers.HasComponent, false, new ExpandedNodeId(new NodeId(complianceNamespaceIndex, "StaticVariablesFolder")),
		            new QualifiedName("StaticVariables"), new LocalizedText("StaticVariables"), NodeClass.Object, new ExpandedNodeId(Identifiers.FolderType)) }));

		// *******************************************************************************
		// Put all read datavalues in one HashMap for better readability and performance
		// *******************************************************************************
		final DateTime serverTimeStamp = DateTime.currentTime();
		onReadResultsMap = new HashMap<NodeId, Map<UnsignedInteger, DataValue>>();

		onReadResultsMap.put(new NodeId(complianceNamespaceIndex, "Boolean"), new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(new NodeId(complianceNamespaceIndex, "Boolean")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Variable), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName(complianceNamespaceIndex + ":Boolean")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("Boolean", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Value, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DataType, new DataValue(new Variant(Identifiers.Boolean), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.ValueRank, new DataValue(new Variant(-1), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.ArrayDimensions, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.AccessLevel, new DataValue(new Variant(new UnsignedInteger(3)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserAccessLevel, new DataValue(new Variant(new UnsignedInteger(3)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.MinimumSamplingInterval, new DataValue(new Variant(-1.0), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Historizing, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.RootFolder, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.RootFolder), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("Root")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("Root", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The root of the server address space.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.ObjectsFolder, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.ObjectsFolder), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("Objects")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("Objects", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The browse entry point when looking for objects in the server address space.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(0), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.TypesFolder, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.TypesFolder), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("Types")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("Types", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The browse entry point when looking for types in the server address space.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(0), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.ViewsFolder, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.ViewsFolder), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("Views")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("Views", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The browse entry point when looking for views in the server address space.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(0), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.DataTypesFolder, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.DataTypesFolder), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("DataTypes")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("DataTypes", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The browse entry point when looking for data types in the server address space.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(0), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.ReferenceTypesFolder, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.ReferenceTypesFolder), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("ReferenceTypes")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("ReferenceTypes", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The browse entry point when looking for reference types in the server address space.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(0), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.References, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.References), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("References")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("References", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The abstract base type for all references.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(true), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.HierarchicalReferences, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.HierarchicalReferences), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("HierarchicalReferences")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("HierarchicalReferences", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("The abstract base type for all hierarchical references.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(new Variant(new LocalizedText("HierarchicalReferences.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.NonHierarchicalReferences, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.NonHierarchicalReferences), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("NonHierarchicalReferences")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("NonHierarchicalReferences", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("The abstract base type for all non-hierarchical references.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(new Variant(new LocalizedText("NonHierarchicalReferences.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.FolderType, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.FolderType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ObjectType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("FolderType")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("FolderType", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The type for objects that organize other nodes.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.OPCBinarySchema_TypeSystem, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.OPCBinarySchema_TypeSystem), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("OPC Binary")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("OPC Binary", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("A type system which uses OPC binary schema to describe the encoding of data types.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(0), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.XmlSchema_TypeSystem, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.XmlSchema_TypeSystem), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("XML Schema")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("XML Schema", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("A type system which uses XML schema to describe the encoding of data types.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(0), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.DataTypeSystemType, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.DataTypeSystemType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ObjectType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("DataTypeSystemType")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("DataTypeSystemType", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.PropertyType, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.PropertyType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.VariableType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("PropertyType")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("PropertyType", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("The type for variable that represents a property of another node.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.BaseDataVariableType, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.BaseDataVariableType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.VariableType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("BaseDataVariableType")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("BaseDataVariableType", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("The type for variable that represents a process value.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.Server), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("Server")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("Server", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(new Byte((byte) 1)), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.ServerType, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.ServerType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ObjectType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("ServerType")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("ServerType", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("Specifies the current status and capabilities of the server.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.HasComponent, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.HasComponent), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("HasComponent")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("HasComponent", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The type for non-looping hierarchical reference from a node to its component.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(new Variant(new LocalizedText("ComponentOf", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerStatus_CurrentTime, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.Value, new DataValue(new Variant(serverTimeStamp), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.Server_ServerCapabilities_LocaleIdArray, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.Value, new DataValue(new Variant(new String[1]), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerStatus, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.Server_ServerStatus), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Variable), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("ServerStatus")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("ServerStatus", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The current status of the server.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Value, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerStatus_State, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.Server_ServerStatus_State), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Variable), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("State")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("State", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Value, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		final String applicationURI = application.getApplicationUri();
		onReadResultsMap.put(Identifiers.Server_NamespaceArray, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.Server_NamespaceArray), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Variable), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("NamespaceArray")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("NamespaceArray", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The list of namespace URIs used by the server.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Value, new DataValue(new Variant(new String[] { "http://opcfoundation.org/UA/", applicationURI }), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerArray, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.Server_ServerArray), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Variable), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("ServerArray")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("ServerArray", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The list of server URIs used by the server.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Value, new DataValue(new Variant(new String[] { applicationURI }), StatusCode.GOOD, serverTimeStamp, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.Server_ServerStatus_BuildInfo_ProductName, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.Value, new DataValue(new Variant("SampleNanoServer"), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.Server_ServerStatus_BuildInfo_ManufacturerName, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.Value, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.Server_ServerStatus_BuildInfo_SoftwareVersion, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.Value, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(Identifiers.Server_ServerStatus_BuildInfo_BuildDate, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.Value, new DataValue(new Variant(DateTime.parseDateTime("2014-12-30T00:00:00Z")), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerStatus_StartTime, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.Value, new DataValue(new Variant(serverTimeStamp), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerStatus_SecondsTillShutdown, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.Value, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerStatus_ShutdownReason, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.Value, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerStatus_BuildInfo, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.Value, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerCapabilities, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.Server_ServerCapabilities), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("ServerCapabilities")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("ServerCapabilities", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("Describes the capabilities supported by the server.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(new Byte((byte) 0)), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerCapabilities_MaxBrowseContinuationPoints, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.Server_ServerCapabilities_MaxBrowseContinuationPoints), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Variable), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("MaxBrowseContinuationPoints")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("MaxBrowseContinuationPoints", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The maximum number of continuation points for Browse operations per session.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Value, new DataValue(new Variant(new UnsignedInteger(1)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DataType, new DataValue(new Variant(Identifiers.UInt16), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.ValueRank, new DataValue(new Variant(-2), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.ArrayDimensions, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.AccessLevel, new DataValue(new Variant(AccessLevel.CurrentRead), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserAccessLevel, new DataValue(new Variant(AccessLevel.CurrentRead), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.MinimumSamplingInterval, new DataValue(new Variant(0.0), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Historizing, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.Server_ServerDiagnostics_EnabledFlag, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.Server_ServerDiagnostics_EnabledFlag), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Variable), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("EnabledFlag")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("EnabledFlag", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("If TRUE the diagnostics collection is enabled.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Value, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DataType, new DataValue(new Variant(Identifiers.Boolean), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.ValueRank, new DataValue(new Variant(-2), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.ArrayDimensions, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.AccessLevel, new DataValue(new Variant(AccessLevel.CurrentRead), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserAccessLevel, new DataValue(new Variant(AccessLevel.CurrentRead), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.MinimumSamplingInterval, new DataValue(new Variant(0.0), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Historizing, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		onReadResultsMap.put(Identifiers.References, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.References), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("References")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("References", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The abstract base type for all references.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(true), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(null, StatusCode.GOOD, null, serverTimeStamp));

			}
		});

		onReadResultsMap.put(Identifiers.NonHierarchicalReferences, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.NonHierarchicalReferences), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("NonHierarchicalReferences")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("NonHierarchicalReferences", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("The abstract base type for all non-hierarchical references.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(new Variant(new LocalizedText("NonHierarchicalReferences", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));

			}
		});
		onReadResultsMap.put(Identifiers.HierarchicalReferences, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.HierarchicalReferences), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("HierarchicalReferences")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("HierarchicalReferences", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("The abstract base type for all hierarchical references.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(new Variant(new LocalizedText("HierarchicalReferences", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));

			}
		});

		onReadResultsMap.put(Identifiers.HasSubtype, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.HasSubtype), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("HasSubtype")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("HasSubtype", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The type for non-looping hierarchical references that are used to define sub types.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(new Variant(new LocalizedText("HasSupertype", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));

			}
		});
		onReadResultsMap.put(Identifiers.HasProperty, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.HasProperty), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("HasProperty")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("HasProperty", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The type for non-looping hierarchical reference from a node to its property.", LocalizedText.NO_LOCALE)),
				    StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(new Variant(new LocalizedText("PropertyOf", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));

			}
		});

		onReadResultsMap.put(Identifiers.HasChild, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.HasChild), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("HasChild")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("HasChild", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("The abstract base type for all non-looping hierarchical references.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(new Variant(new LocalizedText("ChildOf", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));

			}
		});

		onReadResultsMap.put(Identifiers.Organizes, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.Organizes), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("Organizes")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("Organizes", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("The type for hierarchical references that are used to organize nodes.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(new Variant(new LocalizedText("OrganizedBy", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));

			}
		});

		onReadResultsMap.put(Identifiers.HasTypeDefinition, new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(Identifiers.HasTypeDefinition), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.ReferenceType), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName("HasTypeDefinition")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("HasTypeDefinition", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description,
				    new DataValue(new Variant(new LocalizedText("The type for references from a instance node its type definition node.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.IsAbstract, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Symmetric, new DataValue(new Variant(false), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.InverseName, new DataValue(new Variant(new LocalizedText("TypeDefinitionOf", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));

			}
		});

		onReadResultsMap.put(new NodeId(complianceNamespaceIndex, "StaticData"), new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(new NodeId(complianceNamespaceIndex, "StaticData")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName(complianceNamespaceIndex + ":StaticData")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("StaticData", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The type for objects that organize other nodes.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(0), StatusCode.GOOD, null, serverTimeStamp));
			}
		});
		onReadResultsMap.put(new NodeId(complianceNamespaceIndex, "StaticVariablesFolder"), new HashMap<UnsignedInteger, DataValue>() {
			{
				put(Attributes.NodeId, new DataValue(new Variant(new NodeId(complianceNamespaceIndex, "StaticVariablesFolder")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.NodeClass, new DataValue(new Variant(NodeClass.Object), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.BrowseName, new DataValue(new Variant(new QualifiedName(complianceNamespaceIndex + ":StaticVariables")), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.DisplayName, new DataValue(new Variant(new LocalizedText("StaticVariables", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.Description, new DataValue(new Variant(new LocalizedText("The type for objects that organize other nodes.", LocalizedText.NO_LOCALE)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.WriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.UserWriteMask, new DataValue(new Variant(new UnsignedInteger(0)), StatusCode.GOOD, null, serverTimeStamp));
				put(Attributes.EventNotifier, new DataValue(new Variant(0), StatusCode.GOOD, null, serverTimeStamp));
			}
		});

		// *******************************************************************************
		// Put all data type mappings in one HashMap for better readability and performance
		// Only boolean supported at the moment
		// *******************************************************************************
		datatypeMap = new HashMap<NodeId, Class<?>>() {
			{
				put(Identifiers.Boolean, java.lang.Boolean.class);
			}
		};
		//////////////////////////////////////
	}

	@Override
	public void onActivateSession(EndpointServiceRequest<ActivateSessionRequest, ActivateSessionResponse> msgExchange) throws ServiceFaultException {

		ActivateSessionRequest request = msgExchange.getRequest();

		StatusCode statusCode = null;
		ActivateSessionResponse response = new ActivateSessionResponse();

		RequestHeader requestHeader = request.getRequestHeader();
		NodeId authenticationToken = requestHeader.getAuthenticationToken();
		if (!sessions.contains(authenticationToken)) {
			// This session is not valid
			statusCode = new StatusCode(StatusCodes.Bad_SessionClosed);
		}
		if (statusCode == null) {
			final ExtensionObject encodedToken = request.getUserIdentityToken();
			UserIdentityToken token = null;
			if (encodedToken != null) {
				try {
					token = encodedToken.decode(getEncoderContext());
					if (token.getTypeId().equals(new ExpandedNodeId(Identifiers.X509IdentityToken_Encoding_DefaultBinary)))
						statusCode = new StatusCode(StatusCodes.Bad_IdentityTokenInvalid);
				}
				catch (DecodingException e) {
					statusCode = new StatusCode(StatusCodes.Bad_IdentityTokenInvalid);
				}
			}

			if (timeoutPeriods != null && authenticationToken != null && timeoutPeriods.get(authenticationToken) != null) {

				Long timeToTimeout = timeoutPeriods.get(authenticationToken);

				Long now = System.currentTimeMillis();

				if (timeToTimeout < now) {
					statusCode = new StatusCode(StatusCodes.Bad_SessionClosed);
					validAuthenticationTokens.remove(authenticationToken);
					timeoutPeriods.remove(authenticationToken);

				}
			}
			else {
				statusCode = new StatusCode(StatusCodes.Bad_SessionIdInvalid);
			}
		}
		if (statusCode == null) {
			try {

				IEncodeable uit = request.getUserIdentityToken().decode(getEncoderContext());

				if (uit instanceof UserNameIdentityToken) {
					UserNameIdentityToken userNameIdentityToken = (UserNameIdentityToken) uit;
					String userName = userNameIdentityToken.getUserName();
					String policyId = userNameIdentityToken.getPolicyId();
					String encryptionAlgorithm = userNameIdentityToken.getEncryptionAlgorithm();

					if (userName == null) {
						statusCode = new StatusCode(StatusCodes.Bad_IdentityTokenInvalid);
					}
					else if (userName.equals("username")) {
						statusCode = new StatusCode(StatusCodes.Bad_UserAccessDenied);
					}
					else if (!userName.equals("user1") && !userName.equals("user2")) {
						statusCode = new StatusCode(StatusCodes.Bad_IdentityTokenRejected);
					}

					// Checking that policy id and encryption algorithm are
					// valid.
					// Add all supported policy ids and encryption
					// algorithms here.
					if (policyId == null || !policyId.equals("username_basic128") || encryptionAlgorithm == null || !encryptionAlgorithm.equals("http://www.w3.org/2001/04/xmlenc#rsa-1_5")) {
						statusCode = new StatusCode(StatusCodes.Bad_IdentityTokenInvalid);
					}
					else if (statusCode == null) {
						// user is user1 or user2, decrypt the password and
						// check password correctness

						PrivateKey pk = application.getApplicationInstanceCertificate().privateKey.getPrivateKey();
						ByteString dataToDecrypt = userNameIdentityToken.getPassword();
						// SunJceCryptoProvider needs buffer of at least 256
						// bytes
						byte[] output = new byte[256];
						int outputOffset = 0;

						CryptoUtil.getCryptoProvider().decryptAsymm(pk, SecurityAlgorithm.Rsa15, dataToDecrypt.getValue(), output, outputOffset);

						int count = 11; // Hard-coded for now. CTT only uses
						// passwords that are 8
						// characters...
						String plaintextPassword = new String(output, 1, count).trim();

						// These usernames and passwords are defined in CTT
						// settings
						if ((userName.equals("user1") && !plaintextPassword.equals("p4ssword")) || (userName.equals("user2") && !plaintextPassword.equals("passw0rd"))) {
							statusCode = new StatusCode(StatusCodes.Bad_UserAccessDenied);
						}

					}
				}

			}
			catch (DecodingException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
			catch (ServiceResultException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}

		}
		response.setServerNonce(CryptoUtil.createNonce(32));

		if (statusCode == null) {
			statusCode = StatusCode.GOOD;
			validAuthenticationTokens.add(authenticationToken);
		}
		ResponseHeader h = new ResponseHeader(DateTime.currentTime(), requestHeader.getRequestHandle(), statusCode, null, getApplication().getLocaleIds(), null);
		response.setResponseHeader(h);

		msgExchange.sendResponse(response);
	}

	@Override
	public void onCancel(EndpointServiceRequest<CancelRequest, CancelResponse> msgExchange) throws ServiceFaultException {
		CancelResponse response = new CancelResponse();
		ResponseHeader h = new ResponseHeader(DateTime.currentTime(), msgExchange.getRequest().getRequestHandle(), new StatusCode(StatusCodes.Bad_NotSupported), null, getApplication().getLocaleIds(),
		    null);
		response.setResponseHeader(h);

		msgExchange.sendResponse(response);
	}

	@Override
	public void onCloseSession(EndpointServiceRequest<CloseSessionRequest, CloseSessionResponse> msgExchange) throws ServiceFaultException {
		CloseSessionResponse res = new CloseSessionResponse();
		CloseSessionRequest req = msgExchange.getRequest();

		ResponseHeader h = Utils.checkRequestHeader(this, req.getRequestHeader());

		// take authentication token out of valid tokens
		validAuthenticationTokens.remove(req.getRequestHeader().getAuthenticationToken());
		sessions.remove(req.getRequestHeader().getAuthenticationToken());

		// Set continuation point to null, this also means that more than
		// one concurrent sessions cannot use continuation points
		continuationPoint = null;

		res.setResponseHeader(h);

		msgExchange.sendResponse(res);
	}

	@Override
	public void onCreateSession(EndpointServiceRequest<CreateSessionRequest, CreateSessionResponse> msgExchange) throws ServiceFaultException {
		CreateSessionRequest request = msgExchange.getRequest();
		CreateSessionResponse response = new CreateSessionResponse();

		StatusCode statusCode = null;
		byte[] token = new byte[32];
		byte[] nonce = new byte[32];
		Random r = new Random();
		r.nextBytes(nonce);
		r.nextBytes(token);

		// Check client nonce
		ByteString clientNonce = request.getClientNonce();
		if (clientNonce != null) {
			if (clientNonce.getLength() < 32) {
				statusCode = new StatusCode(StatusCodes.Bad_NonceInvalid);
			}
		}
		ByteString clientCertificate = request.getClientCertificate();
		if (clientCertificate != null) {
			String clientApplicationUri = request.getClientDescription().getApplicationUri();
			X509Certificate clientCertificateDecoded = null;
			String applicationUriOfDecoded = null;
			try {
				clientCertificateDecoded = CertificateUtils.decodeX509Certificate(clientCertificate.getValue());
				applicationUriOfDecoded = CertificateUtils.getApplicationUriOfCertificate(clientCertificateDecoded);
			}
			catch (CertificateException e) {
				e.printStackTrace();
			}

			if (!clientApplicationUri.equals(applicationUriOfDecoded)) {
				statusCode = new StatusCode(StatusCodes.Bad_CertificateUriInvalid);
			}
		}

		if (statusCode == null) {

			EndpointConfiguration endpointConfiguration = EndpointConfiguration.defaults();
			response.setMaxRequestMessageSize(UnsignedInteger.valueOf(Math.max(endpointConfiguration.getMaxMessageSize(), request.getMaxResponseMessageSize().longValue())));

			Double timeout = new Double(60 * 1000);
			if (!request.getRequestedSessionTimeout().equals(new Double(0))) {
				// set revised session timeout to 60 seconds or to lower
				// value if client requests
				timeout = Math.min(request.getRequestedSessionTimeout(), 60 * 1000);
			}
			response.setRevisedSessionTimeout(timeout);

			NodeId tokenId = new NodeId(0, token);
			response.setAuthenticationToken(tokenId);
			// Put authentication to memory in order to check validity of
			// incoming authentication tokens
			sessions.add(tokenId);
			Long time = System.currentTimeMillis();
			timeoutPeriods.put(tokenId, time + timeout.longValue());
		}

		KeyPair cert = getApplication().getApplicationInstanceCertificates()[0];
		response.setServerCertificate(ByteString.valueOf(cert.getCertificate().getEncoded()));
		response.setServerEndpoints(this.getEndpointDescriptions());
		response.setServerNonce(ByteString.valueOf(nonce));

		SecurityPolicy securityPolicy = msgExchange.getChannel().getSecurityPolicy();
		response.setServerSignature(getServerSignature(clientCertificate, clientNonce, securityPolicy, cert.getPrivateKey().getPrivateKey()));

		response.setServerSoftwareCertificates(getApplication().getSoftwareCertificates());
		response.setSessionId(new NodeId(0, "Session-" + UUID.randomUUID()));

		if (statusCode == null) {
			statusCode = StatusCode.GOOD;
		}
		// Set response header
		ResponseHeader h = new ResponseHeader(DateTime.currentTime(), request.getRequestHeader().getRequestHandle(), statusCode, null, getApplication().getLocaleIds(), null);
		response.setResponseHeader(h);

		msgExchange.sendResponse(response);
	}

	private SignatureData getServerSignature(ByteString clientCertificate, ByteString clientNonce, SecurityPolicy securityPolicy, final RSAPrivateKey privateKey) throws ServiceFaultException {
		if (clientCertificate != null) {
			ByteArrayOutputStream s = new ByteArrayOutputStream();
			try {
				s.write(clientCertificate.getValue());
			}
			catch (IOException e) {
				throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_SecurityChecksFailed));
			}
			catch (Exception e) {
				throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NonceInvalid));
			}
			try {
				s.write(clientNonce.getValue());
			}
			catch (IOException e) {
				throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NonceInvalid));
			}
			catch (Exception e) {
				throw new ServiceFaultException(ServiceFault.createServiceFault(StatusCodes.Bad_NonceInvalid));
			}
			try {
				SecurityAlgorithm algorithm = securityPolicy.getAsymmetricSignatureAlgorithm();
				if (algorithm == null) {
					algorithm = SecurityAlgorithm.RsaSha1;
				}
				return new SignatureData(algorithm.getUri(), ByteString.valueOf(CryptoUtil.getCryptoProvider().signAsymm(privateKey, algorithm, s.toByteArray())));
			}
			catch (ServiceResultException e) {
				throw new ServiceFaultException(e);
			}
		}
		return null;
	}

	static String getPublicHostname() {
		String publicHostname = "";
		try {
			publicHostname = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e) {
		}
		return publicHostname;
	}

	public static void main(String[] args) throws Exception {

		// Create Application
		String applicationName = "NanoServer";
		Application appl = new Application();
		appl.setApplicationName(new LocalizedText(applicationName, new Locale("en")));
		appl.setProductUri("urn:opcfoundation.org:OPCUA:" + applicationName);
		// set custom application URI and not default randomUUID
		appl.setApplicationUri("urn:" + getPublicHostname() + ":NanoServer");

		// Create NanoServer
		NanoServer nanoServer = new NanoServer(appl);

		CryptoUtil.setCryptoProvider(new BcCryptoProvider());

		// Press enter to shutdown
		System.out.println("Press enter to shutdown");
		System.in.read();

		nanoServer.shutdown();
	}

}
