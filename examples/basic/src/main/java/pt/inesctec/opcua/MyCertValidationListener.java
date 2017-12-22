package pt.inesctec.opcua;

import java.io.IOException;
import java.util.EnumSet;

import org.opcfoundation.ua.cert.CertificateCheck;
import org.opcfoundation.ua.cert.DefaultCertificateValidatorListener;
import org.opcfoundation.ua.cert.ValidationResult;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.transport.security.Cert;

public class MyCertValidationListener implements DefaultCertificateValidatorListener {

	@Override
	public ValidationResult onValidate(Cert certificate, ApplicationDescription applicationDescription, EnumSet<CertificateCheck> passedChecks) {
		System.out.println("Validating Server Certificate...");
		if (passedChecks.containsAll(CertificateCheck.COMPULSORY)) {
			System.out.println("Server Certificate is valid and trusted, accepting certificate!");
			return ValidationResult.AcceptPermanently;
		}
		else {
			System.out.println("Certificate Details: " + certificate.getCertificate().toString());
			System.out.println("Do you want to accept this certificate?\n" + " (A=Always, Y=Yes, this time, N=No)");
			while (true) {
				try {
					char c;
					c = Character.toLowerCase((char) System.in.read());
					if (c == 'a') {
						return ValidationResult.AcceptPermanently;
					}
					if (c == 'y') {
						return ValidationResult.AcceptOnce;
					}
					if (c == 'n') {
						return ValidationResult.Reject;
					}
				}
				catch (IOException e) {
					System.out.println("Error reading input! Not accepting certificate.");
					return ValidationResult.Reject;
				}
			}
		}
	}

}
