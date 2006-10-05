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

package com.zimbra.cs.im.xmpp.srv;

import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import com.zimbra.cs.im.xmpp.database.DbConnectionManager;
import com.zimbra.cs.im.xmpp.database.SequenceManager;
import com.zimbra.cs.im.xmpp.util.*;
import com.zimbra.cs.im.xmpp.srv.container.BasicModule;
import com.zimbra.cs.im.xmpp.srv.event.UserEventDispatcher;
import com.zimbra.cs.im.xmpp.srv.event.UserEventListener;
import com.zimbra.cs.im.xmpp.srv.user.User;
import com.zimbra.cs.im.xmpp.srv.user.UserManager;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents the user's offline message storage. A message store holds messages that were
 * sent to the user while they were unavailable. The user can retrieve their messages by
 * setting their presence to "available". The messages will then be delivered normally.
 * Offline message storage is optional, in which case a null implementation is returned that
 * always throws UnauthorizedException when adding messages to the store.
 *
 * @author Iain Shigeoka
 */
public class OfflineMessageStore extends BasicModule implements UserEventListener {

    /**
     * Returns the instance of <tt>OfflineMessageStore</tt> being used by the XMPPServer.
     *
     * @return the instance of <tt>OfflineMessageStore</tt> being used by the XMPPServer.
     */
    public static OfflineMessageStore getInstance() {
        return XMPPServer.getInstance().getOfflineMessageStore();
    }

    /**
     * Pool of SAX Readers. SAXReader is not thread safe so we need to have a pool of readers.
     */
    private BlockingQueue<SAXReader> xmlReaders = new LinkedBlockingQueue<SAXReader>();

    /**
     * Constructs a new offline message store.
     */
    public OfflineMessageStore() {
        super("Offline Message Store");
    }

    /**
     * Adds a message to this message store. Messages will be stored and made
     * available for later delivery.
     *
     * @param message the message to store.
     */
    public void addMessage(Message message) {
    }

    /**
     * Returns a Collection of all messages in the store for a user.
     * Messages may be deleted after being selected from the database depending on
     * the delete param.
     *
     * @param username the username of the user who's messages you'd like to receive.
     * @param delete true if the offline messages should be deleted.
     * @return An iterator of packets containing all offline messages.
     */
    public Collection<OfflineMessage> getMessages(String username, boolean delete) {
        List<OfflineMessage> messages = new ArrayList<OfflineMessage>();
        return messages;
    }

    /**
     * Returns the offline message of the specified user with the given creation date. The
     * returned message will NOT be deleted from the database.
     *
     * @param username the username of the user who's message you'd like to receive.
     * @param creationDate the date when the offline message was stored in the database.
     * @return the offline message of the specified user with the given creation stamp.
     */
    public OfflineMessage getMessage(String username, Date creationDate) {
        OfflineMessage message = null;
        return message;
    }

    /**
     * Deletes all offline messages in the store for a user.
     *
     * @param username the username of the user who's messages are going to be deleted.
     */
    public void deleteMessages(String username) {
    }

    private void removeUsernameFromSizeCache(String username) {
    }

    /**
     * Deletes the specified offline message in the store for a user. The way to identify the
     * message to delete is based on the creationDate and username.
     *
     * @param username the username of the user who's message is going to be deleted.
     * @param creationDate the date when the offline message was stored in the database.
     */
    public void deleteMessage(String username, Date creationDate) {
    }

    /**
     * Returns the approximate size (in bytes) of the XML messages stored for
     * a particular user.
     *
     * @param username the username of the user.
     * @return the approximate size of stored messages (in bytes).
     */
    public int getSize(String username) {
        int size = 0;
        return size;
    }

    /**
     * Returns the approximate size (in bytes) of the XML messages stored for all
     * users.
     *
     * @return the approximate size of all stored messages (in bytes).
     */
    public int getSize() {
        int size = 0;
        return size;
    }

    public void userCreated(User user, Map params) {
        //Do nothing
    }

    public void userDeleting(User user, Map params) {
        // Delete all offline messages of the user
        deleteMessages(user.getUsername());
    }

    public void userModified(User user, Map params) {
        //Do nothing
    }

    public void start() throws IllegalStateException {
        super.start();
        // Initialize the pool of sax readers
        for (int i=0; i<10; i++) {
            xmlReaders.add(new SAXReader());
        }
        // Add this module as a user event listener so we can delete
        // all offline messages when a user is deleted
        UserEventDispatcher.addListener(this);
    }

    public void stop() {
        super.stop();
        // Clean up the pool of sax readers
        xmlReaders.clear();
        // Remove this module as a user event listener
        UserEventDispatcher.removeListener(this);
    }
}