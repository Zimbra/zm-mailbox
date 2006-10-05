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

package com.zimbra.cs.im.xmpp.srv.roster;

import com.zimbra.cs.im.xmpp.database.DbConnectionManager;
import com.zimbra.cs.im.xmpp.database.SequenceManager;
import com.zimbra.cs.im.xmpp.util.JiveConstants;
import com.zimbra.cs.im.xmpp.util.LocaleUtils;
import com.zimbra.cs.im.xmpp.util.Log;
import com.zimbra.cs.im.xmpp.srv.user.UserAlreadyExistsException;
import com.zimbra.cs.im.xmpp.srv.user.UserNotFoundException;
import org.xmpp.packet.JID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines the provider methods required for creating, reading, updating and deleting roster
 * items.<p>
 *
 * Rosters are another user resource accessed via the user or chatbot's long ID. A user/chatbot
 * may have zero or more roster items and each roster item may have zero or more groups. Each
 * roster item is additionaly keyed on a XMPP jid. In most cases, the entire roster will be read
 * in from memory and manipulated or sent to the user. However some operations will need to retrive
 * specific roster items rather than the entire roster.
 *
 * @author Iain Shigeoka
 */
public class RosterItemProvider {

    private static RosterItemProvider instance = new RosterItemProvider();

    public static RosterItemProvider getInstance() {
        return instance;
    }

    /**
     * Creates a new roster item for the given user (optional operation).<p>
     *
     * <b>Important!</b> The item passed as a parameter to this method is strictly a convenience
     * for passing all of the data needed for a new roster item. The roster item returned from the
     * method will be cached by Wildfire. In some cases, the roster item passed in will be passed
     * back out. However, if an implementation may return RosterItems as a separate class
     * (for example, a RosterItem that directly accesses the backend storage, or one that is an
     * object in an object database).<p>
     *
     * If you don't want roster items edited through wildfire, throw
     * UnsupportedOperationException.
     *
     * @param username the username of the user/chatbot that owns the roster item
     * @param item the settings for the roster item to create
     * @return The created roster item
     */
    public RosterItem createItem(String username, RosterItem item)
            throws UserAlreadyExistsException
    {
        return item;
    }

    /**
     * Update the roster item in storage with the information contained in the given item
     * (optional operation).<p>
     *
     * If you don't want roster items edited through wildfire, throw UnsupportedOperationException.
     *
     * @param username the username of the user/chatbot that owns the roster item
     * @param item   The roster item to update
     * @throws UserNotFoundException If no entry could be found to update
     */
    public void updateItem(String username, RosterItem item) throws UserNotFoundException {
    }

    /**
     * Delete the roster item with the given itemJID for the user (optional operation).<p>
     *
     * If you don't want roster items deleted through wildfire, throw
     * UnsupportedOperationException.
     *
     * @param username the long ID of the user/chatbot that owns the roster item
     * @param rosterItemID The roster item to delete
     */
    public void deleteItem(String username, long rosterItemID) {
    }

    /**
     * Returns an iterator on the usernames whose roster includes the specified JID.
     *
     * @param jid the jid that the rosters should include.
     * @return an iterator on the usernames whose roster includes the specified JID.
     */
    public Iterator<String> getUsernames(String jid) {
        List<String> answer = new ArrayList<String>();
        return answer.iterator();
    }

    /**
     * Obtain a count of the number of roster items available for the given user.
     *
     * @param username the username of the user/chatbot that owns the roster items
     * @return The number of roster items available for the user
     */
    public int getItemCount(String username) {
        int count = 0;
        return count;
    }

    /**
     * Retrieve an iterator of RosterItems for the given user.<p>
     *
     * This method will commonly be called when a user logs in. The data will be cached
     * in memory when possible. However, some rosters may be very large so items may need
     * to be retrieved from the provider more frequently than usual for provider data.
     *
     * @param username the username of the user/chatbot that owns the roster items
     * @return An iterator of all RosterItems owned by the user
     */
    public Iterator<RosterItem> getItems(String username) {
        LinkedList<RosterItem> itemList = new LinkedList<RosterItem>();
        return itemList.iterator();
    }

    /**
     * Insert the groups into the given roster item.
     *
     * @param rosterID The roster ID of the item the groups belong to
     * @param iter     An iterator over the group names to insert
     */
    private void insertGroups(long rosterID, Iterator<String> iter, Connection con) throws SQLException
    {
    }
}
