/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import javax.servlet.http.Cookie;

import com.zimbra.common.localconfig.LC;

public class ZimbraCookie {
    
    public static String PATH_ROOT = "/";

    
    /**
     * set cookie domain and path for the cookie going back to the browser
     * 
     * @param cookie
     * @param path
     */
    public static void setAuthTokenCookieDomainPath(Cookie cookie, String path) {
        if (LC.zimbra_authtoken_cookie_domain.value().length() > 0)
            cookie.setDomain(LC.zimbra_authtoken_cookie_domain.value());
        
        cookie.setPath(path);
    }
}
