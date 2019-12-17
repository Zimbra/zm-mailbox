/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.authenticator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.X509Extension;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.authenticator.ClientCertPrincipalMap.Rule;
import com.zimbra.cs.util.CertValidationUtil;

public class ClientCertAuthenticator extends SSOAuthenticator {

    static final String LOG_PREFIX = "certauth - ";

    public ClientCertAuthenticator(HttpServletRequest req, HttpServletResponse resp) {
        super(req, resp);
    }

    @Override
    public String getAuthType() {
        return "ClientCert";
    }

    @Override
    public ZimbraPrincipal authenticate() throws ServiceException {
        X509Certificate cert = getCert();

        if (DebugConfig.certAuthCaptureClientCertificate) {
            captureClientCert(cert);
        }

        ClientCertPrincipalMap principalMap = new ClientCertPrincipalMap(req);
        List<Rule> rules = principalMap.getRules();

        for (Rule rule : rules) {
            try {
                ZimbraLog.account.debug(LOG_PREFIX + "Attempting rule " + rule.getName());
                ZimbraPrincipal zimbraPrincipal = rule.apply(cert);
                if (zimbraPrincipal != null) {
                    return zimbraPrincipal;
                }
                ZimbraLog.account.debug(LOG_PREFIX + "Rule " + rule.getName() + " not matched");

            } catch (ServiceException e) {
                ZimbraLog.account.debug(LOG_PREFIX + "Rule " + rule.getName() + " not matched", e);
            }
        }

        throw AuthFailedServiceException.AUTH_FAILED(cert.toString(),
                "ClientCertAuthenticator - no matching Zimbra principal from client certificate.", (Throwable)null);
    }

    /*
     * Save the client cert to file for debugging.
     * To view: /opt/zimbra/openssl/bin/openssl x509 -in /opt/zimbra/data/tmp/clientcert.*** -text
     */
    private void captureClientCert(X509Certificate cert) {

        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd-HHmmss.S");
        FileOutputStream os = null;
        Writer wr = null;
        try {
            File file = new File(LC.zimbra_tmp_directory.value() + "/clientcert." + fmt.format(new Date()));

            // Get the encoded form which is suitable for exporting
            byte[] buf = cert.getEncoded();

            os = new FileOutputStream(file);
            // Write in text form
            wr = new OutputStreamWriter(os, Charset.forName("UTF-8"));
            wr.write("-----BEGIN CERTIFICATE-----\n");
            wr.write(Base64.getEncoder().encodeToString(buf));
            wr.write("\n-----END CERTIFICATE-----\n");
            wr.flush();
        } catch (CertificateEncodingException e) {
            ZimbraLog.account.debug(LOG_PREFIX +  "unable to capture cert", e);
        } catch (IOException e) {
            ZimbraLog.account.debug(LOG_PREFIX + "unable to capture cert", e);
        } finally {
            ByteUtil.closeWriter(wr);
            ByteUtil.closeStream(os);
        }
    }

    private X509Certificate getCert() throws ServiceException {
        X509Certificate[] certs = (X509Certificate[])req.getAttribute("javax.servlet.request.X509Certificate");

        if (certs==null || certs.length==0 || certs[0]==null) {
            throw SSOAuthenticatorServiceException.NO_CLIENT_CERTIFICATE();
        }

        validateClientCert(certs);

        return certs[0];
    }

    // Still called from nginx lookup servlet, TODO: retire
    public static Account getAccountByX509SubjectDN(String x509SubjectDN) throws ServiceException {
        try {
            LdapName dn = new LdapName(x509SubjectDN);
            List<Rdn> rdns = dn.getRdns();

            for (Rdn rdn : rdns) {
                String type = rdn.getType();

                // recognize only email address for now
                if ("EMAILADDRESS".equals(type)) {
                    Object value = rdn.getValue();
                    if (value != null) {
                        String email = value.toString();
                        Account acct = Provisioning.getInstance().get(AccountBy.name, email);
                        if (acct != null) {
                            return acct;
                        } else {
                            ZimbraLog.account.debug(LOG_PREFIX + "account not found: " + email);
                        }
                    }
                }
            }
        } catch (InvalidNameException e) {
            throw AuthFailedServiceException.AUTH_FAILED("ClientCertAuthenticator - invalid X509 subject: " + x509SubjectDN, e);
        }

        return null;
    }

    private String getSubjectDNForLogging(X509Certificate cert) {
        return CertValidationUtil.getSubjectDN(cert);
    }

    private void validateClientCert(X509Certificate[] certs) throws ServiceException {
            String subjectDN = null;
            try {
                boolean revocationCheckEnabled = Provisioning.getInstance().getLocalServer().isMailSSLClientCertOCSPEnabled();
                Set<TrustAnchor> trustedCertsSet = null;
                if (revocationCheckEnabled) {
                    char[] pass = LC.client_ssl_truststore_password.value().toCharArray();
                    trustedCertsSet = CertValidationUtil.loadTrustedAnchors(pass, LC.client_ssl_truststore.value());
                }

                for (X509Certificate cert : certs) {
                    subjectDN = getSubjectDNForLogging(cert);
                    CertValidationUtil.validateCertificate(cert, revocationCheckEnabled, trustedCertsSet);
                }
            } catch (CertificateExpiredException e) {
                throw AuthFailedServiceException.AUTH_FAILED(subjectDN, "client certificate expired", e);
            } catch (CertificateNotYetValidException e) {
                throw AuthFailedServiceException.AUTH_FAILED(subjectDN, "client certificate not yet valid", e);
            } catch (CertificateException e) {
                throw AuthFailedServiceException.AUTH_FAILED(subjectDN, "can't generate certpath for client certificate", e);
            } catch (KeyStoreException e) {
                throw AuthFailedServiceException.AUTH_FAILED(subjectDN, "received KeyStoreException while loading KeyStore", e);
            } catch (NoSuchAlgorithmException e) {
                throw AuthFailedServiceException.AUTH_FAILED(subjectDN, "received NoSuchAlgorithmException while obtaining instance of certpath validator", e);
            } catch (FileNotFoundException e) {
                throw AuthFailedServiceException.AUTH_FAILED(subjectDN, "mailboxd keystore can't be found", e);
            } catch (IOException e) {
                throw AuthFailedServiceException.AUTH_FAILED(subjectDN, "received IOException", e);
            } catch (InvalidAlgorithmParameterException e) {
                throw AuthFailedServiceException.AUTH_FAILED(subjectDN, "received InvalidAlgorithmParameter while obtaining instance of certpath validator", e);
            } catch (CertPathValidatorException e) {
                throw AuthFailedServiceException.AUTH_FAILED(subjectDN, "received CertPathValidatorException" + e.getMessage(), e);
            } 

    }

    // examine the certificate's AuthorityInfoAccess extension
    private boolean IsAIAInfoPresent(X509Certificate cert) {
        try {
            byte[] authInfoAccessExtensionValue = cert
                    .getExtensionValue(X509Extension.authorityInfoAccess.getId());
            ASN1InputStream ais1 = new ASN1InputStream(
                new ByteArrayInputStream(authInfoAccessExtensionValue));
            DEROctetString oct = (DEROctetString) (ais1.readObject());
            ASN1InputStream ais2 = new ASN1InputStream(oct.getOctets());
            AuthorityInformationAccess aia = AuthorityInformationAccess
                .getInstance(ais2.readObject());
            ais1.close();
            ais2.close();
            return (aia != null);
        } catch (IOException ce) {
            // treat this case as if the cert had no extension
            return false;
        }
    }

}
