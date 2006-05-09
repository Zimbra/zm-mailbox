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

package com.zimbra.cs.im.xmpp.srv.net;

import org.dom4j.Element;
import com.zimbra.cs.im.xmpp.util.Log;
import com.zimbra.cs.im.xmpp.srv.PacketRouter;
import com.zimbra.cs.im.xmpp.srv.auth.UnauthorizedException;
import com.zimbra.cs.im.xmpp.srv.component.ComponentSession;
import com.zimbra.cs.im.xmpp.srv.component.InternalComponentManager;
import org.xmlpull.v1.XmlPullParserException;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.PacketError;

import java.io.IOException;
import java.net.Socket;

/**
 * A SocketReader specialized for component connections. This reader will be used when the open
 * stream contains a jabber:component:accept namespace.
 *
 * @author Gaston Dombiak
 */
public class ComponentSocketReader extends SocketReader {

    public ComponentSocketReader(PacketRouter router, String serverName, Socket socket,
            SocketConnection connection) {
        super(router, serverName, socket, connection);
    }

    /**
     * Only <tt>bind<tt> packets will be processed by this class to bind more domains
     * to existing external components. Any other type of packet is unknown and thus
     * rejected generating the connection to be closed.
     *
     * @param doc the unknown DOM element that was received
     * @return false if packet is unknown otherwise true.
     */
    protected boolean processUnknowPacket(Element doc) {
        // Handle subsequent bind packets
        if ("bind".equals(doc.getName())) {
            ComponentSession componentSession = (ComponentSession) session;
            // Get the external component of this session
            ComponentSession.ExternalComponent component = componentSession.getExternalComponent();
            String initialDomain = component.getInitialSubdomain();
            String extraDomain = doc.attributeValue("name");
            if (extraDomain == null || "".equals(extraDomain)) {
                // No new bind domain was specified so return a bad_request error
                Element reply = doc.createCopy();
                reply.add(new PacketError(PacketError.Condition.bad_request).getElement());
                connection.deliverRawText(reply.asXML());
            }
            else if (extraDomain.equals(initialDomain)) {
                // Component is binding initial domain that is already registered
                // Send confirmation that the new domain has been registered
                connection.deliverRawText("<bind/>");
            }
            else if (extraDomain.endsWith(initialDomain)) {
                // Only accept subdomains under the initial registered domain
                if (component.getSubdomains().contains(extraDomain)) {
                    // Domain already in use so return a conflict error
                    Element reply = doc.createCopy();
                    reply.add(new PacketError(PacketError.Condition.conflict).getElement());
                    connection.deliverRawText(reply.asXML());
                }
                else {
                    try {
                        // Get the requested subdomain
                        String subdomain = extraDomain;
                        int index = extraDomain.indexOf(serverName);
                        if (index > -1) {
                            subdomain = extraDomain.substring(0, index -1);
                        }
                        InternalComponentManager.getInstance().addComponent(subdomain, component);
                        // Send confirmation that the new domain has been registered
                        connection.deliverRawText("<bind/>");
                    }
                    catch (ComponentException e) {
                        Log.error("Error binding extra domain: " + extraDomain + " to component: " +
                                component, e);
                        // Return internal server error
                        Element reply = doc.createCopy();
                        reply.add(new PacketError(
                                PacketError.Condition.internal_server_error).getElement());
                        connection.deliverRawText(reply.asXML());
                    }
                }
            }
            else {
                // Return forbidden error since we only allow subdomains of the intial domain
                // to be used by the same external component
                Element reply = doc.createCopy();
                reply.add(new PacketError(PacketError.Condition.forbidden).getElement());
                connection.deliverRawText(reply.asXML());
            }
            return true;
        }
        // This is an unknown packet so return false (and close the connection)
        return false;
    }

    boolean createSession(String namespace) throws UnauthorizedException, XmlPullParserException,
            IOException {
        if ("jabber:component:accept".equals(namespace)) {
            // The connected client is a component so create a ComponentSession
            session = ComponentSession.createSession(serverName, reader, connection);
            return true;
        }
        return false;
    }

    String getNamespace() {
        return "jabber:component:accept";
    }

    boolean validateHost() {
        return false;
    }
}
