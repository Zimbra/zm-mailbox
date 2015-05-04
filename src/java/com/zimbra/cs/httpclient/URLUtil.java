/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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

import com.zimbra.common.consul.ConsulClient;
import com.zimbra.common.consul.ConsulServiceLocator;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.servicelocator.ChainedServiceLocator;
import com.zimbra.common.servicelocator.Selector;
import com.zimbra.common.servicelocator.ServiceLocator;
import com.zimbra.common.servicelocator.ZimbraServiceNames;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ProvisioningServiceLocator;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.MailMode;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.Zimbra;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class URLUtil {

    public static final String PROTO_HTTP  = "http";
    public static final String PROTO_HTTPS = "https";

    public static int DEFAULT_HTTP_PORT = 80;
    public static int DEFAULT_HTTPS_PORT = 443;

    /**
     * Return the URL where SOAP service is available for given store server.
     *
     * @see getMailURL()
     */
    public static String getSoapURL(Server server, boolean preferSSL) throws ServiceException {
        return URLUtil.getServiceURL(server, AccountConstants.USER_SERVICE_URI, preferSSL);
    }

    /** Perform a service locator lookup of a mailstore soap service */
    public static String getSoapURL(ServiceLocator serviceLocator, Selector<ServiceLocator.Entry> selector, boolean healthyOnly) throws ServiceException {
        try {
            ServiceLocator.Entry entry = serviceLocator.findOne(ZimbraServiceNames.MAILSTORE, selector, null, healthyOnly);
            String scheme = entry.tags.contains("ssl") ? "https" : "http";
            return scheme + "://" + entry.hostName + ":" + entry.servicePort + AccountConstants.USER_SERVICE_URI;
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed contacting service locator", e);
        }
    }

    /** Returns a mailstore user soap service url */
    @SuppressWarnings("unchecked")
    public static String getSoapURL() throws ServiceException {
        Server localServer = Provisioning.getInstance().getLocalServer();
        boolean useLocalServer = localServer.hasMailClientService() || !Zimbra.isAlwaysOn();
        if (useLocalServer) {
            return getSoapURL(localServer, true);
        }

        ServiceLocator serviceLocator = null;
        Selector<ServiceLocator.Entry> selector = null;
        try {
            serviceLocator = Zimbra.getAppContext().getBean(ServiceLocator.class);
            selector = Zimbra.getAppContext().getBean(Selector.class);
        } catch (Exception | NoClassDefFoundError e) {}

        if (serviceLocator == null) {
            ServiceLocator sl1 = new ConsulServiceLocator(new ConsulClient());
            ServiceLocator sl2 = new ProvisioningServiceLocator(Provisioning.getInstance());
            serviceLocator = new ChainedServiceLocator(sl1, sl2);
            selector = ServiceLocator.SELECT_RANDOM;
        }

        try {
            ServiceLocator.Entry entry = serviceLocator.findOne(ZimbraServiceNames.MAILSTORE, selector, null, false);
            String scheme = entry.tags.contains("ssl") ? "https" : "http";
            return scheme + "://" + entry.hostName + ":" + entry.servicePort + AccountConstants.USER_SERVICE_URI;
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed contacting service locator", e);
        }
    }

    public static String getSoapPublicURL(Server server, Domain domain, boolean preferSSL) throws ServiceException {
        return URLUtil.getPublicURLForDomain(server, domain, AccountConstants.USER_SERVICE_URI, preferSSL);
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
        String scheme;
        try {
            scheme = Provisioning.getInstance().getLocalServer().getAdminServiceScheme();
        } catch (ServiceException e) {
            ZimbraLog.soap.error("Error while getting admin service scheme", e);
            scheme = "https://";
        }
        sb.append(scheme).append(hostname).append(":").append(port).append(path);
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
        String scheme;
        try {
            scheme = Provisioning.getInstance().getLocalServer().getAdminServiceScheme();
        } catch (ServiceException e) {
            ZimbraLog.soap.error("Error while getting admin service scheme", e);
            scheme = "https://";
        }
        sb.append(scheme).append(hostname).append(":").append(port).append(path);
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
        String scheme;
        try {
            scheme = Provisioning.getInstance().getLocalServer().getAdminServiceScheme();
        } catch (ServiceException e) {
            ZimbraLog.soap.error("Error while getting admin service scheme", e);
            scheme = "https://";
        }
        sb.append(scheme).append(hostname).append(":").append(port).append(AdminConstants.ADMIN_SERVICE_URI);
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
        return getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI);
    }

    /**
     * Returns absolute URL with scheme, host, and port for admin app, using ServiceLocator.
     */
    @SuppressWarnings("unchecked")
    public static String getAdminURL(ServiceLocator serviceLocator, boolean healthyOnly) throws ServiceException {
        Selector<ServiceLocator.Entry> selector = null;
        try {
            selector = Zimbra.getAppContext().getBean(Selector.class);
        } catch (NoClassDefFoundError e) {
            selector = ServiceLocator.SELECT_RANDOM;
        }
        return getAdminURL(serviceLocator, selector, healthyOnly);
    }

    /**
     * Returns absolute URL with scheme, host, and port for admin app.
     */
    public static String getAdminURL(ServiceLocator serviceLocator, Selector<ServiceLocator.Entry> selector, boolean healthyOnly) throws ServiceException {
        try {
            ServiceLocator.Entry entry = serviceLocator.findOne(ZimbraServiceNames.MAILSTOREADMIN, selector, null, healthyOnly);
            String scheme = entry.tags.contains("ssl") ? "https" : "http";
            return scheme + "://" + entry.hostName + ":" + entry.servicePort + AdminConstants.ADMIN_SERVICE_URI;
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed contacting service locator", e);
        }
    }

    /**
     * Returns a mailstore admin soap service url.
     */
    @SuppressWarnings("unchecked")
    public static String getAdminURL() throws ServiceException {
        Server localServer = Provisioning.getInstance().getLocalServer();
        boolean useLocalServer = localServer.hasMailClientService() || !Zimbra.isAlwaysOn();
        if (useLocalServer) {
            return getAdminURL(localServer);
        }

        ServiceLocator serviceLocator = null;
        Selector<ServiceLocator.Entry> selector = null;
        try {
            serviceLocator = Zimbra.getAppContext().getBean(ServiceLocator.class);
            selector = Zimbra.getAppContext().getBean(Selector.class);
        } catch (Exception | NoClassDefFoundError e) {}

        if (serviceLocator == null) {
            ServiceLocator sl1 = new ConsulServiceLocator(new ConsulClient());
            ServiceLocator sl2 = new ProvisioningServiceLocator(Provisioning.getInstance());
            serviceLocator = new ChainedServiceLocator(sl1, sl2);
            selector = ServiceLocator.SELECT_RANDOM;
        }

        try {
            ServiceLocator.Entry entry = serviceLocator.findOne(ZimbraServiceNames.MAILSTOREADMIN, selector, null, false);
            String scheme = entry.tags.contains("ssl") ? "https" : "http";
            return scheme + "://" + entry.hostName + ":" + entry.servicePort + AdminConstants.ADMIN_SERVICE_URI;
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed contacting service locator", e);
        }
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

    public static String getPublicAdminConsoleURLForDomain(Server server, Domain domain) throws ServiceException {
        String publicAdminUrl = getAdminConsoleProxyUrl(server, domain);
        if (publicAdminUrl == null) {
            publicAdminUrl = URLUtil.getAdminURL(server, server.getAdminURL());
        }
        return publicAdminUrl;
    }

    private static String getAdminConsoleProxyUrl(Server server, Domain domain) throws ServiceException {
        if (domain == null) {
            return null;
        }
        String adminReference = domain.getWebClientAdminReference();
        if (adminReference != null) {
            return adminReference;
        }
        String hostname = domain.getAttr(Provisioning.A_zimbraPublicServiceHostname, null);
        if (hostname == null) {
            return null;
        }
        String proto = PROTO_HTTPS;

        String portString = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraAdminProxyPort, null);
        if (portString == null) {
            return null;
        }
        int port = 9071;
        try {
            port = Integer.parseInt(portString);
        } catch (NumberFormatException nfe) {
            throw ServiceException.FAILURE("unable to parse zimbraAdminProxyPort", nfe);
        }
        boolean printPort = port != DEFAULT_HTTPS_PORT;

        StringBuilder buf = new StringBuilder();
        buf.append(proto).append("://").append(hostname);
        if (printPort) {
            buf.append(":").append(port);
        }
        buf.append(server.getAdminURL());
        return buf.toString();

    }

    public static String getServiceURL(Server server, String path, boolean useSSL) throws ServiceException {

        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        if (hostname == null)
            throw ServiceException.INVALID_REQUEST("server " + server.getName() + " does not have " + Provisioning.A_zimbraServiceHostname, null);

    	String modeString = server.getAttr(Provisioning.A_zimbraMailMode, null);
    	if (modeString == null)
    		throw ServiceException.INVALID_REQUEST("server " + server.getName() + " does not have " + Provisioning.A_zimbraMailMode + " set, maybe it is not a store server?", null);
        MailMode mailMode = Provisioning.MailMode.fromString(modeString);

    	String proto;
    	int port;
    	if ((mailMode != MailMode.http && useSSL) || mailMode == MailMode.https) {
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
}
