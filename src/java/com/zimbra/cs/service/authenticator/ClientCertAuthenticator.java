/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.authenticator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sun.security.x509.AuthorityInfoAccessExtension;
import sun.security.x509.X509CertImpl;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.authenticator.ClientCertPrincipalMap.Rule;

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
        try {
            File file = new File(LC.zimbra_tmp_directory.value() + "/clientcert." + fmt.format(new Date()));
            
            // Get the encoded form which is suitable for exporting
            byte[] buf = cert.getEncoded();

            FileOutputStream os = new FileOutputStream(file);
            // Write in text form
            Writer wr = new OutputStreamWriter(os, Charset.forName("UTF-8"));
            wr.write("-----BEGIN CERTIFICATE-----\n");
            wr.write(new sun.misc.BASE64Encoder().encode(buf));
            wr.write("\n-----END CERTIFICATE-----\n");
            wr.flush();
            os.close();
        } catch (CertificateEncodingException e) {
            ZimbraLog.account.debug(LOG_PREFIX +  "unable to capture cert", e);
        } catch (IOException e) {
            ZimbraLog.account.debug(LOG_PREFIX + "unable to capture cert", e);
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
        String subjectDn = null;
        Principal principal = cert.getSubjectDN();
        if (principal != null) {
            subjectDn = principal.getName();
        }
        
        if (subjectDn == null) {
            subjectDn = "";
        }
        
        return subjectDn;
    }

    private void validateClientCert(X509Certificate[] certs) throws ServiceException {
        for (X509Certificate cert : certs) {
            try {
                cert.checkValidity();
                
                if( IsAIAInfoPresent(cert) ) {
                    ZimbraLog.account.debug(LOG_PREFIX +  "found AuthorityInfoAccess extension in client certificate");
                    List<X509Certificate> certificates = new ArrayList<X509Certificate>();
                    certificates.add(cert);

                    CertificateFactory    cf = CertificateFactory.getInstance("X509");
                    CertPath              cp = cf.generateCertPath(certificates);

                    KeyStore ks = KeyStore.getInstance("JKS");
                    char[] pass = LC.mailboxd_keystore_password.value().toCharArray();
                    ks.load(new FileInputStream(LC.mailboxd_keystore.value()), pass);

                    Set<TrustAnchor> trustedCertsSet = new HashSet<TrustAnchor>();
                    Enumeration<String> aliases = ks.aliases();
                    while (aliases.hasMoreElements()) {
                        String alias = (String) aliases.nextElement();

                        X509Certificate rootCACert = (X509Certificate)ks.getCertificate(alias);
                        TrustAnchor ta = new TrustAnchor(rootCACert, null);
                        trustedCertsSet.add(ta);

                        ZimbraLog.account.debug(LOG_PREFIX +  "adding certificate with issuer DN:" + rootCACert.getIssuerDN().toString() + " signature name:"  + rootCACert.getSigAlgName());
                      }

                    // init PKIX parameters
                    PKIXParameters params = new PKIXParameters(trustedCertsSet);

                    // Activate certificate revocation checking
                    params.setRevocationEnabled(true);

                    // Ensure that the ocsp.responderURL property is not set.
                    if (Security.getProperty("ocsp.responderURL") != null) {
                        throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "ocsp.responderURL property should not be set");
                    }

                    // perform validation
                    CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
                    PKIXCertPathValidatorResult cpv_result = (PKIXCertPathValidatorResult) cpv.validate(cp, params);

                    ZimbraLog.account.debug(LOG_PREFIX +  cpv_result.toString());
                }
            } catch (CertificateExpiredException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "client certificate expired", e);
            } catch (CertificateNotYetValidException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "client certificate not yet valid", e);
            } catch (CertificateException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "can't generate certpath for client certificate", e);
            } catch (KeyStoreException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "received KeyStoreException while loading KeyStore", e);
            } catch (NoSuchAlgorithmException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "received NoSuchAlgorithmException while obtaining instance of certpath validator", e);
            } catch (FileNotFoundException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "mailboxd keystore can't be found", e);
            } catch (IOException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "received IOException", e);
            } catch (InvalidAlgorithmParameterException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "received InvalidAlgorithmParameter while obtaining instance of certpath validator", e);
            } catch (CertPathValidatorException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "received CertPathValidatorException while performing OCSP validation:" + e.getMessage(), e);}

          }
    }

    // examine the certificate's AuthorityInfoAccess extension
    private boolean IsAIAInfoPresent(X509Certificate cert) {
        try {
            AuthorityInfoAccessExtension aia = 
                            X509CertImpl.toImpl(cert).getAuthorityInfoAccessExtension();
            return  (aia != null);
        } catch (CertificateException ce) {
            // treat this case as if the cert had no extension
            return false;
            }
    }

}
