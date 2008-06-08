package com.zimbra.common.auth;

import javax.servlet.http.Cookie;

import com.zimbra.common.localconfig.LC;

public class AuthTokenCookie {
    
    public static String PATH_ROOT = "/";

    
    /**
     * set cookie domain and path for the cookie going back to the browser
     * 
     * @param cookie
     * @param path
     */
    public static void setCookieDomainPath(Cookie cookie, String path) {
        if (LC.zimbra_authtoken_cookie_domain.value().length() > 0)
            cookie.setDomain(LC.zimbra_authtoken_cookie_domain.value());
        
        cookie.setPath(path);
    }
}
