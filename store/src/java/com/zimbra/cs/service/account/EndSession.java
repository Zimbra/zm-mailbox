/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.account;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.JWTCache;
import com.zimbra.cs.account.ZimbraJWT;
import com.zimbra.cs.service.util.JWTUtil;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * End the current session immediately cleaning up all resources used by the session
 * including the notification buffer and logging the session out from IM if applicable
 */
public class EndSession extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (zsc.hasSession()) {
            Session s = getSession(zsc);
            endSession(s);
        }
        boolean clearCookies = request.getAttributeBool(AccountConstants.A_LOG_OFF, false);
        if (clearCookies || getAuthenticatedAccount(zsc).isForceClearCookies()) {
            context.put(SoapServlet.INVALIDATE_COOKIES, true);
            try {
                AuthToken at = zsc.getAuthToken();
                if (at.isJWT()) {
                    String jti = at.getId();
                    if (jti != null) {
                        ZimbraLog.security.debug("EndSession: jti: %s",jti);
                        ZimbraJWT jwt = JWTCache.get(jti);
                        if (jwt != null) {
                            String salt = jwt.getSalt();
                            ZimbraLog.security.debug("EndSession: found salt in cache for jti: %s",jti);
                            String zmJWTCookieValue = null;
                            HttpServletRequest httpReq = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
                            HttpServletResponse httpResp = (HttpServletResponse) context.get(SoapServlet.SERVLET_RESPONSE);
                            if (salt != null) {
                                zmJWTCookieValue = JWTUtil.getZMJWTCookieValue(httpReq);
                            }
                            ZimbraCookie.addHttpOnlyCookie(httpResp, Constants.ZM_JWT_COOKIE, JWTUtil.clearSalt(zmJWTCookieValue, salt), ZimbraCookie.PATH_ROOT, -1, true);
                            JWTCache.remove(jti);
                        }
                    }
                }
                at.deRegister();
            } catch (AuthTokenException e) {
                throw ServiceException.FAILURE("Failed to de-register an auth token", e);
            }
        }
        Element response = zsc.createElement(AccountConstants.END_SESSION_RESPONSE);
        return response;
    }
}
