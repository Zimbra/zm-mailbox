/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * Part of the Zimbra Collaboration Suite Server.
 *
 * The Original Code is Copyright (C) Jive Software. Used with permission
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * An XMPPContextListener starts an XMPPServer when a ServletContext is initialized and stops
 * the xmpp server when the servlet context is destroyed.
 *
 * @author evrim ulu
 * @author Gaston Dombiak
 */
public class XMPPContextListener implements ServletContextListener {

    protected String XMPP_KEY = "XMPP_SERVER";

    public void contextInitialized(ServletContextEvent event) {
        if (XMPPServer.getInstance() != null) {
            // Running in standalone mode so do nothing
            return;
        }
        XMPPServer server = new XMPPServer();
        event.getServletContext().setAttribute(XMPP_KEY, server);
    }

    public void contextDestroyed(ServletContextEvent event) {
        XMPPServer server = (XMPPServer) event.getServletContext().getAttribute(XMPP_KEY);
        if (null != server) {
            server.stop();
        }
    }

}
