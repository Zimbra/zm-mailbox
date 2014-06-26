/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014 Zimbra, Inc.
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
 * Created on Oct 26, 2005
 *
 */
package com.zimbra.cs.extension;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;

/**
 * HTTP handler for an extension. HTTP GET and POST requests are dispatched to the handler
 * for processing. Each extension can define multiple handlers and register them under the
 * extension with different paths.
 * 
 * @author kchen
 *
 */
public abstract class ExtensionHttpHandler {

    protected ZimbraExtension mExtension;
    
    /**
     * The path under which the handler is registered for an extension.
     * @return
     */
    public String getPath() {
        return "/" + mExtension.getName();
    }
    
    /**
     * Processes HTTP OPTIONS requests.
     * @param req
     * @param resp
     * @throws IOException
     * @throws ServletException
     */
    public void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        throw new ServletException("HTTP OPTIONS requests are not supported");
    }
    
    /**
     * Processes HTTP GET requests.
     * @param req
     * @param resp
     * @throws IOException
     * @throws ServletException
     */
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        throw new ServletException("HTTP GET requests are not supported");
    }

    /**
     * Processes HTTP POST requests.
     * @param req
     * @param resp
     * @throws IOException
     * @throws ServletException
     */
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        throw new ServletException("HTTP POST requests are not supported");
    }

    /**
     * Called to initialize the handler. If initialization fails, the handler is not registered.
     * @param ext the extension to which this handler belongs
     * @throws ServiceException
     */
    public void init(ZimbraExtension ext) throws ServiceException {
        mExtension = ext;
    }
    
    /**
     * Called to terminate the handler.
     */
    public void destroy() {
        
    }

    /**
     * Hides the extension for requests sent to the default mail port and
     * mail SSL port.
     */
    public boolean hideFromDefaultPorts() {
    	return false;
    }
}
