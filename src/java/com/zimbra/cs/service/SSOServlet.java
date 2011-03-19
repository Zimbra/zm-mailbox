package com.zimbra.cs.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.service.authenticator.SSOAuthenticator;
import com.zimbra.cs.service.authenticator.SSOAuthenticator.ZimbraPrincipal;
import com.zimbra.cs.servlet.ZimbraServlet;

public abstract class SSOServlet extends ZimbraServlet {
    
    protected AuthToken authorize(HttpServletRequest req, SSOAuthenticator authenticator, ZimbraPrincipal principal) 
    throws ServiceException {
        
        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, ZimbraServlet.getOrigIp(req));
        authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, principal.getName());
        authCtxt.put(AuthContext.AC_USER_AGENT, req.getHeader("User-Agent"));
        
        Provisioning prov = Provisioning.getInstance();
        Account acct = principal.getAccount();
        
        // use soap for the protocol for now. should we use a new protocol for each SSO method?
        prov.ssoAuthAccount(acct, AuthContext.Protocol.soap, authCtxt); 
        
        boolean admin = onAdminPort(req);
        if (admin) {
            if (!AccessManager.getInstance().isAdequateAdminAccount(acct)) {
                throw ServiceException.PERM_DENIED("not an admin account");
            }
        }
        
        AuthToken authToken = AuthProvider.getAuthToken(acct, admin);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", authenticator.getAuthMethod(), "account", acct.getName(), "admin", admin+""}));
        
        return authToken;
    }
    
    private boolean onAdminPort(HttpServletRequest req) throws ServiceException {
        int adminPort = Provisioning.getInstance().getLocalServer().getAdminPort();
        return req.getLocalPort() == adminPort;
    }
    
    protected void setCookieAndRedirect(HttpServletRequest req, HttpServletResponse resp, AuthToken authToken) 
    throws IOException, ServiceException {
        
        final String DEFAULT_MAIL_URL = "/zimbra";
        final String DEFAULT_ADMIN_URL = "/zimbraAdmin";
        
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
