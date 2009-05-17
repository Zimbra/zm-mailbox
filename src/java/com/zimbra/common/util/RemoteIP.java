package com.zimbra.common.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.localconfig.LC;

public class RemoteIP {
    
    public static final String X_ORIGINATING_IP_HEADER = LC.zimbra_http_originating_ip_header.value();

    
    /**
     * IP of the http client,
     * Should be always present.
     */
    private String mClientIP;
    
    /**
     * IP of the originating client.  
     * It can be null.
     */
    private String mOrigIP;
    
    /** 
     *  It can be the IP of the http client, or in the presence of a 
     *  real origin IP address http header(header specified in the LC 
     *  key zimbra_http_originating_ip_header) the IP of the real 
     *  origin client if the http client is in a trusted network.
     *  
     *  Should be always present.
     */
    private String mRequestIP;
    
    public RemoteIP(HttpServletRequest req, TrustedIPs trustedIPs) {
        mClientIP = req.getRemoteAddr();
        
        String origIp = null;
        if (trustedIPs.isIpTrusted(mClientIP)) {
            mOrigIP = req.getHeader(X_ORIGINATING_IP_HEADER);
        }
        
        if (mOrigIP != null)
            mRequestIP = mOrigIP;
        else
            mRequestIP = mClientIP;
    }
    
    public String getClientIP() { return mClientIP; }

    public String getOrigIP() { return mOrigIP; }
    
    public String getRequestIP() { return mRequestIP; }
    
    public void addToLoggingContext() {
        if (mOrigIP != null)
            ZimbraLog.addOrigIpToContext(mOrigIP);
        
        // don't log ip if oip is present and ip is localhost
        if (!TrustedIPs.isLocalhost(mClientIP) || mOrigIP == null)
            ZimbraLog.addIpToContext(mClientIP);
    }
    
    /*
    private static class TrustedNetwork {
        
        
        // returns if an ip is in trusted network
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
    */
    
    public static class TrustedIPs {
        private static final String IP_LOCALHOST = "127.0.0.1"; 
        
        private Set<String> mTrustedIPs = new HashSet<String>();
        
        public TrustedIPs(String[] ips) {
            if (ips != null) {
                for (String ip : ips) {
                    if (!StringUtil.isNullOrEmpty(ip))
                        mTrustedIPs.add(ip);
                }
            }
        }
        
        public boolean isIpTrusted(String ip) {
            return isLocalhost(ip) || mTrustedIPs.contains(ip);
        }

        private static boolean isLocalhost(String ip) {
            return IP_LOCALHOST.equals(ip);
        }
    }

}
