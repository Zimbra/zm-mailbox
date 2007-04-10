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

import org.xmpp.packet.Packet;

import com.zimbra.common.service.ServiceException;

public class IMXmppEvent extends IMEvent {

    Packet mPacket;
    private boolean mIntercepted = false;; 
    
    IMXmppEvent(IMAddr target, Packet packet) {
        this(target, packet, false);
    }

    IMXmppEvent(IMAddr target, Packet packet, boolean intercepted) {
        super(target);
        mPacket = packet;
        mIntercepted = intercepted;
    }
    
    boolean isIntercepted() { return mIntercepted; }
    
    protected void handleTarget(IMPersona persona) throws ServiceException {
        if (mIntercepted) {
            persona.processIntercepted(mPacket);
        } else {
            persona.process(mPacket);
        }
    }
    
    public String toString() { 
        return "XMPPEvent: " + mPacket.toXML();
    }
    
}
