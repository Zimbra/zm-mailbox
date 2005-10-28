/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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

import com.zimbra.cs.service.ServiceException;

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

}
