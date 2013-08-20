/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
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
            // jetty actually turns maxAge(lifetime in seconds) into an
            // Expires directive, not the Max-Age directive.
            cookie.setMaxAge(maxAge.intValue());
        }
        ZimbraCookie.setAuthTokenCookieDomainPath(cookie, ZimbraCookie.PATH_ROOT);

        cookie.setSecure(secure);

        if (httpOnly) {
            cookie.setHttpOnly(httpOnly);
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
