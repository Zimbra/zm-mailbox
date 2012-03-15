/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;

public class ZimbraCookie {

    public static final String COOKIE_ZM_AUTH_TOKEN       = "ZM_AUTH_TOKEN";
    public static final String COOKIE_ZM_ADMIN_AUTH_TOKEN = "ZM_ADMIN_AUTH_TOKEN";
    
    public static String PATH_ROOT = "/";

    public static String authTokenCookieName(boolean isAdminReq) {
        return isAdminReq? 
                COOKIE_ZM_ADMIN_AUTH_TOKEN : 
                COOKIE_ZM_AUTH_TOKEN;
    }
    
    /**
     * set cookie domain and path for the cookie going back to the browser
     * 
     * @param cookie
     * @param path
     */
    public static void setAuthTokenCookieDomainPath(Cookie cookie, String path) {
        if (LC.zimbra_authtoken_cookie_domain.value().length() > 0) {
            cookie.setDomain(LC.zimbra_authtoken_cookie_domain.value());
        }
        
        cookie.setPath(path);
    }
    
    public static boolean secureCookie(HttpServletRequest request) {
        return "https".equalsIgnoreCase(request.getScheme());
    }
    
    public static void addHttpOnlyCookie(HttpServletResponse response, String name, String value, 
            String path, Integer maxAge, boolean secure) {
        addCookie(response, name, value, path, maxAge, true, secure);
    }
    
    private static void addCookie(HttpServletResponse response, String name, String value, 
            String path, Integer maxAge, boolean httpOnly, boolean secure) {
        Cookie cookie = new Cookie(name, value);

        if (maxAge != null) {
            cookie.setMaxAge(maxAge.intValue());
        }
        ZimbraCookie.setAuthTokenCookieDomainPath(cookie, ZimbraCookie.PATH_ROOT);

        cookie.setSecure(secure);
        
        if (httpOnly) {
            /*
             * jetty specific workaround before Servlet-3.0 is supported in jetty.
             * see https://bugzilla.zimbra.com/show_bug.cgi?id=67078#c2
             * 
             * When we upgrade to jetty-8 and Servlet-3.0 is supporte in jettyd, 
             * change the following line to:
             * cookie.setHttpOnly(boolean isHttpOnly)
             */
            // jetty 7.6 https://bugs.eclipse.org/bugs/show_bug.cgi?id=364657
            cookie.setComment("__HTTP_ONLY__");
        }

        response.addCookie(cookie);
    }

    public static void clearCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setMaxAge(0);
        setAuthTokenCookieDomainPath(cookie, PATH_ROOT);
        response.addCookie(cookie);
    }

}
