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

package com.zimbra.cs.im.xmpp.srv.transport;

import com.zimbra.cs.im.xmpp.srv.container.BasicModule;
import com.zimbra.cs.im.xmpp.util.LocaleUtils;
import com.zimbra.cs.im.xmpp.util.Log;
import com.zimbra.cs.im.xmpp.srv.*;
import com.zimbra.cs.im.xmpp.srv.auth.UnauthorizedException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes packets to the appropriate transport gateway or drops the packet.
 *
 * @author Iain Shigeoka
 */
public class TransportHandler extends BasicModule implements ChannelHandler {

    private Map<String, Channel> transports = new ConcurrentHashMap<String, Channel>();

    private PacketDeliverer deliverer;

    public TransportHandler() {
        super("Transport handler");
    }

    public void addTransport(Channel transport) {
        transports.put(transport.getName(), transport);
    }

    public void process(Packet packet) throws UnauthorizedException, PacketException {
        boolean handled = false;
        String host = packet.getTo().getDomain();
        for (Channel channel : transports.values()) {
            if (channel.getName().equalsIgnoreCase(host)) {
                channel.add(packet);
                handled = true;
            }
        }
        if (!handled) {
            JID recipient = packet.getTo();
            JID sender = packet.getFrom();
            packet.setError(PacketError.Condition.remote_server_timeout);
            packet.setFrom(recipient);
            packet.setTo(sender);
            try {
                deliverer.deliver(packet);
            }
            catch (PacketException e) {
                Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
            }
        }
    }

    public void initialize(XMPPServer server) {
        super.initialize(server);
        deliverer = server.getPacketDeliverer();
    }
}