/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2015, 2016 Synacor, Inc.
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

/*
 * Created on Oct 26, 2005
 *
 */
package com.zimbra.cs.extension;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.servlet.ZimbraServlet;

/**
 * Registers {@link ExtensionHttpHandler} for extensions and dispatches HTTP requests to them.
 * This servlet accepts requests sent to /service/extension/&lt;ext name>/&lt;handler path> URI,
 * where &lt;ext name>/&lt;handler path> is used to locate the handler whose {@link ExtensionHttpHandler#getPath() }
 * has the same path.
 *
 * @author kchen
 *
 */
public class ExtensionDispatcherServlet extends ZimbraServlet {
    private static Map sHandlers = Collections.synchronizedMap(new HashMap());
    public static final String EXTENSION_PATH = "/service/extension";

    /**
     * Registers an HTTP handler for the extension.
     * @param ext the extension to which the handler belongs
     * @param handler
     * @throws ServiceException
     */
    public static void register(ZimbraExtension ext, ExtensionHttpHandler handler) throws ServiceException {
        handler.init(ext);
        String name = handler.getPath();
        synchronized (sHandlers) {
            if (sHandlers.containsKey(name))
                throw ServiceException.FAILURE("HTTP handler already registered: " + name, null);
            sHandlers.put(name, handler);
            ZimbraLog.extensions.info("registered handler at " + name);
        }
    }

    /**
     * Gets the handler for handling HTTP requests for an extension.
     *
     * @param path path in request URI in the form of <ext name>/<handler path>/*
     * @return
     */
    public static ExtensionHttpHandler getHandler(String path) {
        ExtensionHttpHandler handler = null;
        int slash = -1;
        do {
            handler = (ExtensionHttpHandler) sHandlers.get(path);
            if (handler == null) {
                slash = path.lastIndexOf('/');
                if (slash != -1) {
                    path = path.substring(0, slash);
                }
            }
        } while (handler == null && slash > 0);
        return handler;
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        ZimbraLog.clearContext();
        ExtensionHttpHandler handler = null;
        try {
            handler = getHandler(req);
        } catch (ServiceException e) {
            ZimbraLog.extensions.warn("unable to find handler for extension: " + e.getMessage());
            if (ZimbraLog.extensions.isDebugEnabled()) {
                ZimbraLog.extensions.debug("unable to find handler for extension", e);
            }
        }
        if (handler == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
            return;
        }
        String method = req.getMethod();
        if ("OPTIONS".equals(method)) {
            handler.doOptions(req, resp);
        } else if ("GET".equals(method)) {
            handler.doGet(req, resp);
        } else if ("POST".equals(method)) {
            handler.doPost(req, resp);
        } else if ("PUT".equals(method)) {
            handler.doPut(req, resp);
        } else if ("DELETE".equals(method)) {
            handler.doDelete(req, resp);
        } else if ("PATCH".equals(method)) {
            handler.doPatch(req, resp);
        }
        else {
            throw new ServletException("request method " + method + " not supported");
        }
    }

    /**
     * Calls the destroy method for handlers belonging to the extension, and unregisters them.
     * @param ext
     */
    public static void unregister(ZimbraExtension ext) {
        synchronized(sHandlers) {
            for (Iterator it = sHandlers.keySet().iterator(); it.hasNext(); ) {
                String path = (String) it.next();
                if (path.startsWith("/" + ext.getName())) {
                    ExtensionHttpHandler handler = (ExtensionHttpHandler) sHandlers.get(path);
                    try {
                        handler.destroy();
                    } catch (Exception e) {
                        ZimbraLog.extensions.warn("Error in destroy for handler " + handler.getClass(), e);
                    } finally {
                        it.remove();
                    }
                }
            }
        }
    }

    private ExtensionHttpHandler getHandler(HttpServletRequest req) throws ServiceException {
        String uri = req.getRequestURI();
        int pos = uri.indexOf(EXTENSION_PATH);
        String extPath = uri.substring(pos + EXTENSION_PATH.length());
        if (extPath.length() == 0)
            throw ServiceException.INVALID_REQUEST("Invalid request: " + uri, null);
        ZimbraLog.extensions.debug("getting handler registered at " + extPath);
        ExtensionHttpHandler handler = getHandler(extPath);
        if (handler == null)
            throw ServiceException.FAILURE("Extension HTTP handler not found at " + extPath, null);
        if (handler.getAllowedPort() > 0 && handler.getAllowedPort() != req.getServerPort()) {
            throw ServiceException.FAILURE(String.format("extension '%s' not supported on %d port", handler.mExtension.getName(), req.getServerPort()), null);
        }
        if (handler.hideFromDefaultPorts()) {
        	Server server = Provisioning.getInstance().getLocalServer();
        	int port = req.getLocalPort();
        	int mailPort = server.getIntAttr(Provisioning.A_zimbraMailPort, 0);
        	int mailSslPort = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, 0);
        	int adminPort = server.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        	if (port == mailPort || port == mailSslPort || port == adminPort)
        		throw ServiceException.FAILURE("extension not supported on this port", null);
        }
        return handler;
    }
}
