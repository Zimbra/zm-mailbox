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

import java.util.Iterator;
import java.util.List;
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
        return URLUtil.getServiceUrl(server, null, ZimbraServlet.USER_SERVICE_URI, preferSSL, true);
    }
    
    public static String getSoapURL(Server server, Domain domain, boolean preferSSL) throws ServiceException {
        return URLUtil.getServiceUrl(server, domain, ZimbraServlet.USER_SERVICE_URI, preferSSL, true);  
    }
    
    /**
     * Returns absolute URL with scheme, host, and port for mail app on server.
     * 
     * @param server
     * @param path what follows port number; begins with slash
     * @param preferSSL if both SSL and and non-SSL are available, whether to prefer SSL 
     * @return desired URL
     */
    public static String getMailURL(Server server, String path, boolean preferSSL) throws ServiceException {
        return URLUtil.getServiceUrl(server, null, path, preferSSL, true);
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
        List servers = Provisioning.getInstance().getAllServers();
        for (Iterator it = servers.iterator(); it.hasNext();) {
            Server server = (Server) it.next();
            String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname, null);
            if (authHost.equalsIgnoreCase(serviceName)) {
                return URLUtil.getServiceUrl(server, null, ZimbraServlet.USER_SERVICE_URI, true, false);
            }
        }
        throw ServiceException.INVALID_REQUEST("specified " + Provisioning.A_zimbraMtaAuthHost + " does not correspond to a valid service hostname: " + authHost, null);
    }
    
    private static final String SCHEME_HTTP  = "http://";
    private static final String SCHEME_HTTPS = "https://";
    
    private static int DEFAULT_HTTP_PORT = 80;
    private static int DEFAULT_HTTPS_PORT = 443;
    
    /**
     * Returns absolute URL with scheme, host, and port for mail app on server.
     * 
     * @param server
     * @param path what follows port number; begins with slash
     * @param preferSSL if both SSL and and non-SSL are available, whether to prefer SSL 
     * @param checkReverseProxiedMode whether to take into account if the server is running in reverse proxied mode
     * @return desired URL
     */
    public static String getServiceUrl(Server server, Domain domain, String path, boolean preferSSL, boolean checkReverseProxiedMode) throws ServiceException {
        String publicServiceHostname = domain == null ? null : domain.getAttr(Provisioning.A_zimbraPublicServiceHostname, null);
        
        String hostname;
        if (publicServiceHostname == null)
            hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        else
            hostname = publicServiceHostname;
        
        if (hostname == null)
            throw ServiceException.INVALID_REQUEST("server " + server.getName() + " does not have " + Provisioning.A_zimbraServiceHostname, null);
        
        /*
         * We now have a hostname.  
         * if server is running in reverse proxied mode and we need to generate the URL to point to the reverse proxy, 
         * 1. domain.zimbraPublicServiceHostname is set to the hostname on which the reverse proxy is running, and 
         *      - a server can be found by zimbraPublicServiceHostname: this is the ideal case, use it
         *      - a server cannot be found by zimbraPublicServiceHostname: no good, throw ServiceException
         *         
         * 2. domain is null(really an error, but we've been handling it so keep the current code behavior) or 
         *    domain.zimbraPublicServiceHostname is not set.  
         *    This is OK, assuming the reverse proxy is running on the same server.  
         *    We do the same check (as we would do for 1) if the reverse proxy is indeed running on the configured server
         */
        Server publicServiceServer = null;
        boolean reverseProxiedMode = false;
        if (checkReverseProxiedMode && reverseProxiedMode(server)) {
            reverseProxiedMode = true;
            if (publicServiceHostname != null) {
                publicServiceServer = Provisioning.getInstance().get(Provisioning.ServerBy.serviceHostname, publicServiceHostname);
                if (publicServiceServer == null)
                    throw ServiceException.INVALID_REQUEST("server " + publicServiceHostname + " not found", null);
            } else
                publicServiceServer = server;
            
            // check if the reverse proxy is enabled, should we?
            if (!publicServiceServer.getBooleanAttr(Provisioning.A_zimbraReverseProxyHttpEnabled, false))
                throw ServiceException.INVALID_REQUEST("server " + server.getName() + " is running in reverse proxied mode " + 
                                                       "but reverse proxy is not enabled on server " + publicServiceServer.getName() +
                                                       ", either domain " + Provisioning.A_zimbraPublicServiceHostname + " is not set " + 
                                                       "or is set to a server on which reverse proxy is not enabled", 
                                                       null);
        }
        
        String modeString = server.getAttr(Provisioning.A_zimbraMailMode, null);
        if (modeString == null) {
            throw ServiceException.INVALID_REQUEST("server " + server.getName() + " does not have " + Provisioning.A_zimbraMailMode + " set, maybe it is not a store server?", null);
        }
        
        Provisioning.MAIL_MODE mode;
        try {
            mode = Provisioning.MAIL_MODE.valueOf(modeString);
        } catch (IllegalArgumentException iae) {
            throw ServiceException.INVALID_REQUEST("server " + server.getName() + " has invalid " + Provisioning.A_zimbraMailMode + ": " + modeString, iae);
        }
        
        boolean ssl;
        boolean printPort = true;
        
        switch (mode) {
        case both:
        case mixed:
        case redirect:
            ssl = preferSSL;
            break;
        case https:
            ssl = true;
            break;
        case http:
            ssl = false;
            break;
        default:
            throw ServiceException.INVALID_REQUEST("server " + server.getName() + " has unknown " + Provisioning.A_zimbraMailMode + ": " + mode, null);
        }
        
        String scheme;
        String portAttr;
        int port = 0;
        Server targetServer = (publicServiceServer == null) ? server : publicServiceServer;

        if (ssl) {
            scheme = SCHEME_HTTPS;
            if (reverseProxiedMode)
                portAttr = Provisioning.A_zimbraMailSSLProxyPort;
            else
                portAttr = Provisioning.A_zimbraMailSSLPort;
            port = targetServer.getIntAttr(portAttr, 0);
            if (port < 1) {
                throw ServiceException.INVALID_REQUEST("server " + targetServer.getName() + " has invalid " + portAttr + ": " + port, null);
            }
            if (port == DEFAULT_HTTPS_PORT)
            	printPort = false;
        } else {
            scheme = SCHEME_HTTP;
            if (reverseProxiedMode)
                portAttr = Provisioning.A_zimbraMailProxyPort;
            else
                portAttr = Provisioning.A_zimbraMailPort;
            port = targetServer.getIntAttr(portAttr, 0);
            if (port < 1) {
                throw ServiceException.INVALID_REQUEST("server " + targetServer.getName() + " has invalid " + portAttr + ": " + port, null);
            }
            if (port == DEFAULT_HTTP_PORT)
            	printPort = false;
        }

        StringBuilder sb = new StringBuilder(128);
        sb.append(scheme).append(hostname);
        if (printPort)
        	sb.append(":").append(port);
        sb.append(path);
        return sb.toString();
    }
    
    public static String getProxyURL(Server server, String path, boolean useSSL) throws ServiceException {
    	String modeString = server.getAttr(Provisioning.A_zimbraMailMode, null);
    	if (modeString == null)
    		throw ServiceException.INVALID_REQUEST("server " + server.getName() + " does not have " + Provisioning.A_zimbraMailMode + " set, maybe it is not a store server?", null);
        
    	StringBuilder buf = new StringBuilder();
    	int port;
    	if (modeString != Provisioning.MAIL_MODE.http.toString() && useSSL ||
    			modeString == Provisioning.MAIL_MODE.https.toString()) {
            buf.append("https://");
        	port = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, 443);
    	} else {
        	buf.append("http://");
        	port = server.getIntAttr(Provisioning.A_zimbraMailPort, 80);
    	}

        buf.append(server.getAttr(Provisioning.A_zimbraServiceHostname));
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
            else
            	escaped = "%" + Integer.toHexString((int)c).toUpperCase();
            if (escaped != null) {
                if (buf == null) {
                    buf = new StringBuilder();
                    buf.append(str.substring(0, i));
                }
                buf.append(escaped);
            } else if (buf != null) {
                buf.append(c);
            }
		}
        if (buf != null)
            return buf.toString();
        return str;
	}
}
