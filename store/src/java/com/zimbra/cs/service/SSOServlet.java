/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.service;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.authenticator.SSOAuthenticator.ZimbraPrincipal;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.servlet.util.AuthUtil;

public abstract class SSOServlet extends ZimbraServlet {

    protected abstract boolean redirectToRelativeURL();

    public void init() throws ServletException {
        String name = getServletName();
        ZimbraLog.account.info("Servlet " + name + " starting up");
        super.init();
    }

    public void destroy() {
        String name = getServletName();
        ZimbraLog.account.info("Servlet " + name + " shutting down");
        super.destroy();
    }

    protected AuthToken authorize(HttpServletRequest req, AuthContext.Protocol proto,
            ZimbraPrincipal principal, boolean isAdminRequest)
    throws ServiceException {

        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, ZimbraServlet.getOrigIp(req));
        authCtxt.put(AuthContext.AC_REMOTE_IP, ZimbraServlet.getClientIp(req));
        authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, principal.getName());
        authCtxt.put(AuthContext.AC_USER_AGENT, req.getHeader("User-Agent"));

        Provisioning prov = Provisioning.getInstance();
        Account acct = principal.getAccount();

        ZimbraLog.addAccountNameToContext(acct.getName());

        prov.ssoAuthAccount(acct, proto, authCtxt);

        if (isAdminRequest) {
            if (!AccessManager.getInstance().isAdequateAdminAccount(acct)) {
                throw ServiceException.PERM_DENIED("not an admin account");
            }
        }

        AuthToken authToken = AuthProvider.getAuthToken(acct, isAdminRequest);

        /*
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", authType, "account", acct.getName(), "admin", isAdminRequest+""}));
        */

        return authToken;
    }

    protected boolean isOnAdminPort(HttpServletRequest req) throws ServiceException {
        int adminPort = Provisioning.getInstance().getLocalServer().getAdminPort();
        return req.getLocalPort() == adminPort;
    }

    protected boolean isFromZCO(HttpServletRequest req) throws ServiceException {
        final String UA_ZCO = "Zimbra-ZCO";
        String userAgent = req.getHeader("User-Agent");
        return userAgent.contains(UA_ZCO);
    }

    protected void setAuthTokenCookieAndReturn(HttpServletRequest req, HttpServletResponse resp,
            AuthToken authToken)
    throws IOException, ServiceException {

        boolean isAdmin = AuthToken.isAnyAdmin(authToken);
        boolean secureCookie = isProtocolSecure(req.getScheme());
        authToken.encode(resp, isAdmin, secureCookie);
        resp.setContentLength(0);
    }

    private String appendIgnoreLoginURL(String redirectUrl) {
        if (!redirectUrl.endsWith("/")) {
            redirectUrl = redirectUrl + "/";
        }
        return redirectUrl + AuthUtil.IGNORE_LOGIN_URL;
    }

    protected void setAuthTokenCookieAndRedirect(HttpServletRequest req, HttpServletResponse resp,
            Account acct, AuthToken authToken)
    throws IOException, ServiceException {

        boolean isAdmin = AuthToken.isAnyAdmin(authToken);
        boolean secureCookie = isProtocolSecure(req.getScheme());
        authToken.encode(resp, isAdmin, secureCookie);

        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getServer(acct);
        boolean relative = redirectToRelativeURL();
        String redirectUrl = AuthUtil.getRedirectURL(req, server, isAdmin, relative);

        // always append the ignore loginURL query so we do not get into a redirect loop.
        redirectUrl = appendIgnoreLoginURL(redirectUrl);

        if (!relative) {
            URL url = new URL(redirectUrl);
            boolean isRedirectProtocolSecure = isProtocolSecure(url.getProtocol());

            if (secureCookie && !isRedirectProtocolSecure) {
                throw ServiceException.INVALID_REQUEST(
                        "cannot redirect to non-secure protocol: " + redirectUrl, null);
            }
        }

        ZimbraLog.account.debug("SSOServlet - redirecting (with auth token) to: " + redirectUrl);
        resp.sendRedirect(redirectUrl);
    }

    // Redirect to the specified error page without Zimbra auth token cookie.
    // The default error page is the webapp's regular entry page where user can
    // enter his username/password.
    protected void redirectToErrorPage(HttpServletRequest req, HttpServletResponse resp,
            boolean isAdminRequest, String errorUrl)
    throws IOException, ServiceException {
        String redirectUrl;

        if (errorUrl == null) {
            Server server = Provisioning.getInstance().getLocalServer();
            redirectUrl = AuthUtil.getRedirectURL(req, server, isAdminRequest, redirectToRelativeURL());

            // always append the ignore loginURL query so we do not get into a redirect loop.
            redirectUrl = appendIgnoreLoginURL(redirectUrl);

        } else {
            redirectUrl = errorUrl;
        }

        ZimbraLog.account.debug("SSOServlet - redirecting to: " + redirectUrl);
        resp.sendRedirect(redirectUrl);
    }

    private boolean isProtocolSecure(String protocol) {
        return URLUtil.PROTO_HTTPS.equalsIgnoreCase(protocol);
    }
}
