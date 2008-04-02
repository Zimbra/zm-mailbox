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

    private static final String SCHEME_HTTP  = "http://";
    private static final String SCHEME_HTTPS = "https://";
    
    private static int DEFAULT_HTTP_PORT = 80;
    private static int DEFAULT_HTTPS_PORT = 443;
    

    /**
     * Return the URL where SOAP service is available for given store server.
     * 
     * @see getMailURL()
     */
    public static String getSoapURL(Server server, boolean preferSSL) throws ServiceException {
        return getMailURL(server, ZimbraServlet.USER_SERVICE_URI, preferSSL);  
    }
    
    public static String getSoapURL(Server server, Domain domain, boolean preferSSL, boolean checkReferMode) throws ServiceException {
        return getMailURL(server, domain, ZimbraServlet.USER_SERVICE_URI, preferSSL, checkReferMode);  
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
        return getMailURL(server, null, path, preferSSL, false);
    }
    
    /**
     * Returns absolute URL with scheme, host, and port for mail app on server.
     * 
     * @param server
     * @param path what follows port number; begins with slash
     * @param preferSSL if both SSL and and non-SSL are available, whether to prefer SSL 
     * @return desired URL
     */
    private static String getMailURL(Server server, Domain domain, String path, boolean preferSSL, boolean checkReferMode) throws ServiceException {
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
         * 2. domain.zimbraPublicServiceHostname is not set.  This is OK, assuming the reverse proxy is running on the 
         *    same server 
         */
        Server publicServiceServer = null;
        boolean reverseProxyMode = false;
        if (checkReferMode && reverseProxyMode(server)) {
            reverseProxyMode = true;
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
                                                       ", either domain's " + Provisioning.A_zimbraPublicServiceHostname + " is not set " + 
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
            if (reverseProxyMode)
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
            if (reverseProxyMode)
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

        StringBuffer sb = new StringBuffer(128);
        sb.append(scheme).append(hostname);
        if (printPort)
        	sb.append(":").append(port);
        sb.append(path);
        return sb.toString();
    }
    
    public static boolean reverseProxyMode(Server server) throws ServiceException {
        String referMode = server.getAttr(Provisioning.A_zimbraMailReferMode, "wronghost");
        return Provisioning.MAIL_REFER_MODE_REVERSE_PROXIED.equals(referMode);
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
                return URLUtil.getSoapURL(server, true);
            }
        }
        throw ServiceException.INVALID_REQUEST("specified " + Provisioning.A_zimbraMtaAuthHost + " does not correspond to a valid service hostname: " + authHost, null);
    }

}
