/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on 2005. 4. 27.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.httpclient;

import java.io.IOException;
import java.util.Map;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class URLUtil {

    private static Log mLog = LogFactory.getLog(URLUtil.class);
    

    /**
     * Return the URL where SOAP service is available for given store server.
     * 
     * @see getMailURL()
     */
    public static String getSoapURL(Server server, boolean preferSSL) throws ServiceException {
        return URLUtil.getServiceURL(server, ZimbraServlet.USER_SERVICE_URI, preferSSL);
    }
    
    public static String getSoapPublicURL(Server server, Domain domain, boolean preferSSL) throws ServiceException {
        return URLUtil.getPublicURLForDomain(server, domain, ZimbraServlet.USER_SERVICE_URI, preferSSL);  
    }
    
    /**
     * Returns absolute URL with scheme, host, and port for admin app on server.
     * Admin app only runs over SSL.
     * @param server
     * @param path what follows port number; begins with slash
     * @return
     */
    public static String getAdminURL(Server server, String path) {
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        int port = server.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        StringBuffer sb = new StringBuffer(128);
        sb.append(LC.zimbra_admin_service_scheme.value()).append(hostname).append(":").append(port).append(path);
        return sb.toString();
    }
    
    /**
     * Returns absolute URL with scheme, host, and port for admin app on server.
     * Admin app only runs over SSL.
     * @param server
     * @param path what follows port number; begins with slash
     * @checkPort verify if the port is valid
     * @return
     */
    public static String getAdminURL(Server server, String path, boolean checkPort) throws ServiceException {
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        int port = server.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        if (checkPort && port <= 0)
            throw ServiceException.FAILURE("server " + server.getName() + " does not have admin port enabled", null);
        StringBuffer sb = new StringBuffer(128);
        sb.append(LC.zimbra_admin_service_scheme.value()).append(hostname).append(":").append(port).append(path);
        return sb.toString();
    }    


    /**
     * Returns absolute URL with scheme, host, and port for admin app on server.
     * Admin app only runs over SSL. Uses port from localconfig.
     * @param server hostname
     * @return
     */
    public static String getAdminURL(String hostname) {
        int port = (int) LC.zimbra_admin_service_port.longValue();
        StringBuffer sb = new StringBuffer(128);
        sb.append(LC.zimbra_admin_service_scheme.value()).append(hostname).append(":").append(port).append(ZimbraServlet.ADMIN_SERVICE_URI);
        return sb.toString();
    }
    
    /**
     * Returns absolute URL with scheme, host, and port for admin app on server.
     * Admin app only runs over SSL.
     * @param server
     * @param path what follows port number; begins with slash
     * @return
     */
    public static String getAdminURL(Server server) {
        return getAdminURL(server, ZimbraServlet.ADMIN_SERVICE_URI);
    }
    
    /**
     * Utility method to translate zimbraMtaAuthHost -> zimbraMtaAuthURL.
     * 
     * Not the best place for this method, but do not want to pollute
     * Provisioning with utility methods either.
     */
    public static String getMtaAuthURL(String authHost) throws ServiceException {
        for (Server server : Provisioning.getInstance().getAllServers()) {
            String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname, null);
            if (authHost.equalsIgnoreCase(serviceName)) {
                return URLUtil.getSoapURL(server, true);
            }
        }
        throw ServiceException.INVALID_REQUEST("specified " + Provisioning.A_zimbraMtaAuthHost + " does not correspond to a valid service hostname: " + authHost, null);
    }
    
    private static final String PROTO_HTTP  = "http";
    private static final String PROTO_HTTPS = "https";
    
    private static int DEFAULT_HTTP_PORT = 80;
    private static int DEFAULT_HTTPS_PORT = 443;
    
    /**
     * Returns absolute public URL with scheme, host, and port for mail app on server.
     * 
     * @param server
     * @param domain
     * @param path what follows port number; begins with slash
     * @param preferSSL if both SSL and and non-SSL are available, whether to prefer SSL 
     * @return desired URL
     */
    public static String getPublicURLForDomain(Server server, Domain domain, String path, boolean preferSSL) throws ServiceException {
        String publicURLForDomain = getPublicURLForDomain(domain, path);
        if (publicURLForDomain != null)
            return publicURLForDomain;
        
        // fallback to server setting if domain is not configured with public service hostname
        return URLUtil.getServiceURL(server, path, preferSSL);
    }
    
    private static String getPublicURLForDomain(Domain domain, String path) {
        if (domain == null)
            return null;
        
        String hostname = domain.getAttr(Provisioning.A_zimbraPublicServiceHostname, null);
        if (hostname == null)
            return null;
        
        String proto = domain.getAttr(Provisioning.A_zimbraPublicServiceProtocol, PROTO_HTTP);
        
        int defaultPort = PROTO_HTTP.equals(proto) ? DEFAULT_HTTP_PORT : DEFAULT_HTTPS_PORT;
        int port = domain.getIntAttr(Provisioning.A_zimbraPublicServicePort, defaultPort);
        
        boolean printPort = ((PROTO_HTTP.equals(proto) && port != DEFAULT_HTTP_PORT) ||
                             (PROTO_HTTPS.equals(proto) && port != DEFAULT_HTTPS_PORT));
        
        StringBuilder buf = new StringBuilder();
        buf.append(proto).append("://").append(hostname);
        if (printPort)
            buf.append(":").append(port);
        buf.append(path);
        return buf.toString();
    }
    
    public static String getServiceURL(Server server, String path, boolean useSSL) throws ServiceException {
        
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        if (hostname == null)
            throw ServiceException.INVALID_REQUEST("server " + server.getName() + " does not have " + Provisioning.A_zimbraServiceHostname, null);
        
    	String modeString = server.getAttr(Provisioning.A_zimbraMailMode, null);
    	if (modeString == null)
    		throw ServiceException.INVALID_REQUEST("server " + server.getName() + " does not have " + Provisioning.A_zimbraMailMode + " set, maybe it is not a store server?", null);
        
    	String proto;
    	int port;
    	if (modeString != Provisioning.MAIL_MODE.http.toString() && useSSL ||
    			modeString == Provisioning.MAIL_MODE.https.toString()) {
    	    proto = PROTO_HTTPS;
        	port = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, DEFAULT_HTTPS_PORT);
    	} else {
    	    proto = PROTO_HTTP;
        	port = server.getIntAttr(Provisioning.A_zimbraMailPort, DEFAULT_HTTP_PORT);
    	}

    	StringBuilder buf = new StringBuilder();
    	buf.append(proto).append("://").append(hostname);
        buf.append(":").append(port);
        buf.append(path);
    	return buf.toString();
    }
    
    public static boolean reverseProxiedMode(Server server) throws ServiceException {
        String referMode = server.getAttr(Provisioning.A_zimbraMailReferMode, "wronghost");
        return Provisioning.MAIL_REFER_MODE_REVERSE_PROXIED.equals(referMode);
    }

    private static final Map<Character,String> sUrlEscapeMap = new java.util.HashMap<Character,String>();
    
    static {
        sUrlEscapeMap.put(' ', "%20");
        sUrlEscapeMap.put('"', "%22");
        sUrlEscapeMap.put('\'', "%27");
        sUrlEscapeMap.put('{', "%7B");
        sUrlEscapeMap.put('}', "%7D");
        sUrlEscapeMap.put(';', "%3B");
        sUrlEscapeMap.put('?', "%3F");
        sUrlEscapeMap.put('!', "%21");
        sUrlEscapeMap.put(':', "%3A");
        sUrlEscapeMap.put('@', "%40");
        sUrlEscapeMap.put('#', "%23");
        sUrlEscapeMap.put('%', "%25");
        sUrlEscapeMap.put('&', "%26");
        sUrlEscapeMap.put('=', "%3D");
        sUrlEscapeMap.put('+', "%2B");
        sUrlEscapeMap.put('$', "%24");
        sUrlEscapeMap.put(',', "%2C");
    }
    
	public static String urlEscape(String str) {
		// rfc 2396 url escape.
		StringBuilder buf = null;
		for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            String escaped = null;
            if (c < 0x7F)
            	escaped = sUrlEscapeMap.get(c);
            
            if (escaped != null || c >= 0x7F) {
                if (buf == null) {
                    buf = new StringBuilder();
                    buf.append(str.substring(0, i));
                }
                if (escaped != null)
                	buf.append(escaped);
                else {
                	try {
                        byte[] raw = Character.valueOf(c).toString().getBytes("UTF-8");
                    	for (byte b : raw) {
                    		int unsignedB = b & 0xFF;  // byte is signed
                    		buf.append("%").append(Integer.toHexString(unsignedB).toUpperCase());
                    	}
                	} catch (IOException e) {
                		mLog.info("can't decode character "+c, e);
                		buf.append(c);
                	}
                }
            } else if (buf != null) {
                buf.append(c);
            }
		}
        if (buf != null)
            return buf.toString();
        return str;
	}
}
