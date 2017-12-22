package pt.inesctec.opcua;

import java.util.Locale;

import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.cert.DefaultCertificateValidator;
import org.opcfoundation.ua.cert.PkiDirectoryCertificateStore;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.examples.certs.ExampleKeys;
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.utils.CertificateUtils;

public class MyCLient {

	private String appName;
	private KeyPair keyPair;
	public Client client;
	private PkiDirectoryCertificateStore myCertStore;
	private DefaultCertificateValidator myCertValidator;
	private MyCertValidationListener myCertValidationListener;
	private KeyPair myHttpsCertificate;

	public MyCLient(String appName) {
		super();
		this.appName = appName;
	}

	public void create() throws ServiceResultException {

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
	}

}
