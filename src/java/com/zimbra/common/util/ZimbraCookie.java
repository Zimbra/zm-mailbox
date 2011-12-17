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
    
    public static void addHttpOnlyCookie(HttpServletResponse response, String name, String value, 
            String path, Integer maxAge, boolean secure) {
        addCookie(response, name, value, path, maxAge, true, secure);
    }
    
    private static void addCookie(HttpServletResponse response, String name, String value, 
            String path, Integer maxAge, boolean httpOnly, boolean secure) {
        Cookie cookie;
        
        if (httpOnly) {
            // httpOnly code will be activated after bug 64052 is fixed
            // see jetty-7.5 code below
            cookie = new Cookie(name, value);
        } else {
            cookie = new Cookie(name, value);
        }

        if (maxAge != null) {
            cookie.setMaxAge(maxAge.intValue());
        }
        ZimbraCookie.setAuthTokenCookieDomainPath(cookie, ZimbraCookie.PATH_ROOT);

        cookie.setSecure(secure);
        // httpOnly code will be activated after bug 64052 is fixed
        // jetty 7.6 https://bugs.eclipse.org/bugs/show_bug.cgi?id=364657
        /*
        if (httpOnly) {
            cookie.setComment("__HTTP_ONLY__");
        }
        */
        response.addCookie(cookie);
    }
    
    // httpOnly code will be activated after bug 64052 is fixed
    /*
     * jetty 7.5
     *  
    org.eclipse.jetty.http.HttpCookie cookie = 
        new org.eclipse.jetty.http.HttpCookie(
            ck.getKey(),                             // name
            ck.getValue(),                           // value
            null,                                    // domain
            ZimbraCookie.PATH_ROOT,                  // path
            maxAge == null ? -1 : maxAge.intValue(), // maxAge
            true,                                    // httpOnly
            secure                                   // secure
            );
    
    if (response instanceof javax.servlet.ServletResponseWrapper) {
        javax.servlet.ServletResponse servletResp = ((javax.servlet.ServletResponseWrapper) response).getResponse();
        if (servletResp instanceof org.eclipse.jetty.server.Response) {
            org.eclipse.jetty.server.Response resp = (org.eclipse.jetty.server.Response) servletResp;
            resp.addCookie(authTokenCookie);
        }
    }
    */

}
