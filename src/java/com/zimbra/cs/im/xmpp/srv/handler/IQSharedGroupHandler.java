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
import com.zimbra.cs.im.xmpp.srv.IQHandlerInfo;
import com.zimbra.cs.im.xmpp.srv.XMPPServer;
import com.zimbra.cs.im.xmpp.srv.auth.UnauthorizedException;
import com.zimbra.cs.im.xmpp.srv.group.Group;
import com.zimbra.cs.im.xmpp.srv.roster.RosterManager;
import com.zimbra.cs.im.xmpp.srv.user.UserManager;
import com.zimbra.cs.im.xmpp.srv.user.UserNotFoundException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import java.util.Collection;

/**
 * Handler of IQ packets whose child element is "sharedgroup" with namespace
 * "http://www.jivesoftware.org/protocol/sharedgroup". This handler will return the list of
 * shared groups where the user sending the request belongs.
 *
 * @author Gaston Dombiak
 */
public class IQSharedGroupHandler extends IQHandler {

    private IQHandlerInfo info;
    private String serverName;
    private UserManager userManager;
    private RosterManager rosterManager;

    public IQSharedGroupHandler() {
        super("Shared Groups Handler");
        info = new IQHandlerInfo("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup");
    }

    public IQ handleIQ(IQ packet) throws UnauthorizedException {
        IQ result = IQ.createResultIQ(packet);
        String username = packet.getFrom().getNode();
        if (!serverName.equals(packet.getFrom().getDomain()) || username == null) {
            // Users of remote servers are not allowed to get their "shared groups". Users of
            // remote servers cannot have shared groups in this server.
            // Besides, anonymous users do not belong to shared groups so answer an error
            result.setChildElement(packet.getChildElement().createCopy());
            result.setError(PacketError.Condition.not_allowed);
            return result;
        }

        try {
            Collection<Group> groups = rosterManager.getSharedGroups(userManager.getUser(username));
            Element sharedGroups = result.setChildElement("sharedgroup",
                    "http://www.jivesoftware.org/protocol/sharedgroup");
            for (Group sharedGroup : groups) {
                String displayName = sharedGroup.getProperties().get("sharedRoster.displayName");
                if (displayName != null) {
                    sharedGroups.addElement("group").setText(displayName);
                }
            }
        }
        catch (UserNotFoundException e) {
            // User not found return an error. This case should never happen.
            result.setChildElement(packet.getChildElement().createCopy());
            result.setError(PacketError.Condition.not_allowed);
        }
        return result;
    }

    public IQHandlerInfo getInfo() {
        return info;
    }

    public void initialize(XMPPServer server) {
        super.initialize(server);
        serverName = server.getServerInfo().getName();
        userManager = server.getUserManager();
        rosterManager = server.getRosterManager();
    }
}
