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

package com.zimbra.cs.im.xmpp.srv.privacy;

import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import com.zimbra.cs.im.xmpp.database.DbConnectionManager;
import com.zimbra.cs.im.xmpp.util.Log;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provider for the privacy lists system. Privacy lists are read and written
 * from the <tt>jivePrivacyList</tt> database table.
 *
 * @author Gaston Dombiak
 */
public class PrivacyListProvider {

    public PrivacyListProvider() {
        super();
    }

    /**
     * Returns the names of the existing privacy lists indicating which one is the
     * default privacy list associated to a user.
     *
     * @param username the username of the user to get his privacy lists names.
     * @return the names of the existing privacy lists with a default flag.
     */
    public Map<String, Boolean> getPrivacyLists(String username) {
        Map<String, Boolean> names = new HashMap<String, Boolean>();
        return names;
    }

    /**
     * Loads the requested privacy list from the database. Returns <tt>null</tt> if a list
     * with the specified name does not exist.
     *
     * @param username the username of the user to get his privacy list.
     * @param listName name of the list to load.
     * @return the privacy list with the specified name or <tt>null</tt> if a list
     *         with the specified name does not exist.
     */
    public PrivacyList loadPrivacyList(String username, String listName) {
        PrivacyList privacyList = null;
        return privacyList;
    }

    /**
     * Loads the default privacy list of a given user from the database. Returns <tt>null</tt>
     * if no list was found.
     *
     * @param username the username of the user to get his default privacy list.
     * @return the default privacy list or <tt>null</tt> if no list was found.
     */
    public PrivacyList loadDefaultPrivacyList(String username) {
        PrivacyList privacyList = null;
        return privacyList;
    }

    /**
     * Creates and saves the new privacy list to the database.
     *
     * @param username the username of the user that created a new privacy list.
     * @param list the PrivacyList to save.
     */
    public void createPrivacyList(String username, PrivacyList list) {
    }

    /**
     * Updated the existing privacy list in the database.
     *
     * @param username the username of the user that updated a privacy list.
     * @param list the PrivacyList to update in the database.
     */
    public void updatePrivacyList(String username, PrivacyList list) {
    }

    /**
     * Deletes an existing privacy list from the database.
     *
     * @param username the username of the user that deleted a privacy list.
     * @param listName the name of the PrivacyList to delete.
     */
    public void deletePrivacyList(String username, String listName) {
    }

    /**
     * Deletes all existing privacy list from the database for the given user.
     *
     * @param username the username of the user whose privacy lists are going to be deleted.
     */
    public void deletePrivacyLists(String username) {
    }
}
