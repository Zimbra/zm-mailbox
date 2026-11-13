/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.util;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.OAuthConstants;
import com.zimbra.soap.SoapServlet;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public final class OAuthUtil {
    private OAuthUtil() {
    }

    public static boolean isOAuthRequested(Element request, Map<String, Object> context) {
        if (context != null) {
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
        }
        if (request != null) {
            String authTypeAttr = request.getAttribute(OAuthConstants.HEADER_AUTH_TYPE, null);
            if (OAuthConstants.AUTH_TYPE_ZM_OAUTH2.equalsIgnoreCase(authTypeAttr)) {
                return true;
            }
        }
        return false;
    }
}
