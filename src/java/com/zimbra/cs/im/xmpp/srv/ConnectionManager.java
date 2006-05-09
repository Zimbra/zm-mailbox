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
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv;

import java.net.Socket;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Coordinates connections (accept, read, termination) on the server.
 *
 * @author Iain Shigeoka
 */
public interface ConnectionManager {

    /**
     * Returns an array of the ports managed by this connection manager.
     *
     * @return an iterator of the ports managed by this connection manager
     *      (can be an empty but never null).
     */
    public Iterator<ServerPort> getPorts();

    /**
     * Adds a socket to be managed by the connection manager.
     *
     * @param socket the socket to add to this manager for management.
     * @param isSecure true if the connection is secure.
     * @param serverPort holds information about the port on which the server is listening for
     *        connections.
     */
    public void addSocket(Socket socket, boolean isSecure, ServerPort serverPort)
            throws XmlPullParserException;

    /**
     * Sets if the port listener for unsecured clients will be available or not. When disabled
     * there won't be a port listener active. Therefore, new clients won't be able to connect to
     * the server.
     *
     * @param enabled true if new unsecured clients will be able to connect to the server.
     */
    public void enableClientListener(boolean enabled);

    /**
     * Returns true if the port listener for unsecured clients is available. When disabled
     * there won't be a port listener active. Therefore, new clients won't be able to connect to
     * the server.
     *
     * @return true if the port listener for unsecured clients is available.
     */
    public boolean isClientListenerEnabled();

    /**
     * Sets if the port listener for secured clients will be available or not. When disabled
     * there won't be a port listener active. Therefore, new secured clients won't be able to
     * connect to the server.
     *
     * @param enabled true if new secured clients will be able to connect to the server.
     */
    public void enableClientSSLListener(boolean enabled);

    /**
     * Returns true if the port listener for secured clients is available. When disabled
     * there won't be a port listener active. Therefore, new secured clients won't be able to
     * connect to the server.
     *
     * @return true if the port listener for unsecured clients is available.
     */
    public boolean isClientSSLListenerEnabled();

    /**
     * Sets if the port listener for external components will be available or not. When disabled
     * there won't be a port listener active. Therefore, new external components won't be able to
     * connect to the server.
     *
     * @param enabled true if new external components will be able to connect to the server.
     */
    public void enableComponentListener(boolean enabled);

    /**
     * Returns true if the port listener for external components is available. When disabled
     * there won't be a port listener active. Therefore, new external components won't be able to
     * connect to the server.
     *
     * @return true if the port listener for external components is available.
     */
    public boolean isComponentListenerEnabled();

    /**
     * Sets if the port listener for remote servers will be available or not. When disabled
     * there won't be a port listener active. Therefore, new remote servers won't be able to
     * connect to the server.
     *
     * @param enabled true if new remote servers will be able to connect to the server.
     */
    public void enableServerListener(boolean enabled);

    /**
     * Returns true if the port listener for remote servers is available. When disabled
     * there won't be a port listener active. Therefore, new remote servers won't be able to
     * connect to the server.
     *
     * @return true if the port listener for remote servers is available.
     */
    public boolean isServerListenerEnabled();

    /**
     * Sets the port to use for unsecured clients. Default port: 5222.
     *
     * @param port the port to use for unsecured clients.
     */
    public void setClientListenerPort(int port);

    /**
     * Returns the port to use for unsecured clients. Default port: 5222.
     *
     * @return the port to use for unsecured clients.
     */
    public int getClientListenerPort();

    /**
     * Sets the port to use for secured clients. Default port: 5223.
     *
     * @param port the port to use for secured clients.
     */
    public void setClientSSLListenerPort(int port);

    /**
     * Returns the port to use for secured clients. Default port: 5223.
     *
     * @return the port to use for secured clients.
     */
    public int getClientSSLListenerPort();

    /**
     * Sets the port to use for external components.
     *
     * @param port the port to use for external components.
     */
    public void setComponentListenerPort(int port);

    /**
     * Returns the port to use for external components.
     *
     * @return the port to use for external components.
     */
    public int getComponentListenerPort();

    /**
     * Sets the port to use for remote servers. This port is used for remote servers to connect
     * to this server. Default port: 5269.
     *
     * @param port the port to use for remote servers.
     */
    public void setServerListenerPort(int port);

    /**
     * Returns the port to use for remote servers. This port is used for remote servers to connect
     * to this server. Default port: 5269.
     *
     * @return the port to use for remote servers.
     */
    public int getServerListenerPort();
}
