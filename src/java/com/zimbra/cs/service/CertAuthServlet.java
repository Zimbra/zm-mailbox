package com.zimbra.cs.service;

import java.io.IOException;

import java.util.List;

import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.X509TrustManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.net.CertificateValidator;
import com.zimbra.common.net.TrustManagers;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.servlet.ZimbraServlet;

public class CertAuthServlet extends ZimbraServlet {

    private static final String DEFAULT_MAIL_URL = "/zimbra";
    private static final String DEFAULT_ADMIN_URL = "/zimbraAdmin";
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        try {
            Account acct = getAccountByX509Subject(req);
            
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
            ZimbraLog.account.warn("failed to authenticate by client certificate", e.getMessage());
            ZimbraLog.account.debug("failed to authenticate by client certificate", e);
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }
    
    
    private boolean onAdminPort(HttpServletRequest req) throws ServiceException {
        int adminPort = Provisioning.getInstance().getLocalServer().getAdminPort();
        return req.getLocalPort() == adminPort;
    }
    
    private Account getAccountByX509Subject(HttpServletRequest request) throws ServiceException {
        
        String x509Subject = getX509Subject(request);
        
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
