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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.spi;

import com.zimbra.cs.im.xmpp.srv.*;
import com.zimbra.cs.im.xmpp.srv.auth.UnauthorizedException;
import com.zimbra.cs.im.xmpp.srv.container.BasicModule;
import com.zimbra.cs.im.xmpp.srv.net.SocketPacketWriteHandler;
import org.xmpp.packet.Packet;

/**
 * In-memory implementation of the packet deliverer service
 *
 * @author Iain Shigeoka
 */
public class PacketDelivererImpl extends BasicModule implements PacketDeliverer {

    /**
     * The handler that does the actual delivery (could be a channel instead)
     */
    protected SocketPacketWriteHandler deliverHandler;

    private OfflineMessageStrategy messageStrategy;
    private SessionManager sessionManager;

    public PacketDelivererImpl() {
        super("Packet Delivery");
    }

    public void deliver(Packet packet) throws UnauthorizedException, PacketException {
        if (packet == null) {
            throw new PacketException("Packet was null");
        }
        if (deliverHandler == null) {
            throw new PacketException("Could not send packet - no route" + packet.toString());
        }
        // Let the SocketPacketWriteHandler process the packet. SocketPacketWriteHandler may send
        // it over the socket or store it when user is offline or drop it.
        deliverHandler.process(packet);
    }

    public void initialize(XMPPServer server) {
        super.initialize(server);
        messageStrategy = server.getOfflineMessageStrategy();
        sessionManager = server.getSessionManager();
    }

    public void start() throws IllegalStateException {
        super.start();
        deliverHandler =
                new SocketPacketWriteHandler(sessionManager,
                        XMPPServer.getInstance().getRoutingTable(), messageStrategy);
    }

    public void stop() {
        super.stop();
        deliverHandler = null;
    }
}
