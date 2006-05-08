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
 
package com.zimbra.cs.im.xmpp.srv.handler;

import org.dom4j.Element;
import com.zimbra.cs.im.xmpp.srv.IQHandlerInfo;
import com.zimbra.cs.im.xmpp.srv.PresenceManager;
import com.zimbra.cs.im.xmpp.srv.XMPPServer;
import com.zimbra.cs.im.xmpp.srv.auth.UnauthorizedException;
import com.zimbra.cs.im.xmpp.srv.disco.ServerFeaturesProvider;
import com.zimbra.cs.im.xmpp.srv.roster.RosterItem;
import com.zimbra.cs.im.xmpp.srv.roster.RosterManager;
import com.zimbra.cs.im.xmpp.srv.user.User;
import com.zimbra.cs.im.xmpp.srv.user.UserManager;
import com.zimbra.cs.im.xmpp.srv.user.UserNotFoundException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Implements the TYPE_IQ jabber:iq:last protocol (last activity). Allows users to find out
 * the number of seconds another user has been offline. This information is only available to
 * those users that already subscribed to the users presence. Otherwhise, a <tt>forbidden</tt>
 * error will be returned.
 *
 * @author Gaston Dombiak
 */
public class IQLastActivityHandler extends IQHandler implements ServerFeaturesProvider {

    private IQHandlerInfo info;
    private PresenceManager presenceManager;
    private RosterManager rosterManager;

    public IQLastActivityHandler() {
        super("XMPP Last Activity Handler");
        info = new IQHandlerInfo("query", "jabber:iq:last");
    }

    public IQ handleIQ(IQ packet) throws UnauthorizedException {
        IQ reply = IQ.createResultIQ(packet);
        Element lastActivity = reply.setChildElement("query", "jabber:iq:last");
        String sender = packet.getFrom().getNode();
        String username = packet.getTo() == null ? null : packet.getTo().getNode();

        // Check if any of the usernames is null
        if (sender == null || username == null) {
            reply.setError(PacketError.Condition.forbidden);
            return reply;
        }

        try {
            RosterItem item = rosterManager.getRoster(username).getRosterItem(packet.getFrom());
            // Check that the user requesting this information is subscribed to the user's presence
            if (item.getSubStatus() == RosterItem.SUB_FROM ||
                    item.getSubStatus() == RosterItem.SUB_BOTH) {
                if (sessionManager.getSessions(username).isEmpty()) {
                    User user = UserManager.getInstance().getUser(username);
                    // The user is offline so answer the user's "last available time and the
                    // status message of the last unavailable presence received from the user"
                    long lastActivityTime = presenceManager.getLastActivity(user);
                    lastActivity.addAttribute("seconds", String.valueOf(lastActivityTime));
                    String lastStatus = presenceManager.getLastPresenceStatus(user);
                    if (lastStatus != null && lastStatus.length() > 0) {
                        lastActivity.setText(lastStatus);
                    }
                }
                else {
                    // The user is online so answer seconds=0
                    lastActivity.addAttribute("seconds", "0");
                }
            }
            else {
                reply.setError(PacketError.Condition.forbidden);
            }
        }
        catch (UserNotFoundException e) {
            reply.setError(PacketError.Condition.forbidden);
        }
        return reply;
    }

    public IQHandlerInfo getInfo() {
        return info;
    }

    public Iterator<String> getFeatures() {
        ArrayList<String> features = new ArrayList<String>();
        features.add("jabber:iq:last");
        return features.iterator();
    }

    public void initialize(XMPPServer server) {
        super.initialize(server);
        presenceManager = server.getPresenceManager();
        rosterManager = server.getRosterManager();
    }
}
