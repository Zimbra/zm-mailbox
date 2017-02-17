/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.servlet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.AuthProvider;

/**
 * Jetty authenticator which implements Zimbra auth token in addition to HTTP BASIC
 *
 */
public class ZimbraAuthenticator extends BasicAuthenticator {

    protected String urlPattern = "";

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern == null ? null : urlPattern.replace("//","/");
    }

    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse resp, boolean mandatory)
                    throws ServerAuthException {
        if (mandatory && req instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) req;

            //url pattern is mostly redundant with web.xml security-constraint declaration
            //however jetty does make upcall into authenticator from DoSFilter and other sites to find login username for logging
            //we want to just ignore rather than potentially flooding auth provider (which may be external)
            if (PathMap.match(urlPattern, httpReq.getRequestURI())) {
                Cookie[] cookies = httpReq.getCookies();

                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (ZimbraCookie.authTokenCookieName(true).equalsIgnoreCase(cookie.getName())
                                        || ZimbraCookie.authTokenCookieName(false).equalsIgnoreCase(cookie.getName())) {
                            String encoded = cookie.getValue();
                            AuthToken token;
                            try {
                                token = AuthProvider.getAuthToken(encoded);
                                Account authAcct = AuthProvider.validateAuthToken(Provisioning.getInstance(), token, false);
                                if (authAcct != null) {
                                    if (_loginService instanceof ZimbraLoginService) {
                                        UserIdentity user = ((ZimbraLoginService) _loginService).makeUserIdentity(authAcct.getMail());
                                        ZimbraLog.security.debug("Auth token validated");
                                        return new UserAuthentication(getAuthMethod(), user);
                                    } else {
                                        ZimbraLog.security.warn("Misconfigured? _loginService not ZimbraLoginService");
                                        assert(false);
                                    }
                                }
                            } catch (AuthTokenException e) {
                                ZimbraLog.security.error("Unable to authenticate due to AuthTokenException", e);
                            } catch (ServiceException e) {
                                ZimbraLog.security.error("Unable to authenticate due to ServiceException", e);
                            }
                        }
                    }
                    ZimbraLog.security.debug("no valid auth token, fallback to basic");
                }
            }
        }
        return super.validateRequest(req, resp, mandatory);
    }
}
