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
package com.zimbra.cs.im;

import org.jivesoftware.wildfire.Session;
import org.xmpp.packet.Packet;

public class PacketInterceptor implements org.jivesoftware.wildfire.interceptor.PacketInterceptor {

    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) /* throws PacketRejectedException */ {
//        ZimbraLog.im.info("Session\n"+ session.toString() +"\nIntercepting an " + (incoming ? "INCOMING " : "OUTGOING ") + (processed ? "PROCESSED " : "NOT PROCESSED ") +" packet:\n"+packet.toString()+"\n");
        if (processed) {
            if (session.getAddress().getNode() != null) {
                String addr = session.getAddress().toBareJID().toString();
                IMXmppEvent imXmppEvent = new IMXmppEvent(new IMAddr(addr), packet);
                IMRouter.getInstance().postEvent(imXmppEvent);
            } 
        }
    }
}
