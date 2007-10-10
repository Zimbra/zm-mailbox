package com.zimbra.common.util;

import javax.servlet.http.HttpServletRequest;


public class TrustedNetwork {
    
    private static final String IP_LOCALHOST = "127.0.0.1"; 
    
    /*
     * returns if an ip is in trusted network
     */
    public static boolean isIpTrusted(String ip) {
        if (StringUtil.isNullOrEmpty(ip))
            return false;
        
        // For now the only trusted ip is localhost
        return isLocalhost(ip);
    }

    public static boolean isLocalhost(String ip) {
        return IP_LOCALHOST.equals(ip);
    }

}
