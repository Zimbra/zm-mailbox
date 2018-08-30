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
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.EndSessionRequest;

/**
 * End the current session immediately cleaning up all resources used by the session
 * including the notification buffer and logging the session out from IM if applicable
 */
public class EndSession extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        EndSessionRequest req = JaxbUtil.elementToJaxb(request);
        String sessionId = req.getSessionId();
        boolean clearCookies = req.isLogOff();
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (StringUtil.isNullOrEmpty(sessionId)) {
            if (zsc.hasSession()) {
                Session s = getSession(zsc);
                endSession(s);
                if (clearCookies || getAuthenticatedAccount(zsc).isForceClearCookies()) {
                    AuthToken at = zsc.getAuthToken();
                    clearCookies(context, zsc, at);
                }
            }
        } else {
            Session s = SessionCache.lookup(sessionId, zsc.getAuthtokenAccountId());
            if (s == null) {
                throw ServiceException.FAILURE("Failed to find session with given sessionId", null);
            } else {
                endSession(s);
            }
            if (clearCookies || getAuthenticatedAccount(zsc).isForceClearCookies()) {
                if (s instanceof SoapSession) {
                    SoapSession ss = (SoapSession) s;
                    AuthToken at = ss.getAuthToken();
                    if (at != null) {
                        clearCookies(context, zsc, at);
                    }
                }
            }
        }
        Element response = zsc.createElement(AccountConstants.END_SESSION_RESPONSE);
        return response;
    }

    private void clearCookies(Map<String, Object> context, ZimbraSoapContext zsc, AuthToken at) throws ServiceException {
        context.put(SoapServlet.INVALIDATE_COOKIES, true);
        try {
            HttpServletRequest httpReq = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
            HttpServletResponse httpResp = (HttpServletResponse) context.get(SoapServlet.SERVLET_RESPONSE);
            at.encode(httpReq, httpResp, true);
            at.deRegister();
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("Failed to de-register an auth token", e);
        }
    }
}
