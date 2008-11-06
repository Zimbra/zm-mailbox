/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import org.jivesoftware.wildfire.Session;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.zimbra.common.util.ZimbraLog;

public class PacketInterceptor implements org.jivesoftware.wildfire.interceptor.PacketInterceptor {

    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) /* throws PacketRejectedException */ {
        if (processed && packet instanceof Message) {
            if (session.getAddress().getNode() != null) {
                ZimbraLog.im_intercept.debug("Session "+ session.toString() +" Intercepting " + (incoming ? "INCOMING " : "OUTGOING ") + (processed ? "PROCESSED " : "NOT PROCESSED ") +" packet: "+packet.toString());
                String addr = session.getAddress().toBareJID().toString();
                IMXmppEvent imXmppEvent = new IMXmppEvent(new IMAddr(addr), packet, true);
                IMRouter.getInstance().postEvent(imXmppEvent);
            } 
        }
    }
}
