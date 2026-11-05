/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.service.util;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.OAuthConstants;
import com.zimbra.soap.SoapServlet;

public final class OAuthUtil {
    private OAuthUtil() {

    }
    public static boolean isOAuthRequested(Element request, Map<String, Object> context) {

        HttpServletRequest httpRequest = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        if (httpRequest != null) {
            String headerVal = httpRequest.getHeader(OAuthConstants.HEADER_AUTH_TYPE);
            if (OAuthConstants.AUTH_TYPE_ZM_OAUTH2.equalsIgnoreCase(headerVal)) {
                return true;
            }
            if ("true".equalsIgnoreCase(httpRequest.getHeader(OAuthConstants.AUTH_TYPE_ZM_OAUTH2))) {
                return true;
            }
        }

        String authTypeAttr = request.getAttribute("authType", null);
        if (OAuthConstants.AUTH_TYPE_ZM_OAUTH2.equalsIgnoreCase(authTypeAttr)) {
            return true;
        }
        return false;
    }
}