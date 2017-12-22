package pt.inesctec.opcua;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.cert.DefaultCertificateValidator;
import org.opcfoundation.ua.cert.PkiDirectoryCertificateStore;
import org.opcfoundation.ua.common.NamespaceTable;
import org.opcfoundation.ua.common.ServiceFaultException;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReadResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.examples.certs.ExampleKeys;
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.utils.CertificateUtils;

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
		sessionChannel.activate();

		return sessionChannel;
	}

	public void shutdownSession() throws ServiceFaultException, ServiceResultException {
		sessionChannel.close();
		sessionChannel.closeAsync();
	}

	public ReferenceDescription[] browse(NodeId... nodeIdArray) throws ServiceFaultException, ServiceResultException {
		BrowseDescription[] nodesToBrowse = new BrowseDescription[nodeIdArray.length];
		for (int i = 0; i < nodeIdArray.length; ++i) {
			nodesToBrowse[i] = new BrowseDescription();
			nodesToBrowse[i].setNodeId(nodeIdArray[i]);
			nodesToBrowse[i].setBrowseDirection(BrowseDirection.Forward);
			nodesToBrowse[i].setIncludeSubtypes(true);
			nodesToBrowse[i].setNodeClassMask(NodeClass.Object, NodeClass.Variable);
			nodesToBrowse[i].setResultMask(BrowseResultMask.All);
			//nodesToBrowse[i].setReferenceTypeId
		}

		BrowseResponse res = sessionChannel.Browse(null, null, null, nodesToBrowse);

		assertEquals(0, res.getDiagnosticInfos().length);
		assertEquals(StatusCode.GOOD, res.getResponseHeader().getServiceResult());
		assertEquals(StatusCode.GOOD, res.getResults()[0].getStatusCode());

		return res.getResults()[0].getReferences();
	}

	public ReferenceDescription[] browse(ExpandedNodeId... expandedNodeIdArray) throws ServiceFaultException, ServiceResultException {
		return browse(toNodeId(expandedNodeIdArray));
	}

	public DataValue[] read(NodeId... nodeIdArray) throws ServiceFaultException, ServiceResultException {
		ReadValueId[] nodesToRead = new ReadValueId[nodeIdArray.length];
		for (int i = 0; i < nodeIdArray.length; ++i)
			nodesToRead[i] = new ReadValueId(nodeIdArray[i], Attributes.Value, null, null);
		ReadResponse res = sessionChannel.Read(null, null, TimestampsToReturn.Neither, nodesToRead);

		assertEquals(0, res.getDiagnosticInfos().length);
		assertEquals(StatusCode.GOOD, res.getResponseHeader().getServiceResult());
		assertEquals(StatusCode.GOOD, res.getResults()[0].getStatusCode());

		return res.getResults();
	}

	public DataValue[] read(ExpandedNodeId... expandedNodeIdArray) throws ServiceFaultException, ServiceResultException {
		return read(toNodeId(expandedNodeIdArray));
	}

	private NodeId[] toNodeId(ExpandedNodeId... expandedNodeIdArray) throws ServiceResultException {
		NodeId[] nodeIdArray = new NodeId[expandedNodeIdArray.length];
		for (int i = 0; i < nodeIdArray.length; ++i)
			nodeIdArray[i] = namespaceTable.toNodeId(expandedNodeIdArray[i]);
		return nodeIdArray;
	}

}
