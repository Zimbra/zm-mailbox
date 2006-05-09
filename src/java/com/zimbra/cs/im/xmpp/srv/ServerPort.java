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

import java.util.Iterator;
import java.util.ArrayList;

/**
 * Represents a port on which the server will listen for connections.
 * Used to aggregate information that the rest of the system needs
 * regarding the port while hiding implementation details.
 *
 * @author Iain Shigeoka
 */
public class ServerPort {

    private int port;
    private ArrayList<String> names;
    private String address;
    private boolean secure;
    private String algorithm;
    private Type type;

    public ServerPort(int port, String name, String address,
            boolean isSecure, String algorithm, Type type)
    {
        this.port = port;
        this.names = new ArrayList<String>(1);
        this.names.add(name);
        this.address = address;
        this.secure = isSecure;
        this.algorithm = algorithm;
        this.type = type;
    }

    /**
     * Returns the port number that is being used.
     *
     * @return the port number this server port is listening on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the logical domains for this server port. As multiple
     * domains may point to the same server, this helps to define what
     * the server considers "local".
     *
     * @return the server domain name(s) as Strings.
     */
    public Iterator getDomainNames() {
        return names.iterator();
    }

    /**
     * Returns the dot separated IP address for the server.
     *
     * @return The dot separated IP address for the server
     */
    public String getIPAddress() {
        return address;
    }

    /**
     * Determines if the connection is secure.
     *
     * @return True if the connection is secure
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Returns the basic protocol/algorithm being used to secure
     * the port connections. An example would be "SSL" or "TLS".
     *
     * @return The protocol used or null if this is not a secure server port
     */
    public String getSecurityType() {
        return algorithm;
    }

    /**
     * Returns true if other servers can connect to this port for s2s communication.
     *
     * @return true if other servers can connect to this port for s2s communication.
     */
    public boolean isServerPort() {
        return type == Type.server;
    }

    /**
     * Returns true if clients can connect to this port.
     *
     * @return true if clients can connect to this port.
     */
    public boolean isClientPort() {
        return type == Type.client;
    }

    /**
     * Returns true if external components can connect to this port.
     *
     * @return true if external components can connect to this port.
     */
    public boolean isComponentPort() {
        return type == Type.component;
    }

    public static enum Type {
        client,

        server,

        component;
    }
}
