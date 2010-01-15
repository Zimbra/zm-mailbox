/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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

    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.net.VirtualConnection#closeVirtualConnection()
     */
    public void closeVirtualConnection() {
    }

    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.Connection#deliver(org.xmpp.packet.Packet)
     */
    public void deliver(Packet packet) throws UnauthorizedException {
        IMXmppEvent imXmppEvent = new IMXmppEvent(mAddr, packet);
        IMRouter.getInstance().postEvent(imXmppEvent);
    }

    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.Connection#deliverRawText(java.lang.String)
     */
    public void deliverRawText(String text) {
        // ignored for now
    }

    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.Connection#getInetAddress()
     */
    public InetAddress getInetAddress() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.jivesoftware.wildfire.Connection#systemShutdown()
     */
    public void systemShutdown() {
    }
}
