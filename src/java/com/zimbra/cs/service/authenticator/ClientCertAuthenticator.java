/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.localconfig.DebugConfig;
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
        // CertificateValidator.validateClientCert(certs);
        for (X509Certificate cert : certs) {
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "client certificate expired", e);
            } catch (CertificateNotYetValidException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "client certificate not yet valid", e);
            }
        }
    }

}
