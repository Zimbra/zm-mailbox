/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.servlet.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.servlet.ZimbraServlet;

public class AuthUtil {
    private static Log mLog = LogFactory.getLog(AuthUtil.class);

    public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    public static final String HTTP_AUTH_HEADER = "Authorization";

    /**
     * Checks to see if this is an admin request
     * @param req
     * @return
     * @throws ServiceException
     */
    public static boolean isAdminRequest(HttpServletRequest req) throws ServiceException {
        int adminPort = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, -1);
        if (req.getLocalPort() == adminPort) {
            //can still be in offline server where port=adminPort
            int mailPort = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraMailPort, -1);
            if (mailPort == adminPort) //we are in offline, so check cookie
                return getAdminAuthTokenFromCookie(req) != null;
            else
                return true;
        }
        return false;
    }

    public static AuthToken getAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, false, false);
    }

    public static AuthToken getAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp, boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, false, doNotSendHttpError);
    }

    public static AuthToken getAuthTokenFromHttpReq(HttpServletRequest req, boolean isAdminReq) {
        AuthToken authToken = null;
        try {
            authToken = AuthProvider.getAuthToken(req, isAdminReq);
            if (authToken == null)
                return null;

            if (authToken.isExpired() || !authToken.isRegistered())
                return null;

            return authToken;
        } catch (AuthTokenException e) {
            return null;
        }
    }

    public static AuthToken getAuthTokenFromHttpReq(HttpServletRequest req,
                                                    HttpServletResponse resp,
                                                    boolean isAdminReq,
                                                    boolean doNotSendHttpError) throws IOException {
        AuthToken authToken = null;
        try {
            authToken = AuthProvider.getAuthToken(req, isAdminReq);
            if (authToken == null) {
                if (!doNotSendHttpError)
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "no authtoken cookie");
                return null;
            }

            if (authToken.isExpired() || !authToken.isRegistered()) {
                if (!doNotSendHttpError)
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "authtoken expired");
                return null;
            }
            return authToken;
        } catch (AuthTokenException e) {
            if (!doNotSendHttpError)
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "unable to parse authtoken");
            return null;
        }
    }

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req) {
        return getAuthTokenFromHttpReq(req, true);
    }

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, true, false);
    }

    public static AuthToken getAdminAuthTokenFromCookie(HttpServletRequest req, HttpServletResponse resp, boolean doNotSendHttpError)
    throws IOException {
        return getAuthTokenFromHttpReq(req, resp, true, doNotSendHttpError);
    }

    public static Account basicAuthRequest(HttpServletRequest req,
                                           HttpServletResponse resp,
                                           ZimbraServlet servlet,
                                           boolean sendChallenge) throws IOException, ServiceException
    {
        if (!AuthProvider.allowBasicAuth(req, servlet))
            return null;

        return basicAuthRequest(req, resp, sendChallenge);
    }

    public static Account basicAuthRequest(HttpServletRequest req, HttpServletResponse resp, boolean sendChallenge)
            throws IOException, ServiceException
    {
        try {
            return basicAuthRequest(req, !sendChallenge);
        } catch (UserServletException e) {
            if (e.getHttpStatusCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                if (sendChallenge) {
                    resp.addHeader(WWW_AUTHENTICATE_HEADER, getRealmHeader(req, null));
                    resp.sendError(e.getHttpStatusCode(), e.getMessage());
                }
            } else {
                resp.sendError(e.getHttpStatusCode(), e.getMessage());
            }
            return null;
        }
    }

    public static Account basicAuthRequest(HttpServletRequest req, boolean allowGuest)
        throws IOException, ServiceException, UserServletException
    {
        String auth = req.getHeader(HTTP_AUTH_HEADER);

        // TODO: more liberal parsing of Authorization value...
        if (auth == null || !auth.startsWith("Basic ")) {
            throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED, "must authenticate");
        }

        // 6 comes from "Basic ".length();
        String userPass = new String(Base64.decodeBase64(auth.substring(6).getBytes()), "UTF-8");

        int loc = userPass.indexOf(":");
        if (loc == -1) {
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "invalid basic auth credentials");
        }

        String userPassedIn = userPass.substring(0, loc);
        String user = userPassedIn;
        String pass = userPass.substring(loc + 1);

        Provisioning prov = Provisioning.getInstance();

        if (user.indexOf('@') == -1) {
            String host = HttpUtil.getVirtualHost(req);
            if (host != null) {
                Domain d = prov.get(Key.DomainBy.virtualHostname, host.toLowerCase());
                if (d != null) user += "@" + d.getName();
            }
        }

        Account acct = prov.get(AccountBy.name, user);

        if (acct == null) {
            if (allowGuest) {
                return new GuestAccount(user, pass);
            }

            throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED, "invalid username/password");
        }
        try {
            Map<String, Object> authCtxt = new HashMap<String, Object>();
            authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, ZimbraServlet.getOrigIp(req));
            authCtxt.put(AuthContext.AC_REMOTE_IP, ZimbraServlet.getClientIp(req));
            authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, userPassedIn);
            authCtxt.put(AuthContext.AC_USER_AGENT, req.getHeader("User-Agent"));
            prov.authAccount(acct, pass, AuthContext.Protocol.http_basic, authCtxt);
        } catch (ServiceException se) {
            throw new UserServletException(HttpServletResponse.SC_UNAUTHORIZED, "invalid username/password");
        }

        return acct;
    }

    public AuthToken cookieAuthRequest(HttpServletRequest req, HttpServletResponse resp)
    throws IOException, ServiceException {
        AuthToken at = AuthUtil.isAdminRequest(req) ? AuthUtil.getAdminAuthTokenFromCookie(req, resp, true) : AuthUtil.getAuthTokenFromCookie(req, resp, true);
        return at;
    }

    public static String getRealmHeader(HttpServletRequest req, Domain domain)  {
        String realm = null;

        if (domain == null) {
            // get domain by virtual host
            String host = HttpUtil.getVirtualHost(req);
            if (host != null) {
                // to defend against DOS attack, use the negative domain cache
                try {
                    domain = Provisioning.getInstance().getDomain(Key.DomainBy.virtualHostname, host.toLowerCase(), true);
                } catch (ServiceException e) {
                    mLog.warn("caught exception while getting domain by virtual host: " + host, e);
                }
            }
        }

        if (domain != null)
            realm = domain.getBasicAuthRealm();

        return getRealmHeader(realm);
    }

    public static String getRealmHeader(String realm)  {
        if (realm == null)
            realm = "Zimbra";
        return "BASIC realm=\"" + realm + "\"";
    }

}
