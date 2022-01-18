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
package com.zimbra.common.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;

public class ZimbraCookie {

    public static final String COOKIE_ZM_AUTH_TOKEN       = "ZM_AUTH_TOKEN";
    public static final String COOKIE_ZM_ADMIN_AUTH_TOKEN = "ZM_ADMIN_AUTH_TOKEN";
    public static final String COOKIE_ZM_TRUST_TOKEN      = "ZM_TRUST_TOKEN";
    public static final String COOKIE_ZM_JWT              = "ZM_JWT";
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

        String cookieVal = LC.zimbra_same_site_cookie.value();
        if (!StringUtil.isNullOrEmpty(cookieVal)) {
            // setting cookie value like "SameSite=Strict;", value can be Strict, Lax, None
            path = new StringBuilder(path).append(";SameSite=").append(cookieVal).append(";").toString();
            cookie.setSecure(true);
        } else {
            cookie.setSecure(secure);
        }

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
