package com.zimbra.cs.service;

import java.io.IOException;
import java.net.URL;
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
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.authenticator.SSOAuthenticator;
import com.zimbra.cs.service.authenticator.SSOAuthenticator.ZimbraPrincipal;
import com.zimbra.cs.servlet.ZimbraServlet;

public abstract class SSOServlet extends ZimbraServlet {
    
    protected AuthToken authorize(HttpServletRequest req, SSOAuthenticator authenticator, 
            ZimbraPrincipal principal, boolean isAdminRequest) 
    throws ServiceException {
        
        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, ZimbraServlet.getOrigIp(req));
        authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, principal.getName());
        authCtxt.put(AuthContext.AC_USER_AGENT, req.getHeader("User-Agent"));
        
        Provisioning prov = Provisioning.getInstance();
        Account acct = principal.getAccount();
        
        // use soap for the protocol for now. should we use a new protocol for each SSO method?
        prov.ssoAuthAccount(acct, AuthContext.Protocol.soap, authCtxt); 
        
        if (isAdminRequest) {
            if (!AccessManager.getInstance().isAdequateAdminAccount(acct)) {
                throw ServiceException.PERM_DENIED("not an admin account");
            }
        }
        
        AuthToken authToken = AuthProvider.getAuthToken(acct, isAdminRequest);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", authenticator.getAuthMethod(), "account", acct.getName(), "admin", isAdminRequest+""}));
        
        return authToken;
    }
    
    protected boolean onAdminPort(HttpServletRequest req) throws ServiceException {
        int adminPort = Provisioning.getInstance().getLocalServer().getAdminPort();
        return req.getLocalPort() == adminPort;
    }
    
    protected void setAuthTokenCookieAndRedirect(HttpServletRequest req, HttpServletResponse resp, 
            Account acct, AuthToken authToken) 
    throws IOException, ServiceException {
        
        boolean isAdmin = AuthToken.isAnyAdmin(authToken);
        boolean secureCookie = isProtocolSecure(req.getScheme());
        authToken.encode(resp, isAdmin, secureCookie);

        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getServer(acct);
        
        String redirectUrl;
        if (isAdmin) {
            redirectUrl = getAdminUrl(server);
        } else {
            redirectUrl = getMailUrl(server);
        }
        
        URL url = new URL(redirectUrl);
        boolean isRedirectProtocolSecure = isProtocolSecure(url.getProtocol());
        
        if (secureCookie && !isRedirectProtocolSecure) {
            throw ServiceException.INVALID_REQUEST("cannot redirect to non-secure protocol: " + redirectUrl, null);
        }
        
        resp.sendRedirect(redirectUrl);
    }
    
    // redirect to the webapp's regular entry page without an auth token cookie
    // the default behavior is the login/password page
    protected void redirectWithoutAuthTokenCookie(HttpServletRequest req, HttpServletResponse resp, boolean isAdminRequest) 
    throws IOException, ServiceException {
        
        // append the ignore loginURL query so we do not get into a redirect loop.
        final String IGNORE_LOGIN_URL = "/?ignoreLoginURL=1";
        
        Server server = Provisioning.getInstance().getLocalServer();
        String redirectUrl;
        if (isAdminRequest) {
            redirectUrl = getAdminUrl(server) + IGNORE_LOGIN_URL; // not yet supported for admin console
        } else {
            redirectUrl = getMailUrl(server) + IGNORE_LOGIN_URL;
        }
        
        URL url = new URL(redirectUrl);
        boolean isRedirectProtocolSecure = isProtocolSecure(url.getProtocol());
        
        resp.sendRedirect(redirectUrl);
    }
    
    
    private boolean isProtocolSecure(String protocol) {
        return URLUtil.PROTO_HTTPS.equalsIgnoreCase(protocol);
    }
    
    protected String getMailUrl(Server server) throws ServiceException {
        final String DEFAULT_MAIL_URL = "/zimbra";
    
        String serviceUrl = server.getAttr(Provisioning.A_zimbraMailURL, DEFAULT_MAIL_URL);
        return URLUtil.getServiceURL(server, serviceUrl, true);
    }
    
    protected String getAdminUrl(Server server) throws ServiceException {
        final String DEFAULT_ADMIN_URL = "/zimbraAdmin";
        
        String serviceUrl = server.getAttr(Provisioning.A_zimbraAdminURL, DEFAULT_ADMIN_URL);
        return URLUtil.getAdminURL(server, serviceUrl, true);
    }
}
