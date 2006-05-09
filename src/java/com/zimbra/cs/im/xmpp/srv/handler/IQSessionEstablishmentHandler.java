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

package com.zimbra.cs.im.xmpp.srv.handler;

import com.zimbra.cs.im.xmpp.srv.IQHandlerInfo;
import com.zimbra.cs.im.xmpp.srv.auth.UnauthorizedException;
import org.xmpp.packet.IQ;

/**
 * Activate client sessions once resource binding has been done. Clients need to active their
 * sessions in order to engage in instant messaging and presence activities. The server may
 * deny sessions activations if the max number of sessions in the server has been reached or
 * if a user does not have permissions to create sessions.<p>
 *
 * Current implementation does not check any of the above conditions. However, future versions
 * may add support for those checkings.
 *
 * @author Gaston Dombiak
 */
public class IQSessionEstablishmentHandler extends IQHandler {

    private IQHandlerInfo info;

    public IQSessionEstablishmentHandler() {
        super("Session Establishment handler");
        info = new IQHandlerInfo("session", "urn:ietf:params:xml:ns:xmpp-session");
    }

    public IQ handleIQ(IQ packet) throws UnauthorizedException {
        // Just answer that the session has been activated
        IQ reply = IQ.createResultIQ(packet);
        reply.setChildElement(packet.getChildElement().createCopy());
        return reply;
    }

    public IQHandlerInfo getInfo() {
        return info;
    }
}
