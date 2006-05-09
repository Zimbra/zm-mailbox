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

package com.zimbra.cs.im.xmpp.srv.muc;

import com.zimbra.cs.im.xmpp.util.NotFoundException;
import com.zimbra.cs.im.xmpp.srv.ChannelHandler;
import org.xmpp.packet.JID;

import java.util.Iterator;

/**
 * The chat user is a separate user abstraction for interacting with
 * the chat server. Centralizing chat users to the Jabber entity that
 * sends and receives the chat messages allows us to create quality of
 * service, authorization, and resource decisions on a real-user basis.
 * <p/>
 * Most chat users in a typical s2s scenario will not be local users.
 * </p><p>
 * MUCUsers play one or more roles in one or more chat rooms on the
 * server.
 *
 * @author Gaston Dombiak
 */
public interface MUCUser extends ChannelHandler {

    /**
     * Obtain a user ID (useful for database indexing).
     *
     * @return The user's id number if any (-1 indicates the implementation doesn't support ids)
     */
    long getID();

    /**
      * Obtain the address of the user. The address is used by services like the core
      * server packet router to determine if a packet should be sent to the handler.
      * Handlers that are working on behalf of the server should use the generic server
      * hostname address (e.g. server.com).
      *
      * @return the address of the packet handler.
      */
     public JID getAddress();

    /**
     * Obtain the role of the user in a particular room.
     *
     * @param roomName The name of the room we're interested in
     * @return The role the user plays in that room
     * @throws NotFoundException     if the user does not have a role in the given room
     */
    MUCRole getRole(String roomName) throws NotFoundException;

    /**
     * Get all roles for this user.
     *
     * @return Iterator over all roles for this user
     */
    Iterator<MUCRole> getRoles();

    /**
     * Adds the role of the user in a particular room.
     *
     * @param roomName The name of the room.
     * @param role The new role of the user.
     */
    void addRole(String roomName, MUCRole role);

    /**
     * Removes the role of the user in a particular room.<p>
     *
     * Note: PREREQUISITE: A lock on this object has already been obtained.
     *
     * @param roomName The name of the room we're being removed
     */
    void removeRole(String roomName);

    /**
     * Get time (in milliseconds from System currentTimeMillis()) since last packet.
     *
     * @return The time when the last packet was sent from this user
     */
    long getLastPacketTime();
}