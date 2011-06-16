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

import java.security.Principal;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.authenticator.ClientCertPrincipalMap.CertField;
import com.zimbra.cs.service.authenticator.ClientCertPrincipalMap.Rule;
import com.zimbra.cs.service.authenticator.ClientCertPrincipalMap.ZimbraKey;

public class ClientCertAuthenticator extends SSOAuthenticator {

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

        ClientCertPrincipalMap principalMap = new ClientCertPrincipalMap(req);
        List<Rule> rules = principalMap.getRules();
        
        String certFieldValue = null;
        Account acct = null;
        
        for (Rule rule : rules) {
            try {
                ZimbraLog.account.debug("ClientCertAuthenticator - Attempting rule " + rule.toString());
                
                certFieldValue = getCertField(rule.getCertFiled(), cert);
                if (certFieldValue != null) {
                    acct = getZimbraAccount(rule.getZimbraKey(), rule.getCertFiled(), certFieldValue);
                    if (acct != null) {
                        return new ZimbraPrincipal(certFieldValue, acct);
                    }
                }
            } catch (ServiceException e) {
                ZimbraLog.account.debug("ClientCertAuthenticator - Rule " + rule.toString() + " not applied", e);
            }
        }
        
        throw AuthFailedServiceException.AUTH_FAILED(cert.toString(),
                "ClientCertAuthenticator - no matching Zimbra principal from client certificate.", (Throwable)null);
    }
    
    private X509Certificate getCert() throws ServiceException {
        X509Certificate[] certs = (X509Certificate[])req.getAttribute("javax.servlet.request.X509Certificate");
        
        if (certs==null || certs.length==0 || certs[0]==null) {
            throw SSOAuthenticatorServiceException.NO_CLIENT_CERTIFICATE();
        }
        
        return certs[0];
    }
    
    private String getCertField(CertField certField, X509Certificate cert) throws ServiceException {
        CertUtil certUtil = new CertUtil(cert);
        return certUtil.getCertField(certField);
    }
    
    private Account getZimbraAccount(ZimbraKey zimbraKey, CertField certField, String certFieldValue) {
        ZimbraLog.account.debug("ClientCertAuthenticator - get account by " +
                zimbraKey.name() + ", " + certField.name() + "=" + certFieldValue);
        
        Provisioning prov = Provisioning.getInstance();
        Account acct = null;
        
        try {
            switch (zimbraKey) {
                case name:
                    acct = prov.get(AccountBy.name, certFieldValue);
                    break;
                case zimbraId:
                    acct = prov.get(AccountBy.id, certFieldValue);
                    break;
                case zimbraForeignPrincipal:
                    String foreignPrincipal = 
                        String.format(Provisioning.FP_PREFIX_CERT, certField.name(),certFieldValue);
                    acct = prov.get(AccountBy.foreignPrincipal, foreignPrincipal);
                    break;
            }
        } catch (ServiceException e) {
            ZimbraLog.account.debug("ClientCertAuthenticator - no matching account by " +
                    zimbraKey.name() + ", " + certField.name() + "=" + certFieldValue, e);
        }
        return acct;
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
                            ZimbraLog.account.debug("ClientCertAuthenticator - account not found: " + email);
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
