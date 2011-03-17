/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.service;

import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.security.Principal;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.soap.SoapEngine;

public class CertAuthServlet extends ZimbraServlet {

    private static final String DEFAULT_MAIL_URL = "/zimbra";
    private static final String DEFAULT_ADMIN_URL = "/zimbraAdmin";
    
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ZimbraLog.clearContext();
        addRemoteIpToLoggingContext(req);
        ZimbraLog.addUserAgentToContext(req.getHeader("User-Agent"));
        
        try {
            String x509Subject = getX509Subject(req);
            Account acct = getAccountByX509Subject(x509Subject);
            
            Map<String, Object> authCtxt = new HashMap<String, Object>();
            authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, ZimbraServlet.getOrigIp(req));
            authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, x509Subject);
            authCtxt.put(AuthContext.AC_USER_AGENT, req.getHeader("User-Agent"));
            
            Provisioning prov = Provisioning.getInstance();
            
            // use soap for the protocol for now. should we use a new protocol "cert"?  
            prov.certAuthAccount(acct, AuthContext.Protocol.soap, authCtxt); 
            
            boolean admin = onAdminPort(req);
            if (admin) {
                if (!AccessManager.getInstance().isAdequateAdminAccount(acct)) {
                    throw ServiceException.PERM_DENIED("not an admin account");
                }
            }
            
            AuthToken authToken = AuthProvider.getAuthToken(acct, admin);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "CertAuth","account", acct.getName(), "admin", admin+""}));
            
            setCookieAndRedirect(req, resp, authToken);
        } catch (ServiceException e) {
            if (e instanceof AuthFailedServiceException) {
                AuthFailedServiceException afe = (AuthFailedServiceException)e;
                ZimbraLog.account.info("failed to authenticate by client certificate: " + afe.getMessage() + afe.getReason(", %s"));
            } else {
                ZimbraLog.account.info("failed to authenticate by client certificate: " + e.getMessage());
            }
            ZimbraLog.account.debug("failed to authenticate by client certificate", e);
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
    
    private boolean onAdminPort(HttpServletRequest req) throws ServiceException {
        int adminPort = Provisioning.getInstance().getLocalServer().getAdminPort();
        return req.getLocalPort() == adminPort;
    }
    
    private Account getAccountByX509Subject(String x509Subject) throws ServiceException {
        try {
            LdapName dn = new LdapName(x509Subject);
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
                            ZimbraLog.account.debug("account not found: " + email);
                        }
                    }
                }
            }
        } catch (InvalidNameException e) {
            throw ServiceException.INVALID_REQUEST("invalid X509 subject: " + x509Subject, e);
        }
        
        throw ServiceException.DEFEND_ACCOUNT_HARVEST(x509Subject);
    }
    
    private String getX509Subject(HttpServletRequest request) throws ServiceException {
        X509Certificate[] certs = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
            
        if (certs==null || certs.length==0 || certs[0]==null) {
            throw ServiceException.INVALID_REQUEST("no client certificate", null);
        }
        
        validateClientCert(certs);
        
        Principal principal = certs[0].getSubjectDN();
        
        if (principal == null) {
            throw ServiceException.INVALID_REQUEST("missing subject in cert", null);
        }
        
        String subjectDn = principal.getName();
        if (subjectDn == null) {
            throw ServiceException.INVALID_REQUEST("missing name in subject", null);
        }
        
        return subjectDn;
    }
    
    private void validateClientCert(X509Certificate[] certs) throws ServiceException {
        // CertificateValidator.validateClientCert(certs);
        for (X509Certificate cert : certs) {
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException e) {
                throw ServiceException.INVALID_REQUEST("client certificate expired", e);
            } catch (CertificateNotYetValidException e) {
                throw ServiceException.INVALID_REQUEST("client certificate not yet valid", e);
            }
        }
    }
    
    private void setCookieAndRedirect(HttpServletRequest req, HttpServletResponse resp, AuthToken authToken) 
    throws IOException, ServiceException {
        boolean isAdmin = AuthToken.isAnyAdmin(authToken);
        boolean secureCookie = req.getScheme().equals("https");
        authToken.encode(resp, isAdmin, secureCookie);

        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        String redirectUrl;

        if (isAdmin) {
            redirectUrl = server.getAttr(Provisioning.A_zimbraAdminURL, DEFAULT_ADMIN_URL);
        } else {
            redirectUrl = server.getAttr(Provisioning.A_zimbraMailURL, DEFAULT_MAIL_URL);
        }
        
        resp.sendRedirect(redirectUrl);
    }
    
}
