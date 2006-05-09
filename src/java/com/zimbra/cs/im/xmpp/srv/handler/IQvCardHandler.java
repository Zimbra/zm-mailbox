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

import org.dom4j.Element;
import com.zimbra.cs.im.xmpp.util.Log;
import com.zimbra.cs.im.xmpp.srv.IQHandlerInfo;
import com.zimbra.cs.im.xmpp.srv.PacketException;
import com.zimbra.cs.im.xmpp.srv.XMPPServer;
import com.zimbra.cs.im.xmpp.srv.auth.UnauthorizedException;
import com.zimbra.cs.im.xmpp.srv.user.User;
import com.zimbra.cs.im.xmpp.srv.user.UserManager;
import com.zimbra.cs.im.xmpp.srv.user.UserNotFoundException;
import com.zimbra.cs.im.xmpp.srv.vcard.VCardManager;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

/**
 * Implements the TYPE_IQ vcard-temp protocol. Clients
 * use this protocol to set and retrieve the vCard information
 * associated with someone's account.
 * <p/>
 * A 'get' query retrieves the vcard for the addressee.
 * A 'set' query sets the vcard information for the sender's account.
 * <p/>
 * Currently an empty implementation to allow usage with normal
 * clients. Future implementation needed.
 * <p/>
 * <h2>Assumptions</h2>
 * This handler assumes that the request is addressed to the server.
 * An appropriate TYPE_IQ tag matcher should be placed in front of this
 * one to route TYPE_IQ requests not addressed to the server to
 * another channel (probably for direct delivery to the recipient).
 * <p/>
 * <h2>Warning</h2>
 * There should be a way of determining whether a session has
 * authorization to access this feature. I'm not sure it is a good
 * idea to do authorization in each handler. It would be nice if
 * the framework could assert authorization policies across channels.
 * <p/>
 * <h2>Warning</h2>
 * I have noticed incompatibility between vCard XML used by Exodus and Psi.
 * There is a new vCard standard going through the JSF JEP process. We might
 * want to start either standardizing on clients (probably the most practical),
 * sending notices for non-conformance (useful),
 * or attempting to translate between client versions (not likely).
 *
 * @author Iain Shigeoka
 */
public class IQvCardHandler extends IQHandler {

    private IQHandlerInfo info;
    private XMPPServer server;
    private UserManager userManager;

    public IQvCardHandler() {
        super("XMPP vCard Handler");
        info = new IQHandlerInfo("vCard", "vcard-temp");
    }

    public IQ handleIQ(IQ packet) throws UnauthorizedException, PacketException {
        IQ result = IQ.createResultIQ(packet);
        IQ.Type type = packet.getType();
        if (type.equals(IQ.Type.set)) {
            try {
                User user = userManager.getUser(packet.getFrom().getNode());
                Element vcard = packet.getChildElement();
                if (vcard != null) {
                    VCardManager.getInstance().setVCard(user.getUsername(), vcard);
                }
            }
            catch (UserNotFoundException e) {
                result = IQ.createResultIQ(packet);
                result.setChildElement(packet.getChildElement().createCopy());
                result.setError(PacketError.Condition.item_not_found);
            }
            catch (Exception e) {
                Log.error(e);
                result.setError(PacketError.Condition.internal_server_error);
            }
        }
        else if (type.equals(IQ.Type.get)) {
            JID recipient = packet.getTo();
            // If no TO was specified then get the vCard of the sender of the packet
            if (recipient == null) {
                recipient = packet.getFrom();
            }
            // By default return an empty vCard
            result.setChildElement("vCard", "vcard-temp");
            // Only try to get the vCard values of non-anonymous users
            if (recipient != null) {
                if (recipient.getNode() != null && server.isLocal(recipient)) {
                    VCardManager vManager = VCardManager.getInstance();
                    Element userVCard = vManager.getVCard(recipient.getNode());
                    if (userVCard != null) {
                        result.setChildElement(userVCard);
                    }
                }
                else {
                    result = IQ.createResultIQ(packet);
                    result.setChildElement(packet.getChildElement().createCopy());
                    result.setError(PacketError.Condition.item_not_found);
                }
            }
        }
        else {
            result.setChildElement(packet.getChildElement().createCopy());
            result.setError(PacketError.Condition.not_acceptable);
        }
        return result;
    }

    public void initialize(XMPPServer server) {
        super.initialize(server);
        this.server = server;
        userManager = server.getUserManager();
    }

    public IQHandlerInfo getInfo() {
        return info;
    }
}
