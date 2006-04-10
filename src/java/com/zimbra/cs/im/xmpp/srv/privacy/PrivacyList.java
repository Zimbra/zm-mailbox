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
 * Part of the Zimbra Collaboration Suite Server.
 *
 * The Original Code is Copyright (C) Jive Software. Used with permission
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.privacy;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import com.zimbra.cs.im.xmpp.util.Log;
import com.zimbra.cs.im.xmpp.srv.XMPPServer;
import com.zimbra.cs.im.xmpp.srv.roster.Roster;
import com.zimbra.cs.im.xmpp.srv.user.UserNotFoundException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A privacy list contains a set of rules that define if communication with the list owner
 * is allowed or denied. Users may have zero, one or more privacy lists. When a list is the
 * default list then that list is going to be used by default for all user sessions or analyze,
 * when user is offline, if communication may proceed (e.g. define if a message should be stored
 * offline). A user may configure is he wants to have a default list or not. When no default list
 * is defined then communication will not be blocked. However, users may define an active list
 * for a particular session. Active lists override default list (if there is one) and will be used
 * only for the duration of the session.
 *
 * @author Gaston Dombiak
 */
public class PrivacyList implements Serializable {

    private JID userJID;
    private String name;
    private boolean isDefault;
    private List<PrivacyItem> items = new ArrayList<PrivacyItem>();
    private Roster roster;

    public PrivacyList(String username, String name, boolean isDefault, Element listElement) {
        this.userJID = XMPPServer.getInstance().createJID(username, null);
        this.name = name;
        this.isDefault = isDefault;
        // Set the new list items
        updateList(listElement);
    }

    /**
     * Returns the name that uniquely identifies this list among the users lists.
     *
     * @return the name that uniquely identifies this list among the users lists.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true if this privacy list is the default list to apply for the user. Default
     * privacy lists can be overriden per session by setting an active privacy list.
     *
     * @return true if this privacy list is the default list to apply for the user.
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Sets if this privacy list is the default list to apply for the user. Default
     * privacy lists can be overriden per session by setting an active privacy list.
     *
     * @param isDefault true if this privacy list is the default list to apply for the user.
     */
    public void setDefaultList(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * Returns true if the specified packet must be blocked based on this privacy list rules.
     * Rules are going to be analyzed based on their order (in ascending order). When a rule
     * is matched then communication will be blocked or allowed based on that rule. No more
     * further analysis is going to be made.
     *
     * @param packet the packet to analyze if it must be blocked.
     * @return true if the specified packet must be blocked based on this privacy list rules.
     */
    public boolean shouldBlockPacket(Packet packet) {
        if (packet.getFrom() == null) {
            // Sender is the server so it's not denied
            return false;
        }
        // Iterate over the rules and check each rule condition
        for (PrivacyItem item : items) {
            if (item.matchesCondition(packet, roster, userJID)) {
                if (item.isAllow()) {
                    return false;
                }
                if (Log.isDebugEnabled()) {
                    Log.debug("Packet was blocked: " + packet);
                }
                return true;
            }
        }
        // If no rule blocked the communication then allow the packet to flow
        return false;
    }

    /**
     * Returns an Element with the privacy list XML representation.
     *
     * @return an Element with the privacy list XML representation.
     */
    public Element asElement() {
        Element listElement = DocumentFactory.getInstance().createDocument().addElement("list");
        listElement.addAttribute("name", getName());
        // Add the list items to the result
        for (PrivacyItem item : items) {
            listElement.add(item.asElement());
        }
        return listElement;
    }

    /**
     * Sets the new list items based on the specified Element. The Element must contain
     * a list of item elements.
     *
     * @param listElement the element containing a list of items.
     */
    public void updateList(Element listElement) {
        // Reset the list of items of this list
        items = new ArrayList<PrivacyItem>();

        List<Element> itemsElements = listElement.elements("item");
        for (Element itemElement : itemsElements) {
            PrivacyItem newItem = new PrivacyItem(itemElement);
            items.add(newItem);
            // If the user's roster is required to evaluation whether a packet must be blocked
            // then ensure that the roster is available
            if (roster == null && newItem.isRosterRequired()) {
                try {
                    roster = XMPPServer.getInstance().getRosterManager().getRoster(userJID.getNode());
                }
                catch (UserNotFoundException e) {
                    Log.warn("Privacy item removed since roster of user was not found: " +
                            userJID.getNode());
                    items.remove(newItem);
                }
            }
        }
        // Sort items collections
        Collections.sort(items);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object != null && object instanceof PrivacyList) {
            return name.equals(((PrivacyList)object).getName());
        }
        else {
            return false;
        }
    }
}
