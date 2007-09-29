/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.net.InetAddress;

import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.net.VirtualConnection;
import org.xmpp.packet.Packet;

public class FakeClientConnection extends VirtualConnection {
    
    IMAddr mAddr;
    
    FakeClientConnection(IMPersona persona) {
        mAddr = persona.getAddr();
    }

    public void closeVirtualConnection() {
    }

    public void deliver(Packet packet) throws UnauthorizedException {
        IMXmppEvent imXmppEvent = new IMXmppEvent(mAddr, packet);
        IMRouter.getInstance().postEvent(imXmppEvent);
    }

    public void deliverRawText(String text) {
        IMXmppTextEvent imXmppEvent = new IMXmppTextEvent(mAddr, text);
        IMRouter.getInstance().postEvent(imXmppEvent);
    }

    public InetAddress getInetAddress() {
        return null;
    }

    public void systemShutdown() {
    }
}
