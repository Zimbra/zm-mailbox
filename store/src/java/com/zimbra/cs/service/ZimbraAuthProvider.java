/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.TokenType;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.ZimbraJWToken;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.service.util.JWTUtil;
import com.zimbra.soap.SoapServlet;

public class ZimbraAuthProvider extends AuthProvider {

    public static final String ZIMBRA_AUTH_PROVIDER = "zimbra";

    ZimbraAuthProvider() {
        this(ZIMBRA_AUTH_PROVIDER);
    }

    protected ZimbraAuthProvider(String name) {
        super(name);
    }

    private String getEncodedAuthTokenFromCookie(HttpServletRequest req, boolean isAdminReq) {
        String cookieName = ZimbraCookie.authTokenCookieName(isAdminReq);
        String encodedAuthToken = null;
        javax.servlet.http.Cookie cookies[] =  req.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(cookieName)) {
                    encodedAuthToken = cookies[i].getValue();
                    break;
                }
            }
        }
        return encodedAuthToken;
    }

    @Override
    protected AuthToken authToken(HttpServletRequest req, boolean isAdminReq)
    throws AuthProviderException, AuthTokenException {
        String encodedAuthToken = getEncodedAuthTokenFromCookie(req, isAdminReq);
        return genAuthToken(encodedAuthToken);
    }

    @Override
    protected AuthToken authToken(Element soapCtxt, Map engineCtxt)
    throws AuthProviderException, AuthTokenException  {
        String encodedAuthToken = soapCtxt == null ? null :
            soapCtxt.getAttribute(HeaderConstants.E_AUTH_TOKEN, null);

        // check for auth token in engine context if not in soap header
        if (encodedAuthToken == null) {
            encodedAuthToken = (String) engineCtxt.get(SoapServlet.ZIMBRA_AUTH_TOKEN);
        }

        // if still not found, see if it is in the servlet request
        if (encodedAuthToken == null) {
            HttpServletRequest req = (HttpServletRequest) engineCtxt.get(SoapServlet.SERVLET_REQUEST);
            if (req != null) {
                Boolean isAdminReq = (Boolean) engineCtxt.get(SoapServlet.IS_ADMIN_REQUEST);
                if (isAdminReq != null) {
                    // get auth token from cookie only if we can determine if this is an admin request
                    encodedAuthToken = getEncodedAuthTokenFromCookie(req, isAdminReq);
                }
            }
        }

        return genAuthToken(encodedAuthToken);
    }

    @Override
    protected AuthToken authToken(String encoded) throws AuthProviderException, AuthTokenException {
        return genAuthToken(encoded);
    }

    protected AuthToken genAuthToken(String encodedAuthToken) throws AuthProviderException, AuthTokenException {
        if (StringUtil.isNullOrEmpty(encodedAuthToken)) {
            throw AuthProviderException.NO_AUTH_DATA();
        }

        return ZimbraAuthToken.getAuthToken(encodedAuthToken);
    }

    @Override
    protected AuthToken authToken(Account acct) {
        return new ZimbraAuthToken(acct);
    }

    @Override
    protected AuthToken authToken(Account acct, TokenType tokenType) {
        if (TokenType.JWT.equals(tokenType)) {
            return new ZimbraJWToken(acct);
        } else {
            return authToken(acct);
        }
    }

    @Override
    protected AuthToken authToken(Account acct, boolean isAdmin, AuthMech authMech) {
        return new ZimbraAuthToken(acct, isAdmin, authMech);
    }

    @Override
    protected AuthToken authToken(Account acct, long expires) {
        return new ZimbraAuthToken(acct, expires);
    }

    @Override
    protected AuthToken authToken(Account acct, long expires, TokenType tokenType) {
        if (TokenType.JWT.equals(tokenType)) {
            return new ZimbraJWToken(acct, expires);
        } else {
            return authToken(acct, expires);
        }
    }

    @Override
    protected AuthToken authToken(Account acct, long expires, boolean isAdmin,
            Account adminAcct) {
        return new ZimbraAuthToken(acct, expires, isAdmin, adminAcct, null);
    }

    @Override
    protected AuthToken authToken(Account acct, Usage usage) throws AuthProviderException {
        return new ZimbraAuthToken(acct, usage);
    }

    protected AuthToken authToken(Account acct, Usage usage, TokenType tokenType) throws AuthProviderException {
        if (TokenType.JWT.equals(tokenType)) {
            return new ZimbraJWToken(acct, usage);
        } else {
            return authToken(acct, usage);
        }
    }

    @Override
    protected AuthToken jwToken(Element soapCtxt, Map engineCtxt)
    throws AuthProviderException, AuthTokenException {
        AuthToken at = null;
        String jwt = soapCtxt == null ? null : soapCtxt.getAttribute(HeaderConstants.E_JWT_TOKEN, null);
        if (jwt == null && engineCtxt != null) {
            logger().debug("jwt not found in soap context");
            HttpServletRequest req = (HttpServletRequest) engineCtxt.get(SoapServlet.SERVLET_REQUEST);
            if (req != null) {
                String authorization = req.getHeader(Constants.AUTH_HEADER);
                if(!StringUtil.isNullOrEmpty(authorization)) {
                    String[] arr = authorization.split(" ");
                    if (arr.length == 2 && Constants.BEARER.equals(arr[0])) {
                        jwt = arr[1];
                    } else {
                        logger().debug("authorization header doesn't have bearer");
                    }
                } else {
                    logger().debug("authorization header not found");
                }
            }
        }
        if (!StringUtil.isNullOrEmpty(jwt)) {
            at = jwToken(jwt, JWTUtil.getSalt(soapCtxt, engineCtxt));
        } else {
            throw AuthProviderException.NO_AUTH_DATA();
        }
        return at;
    }

    @Override
    protected AuthToken jwToken(String jwt, String currentSalt)
    throws AuthProviderException, AuthTokenException {
        if (StringUtil.isNullOrEmpty(jwt) || StringUtil.isNullOrEmpty(currentSalt)) {
            throw AuthProviderException.NO_AUTH_DATA();
        }
        return ZimbraJWToken.getJWToken(jwt, currentSalt);
    }
}
