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

package com.zimbra.cs.im.xmpp.srv.muc.spi;

import java.util.Date;

import com.zimbra.cs.im.xmpp.srv.muc.MUCRoom;
import org.xmpp.packet.Message;
import org.xmpp.packet.JID;

/**
 * Represents an entry in the conversation log of a room. An entry basically obtains the necessary
 * information to log from the message adding a timestamp of when the message was sent to the room.
 * 
 * @author Gaston Dombiak
 */
class ConversationLogEntry {

    private Date date;

    private String subject;

    private String body;

    private JID sender;
    
    private String nickname;
    
    private long roomID;

    /**
     * Creates a new ConversationLogEntry that registers that a given message was sent to a given
     * room on a given date.
     * 
     * @param date the date when the message was sent to the room.
     * @param room the room that received the message.
     * @param message the message to log as part of the conversation in the room.
     * @param sender the real XMPPAddress of the sender (e.g. john@example.org). 
     */
    public ConversationLogEntry(Date date, MUCRoom room, Message message, JID sender) {
        this.date = date;
        this.subject = message.getSubject();
        this.body = message.getBody();
        this.sender = sender;
        this.roomID = room.getID();
        this.nickname = message.getFrom().getResource();
    }

    /**
     * Returns the body of the logged message.
     * 
     * @return the body of the logged message.
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns the XMPP address of the logged message's sender.
     * 
     * @return the XMPP address of the logged message's sender.
     */
    public JID getSender() {
        return sender;
    }

    /**
     * Returns the nickname that the user had at the moment that the message was sent to the room.
     * 
     * @return the nickname that the user had at the moment that the message was sent to the room.
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Returns the subject of the logged message.
     * 
     * @return the subject of the logged message.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Returns the date when the logged message was sent to the room.
     * 
     * @return the date when the logged message was sent to the room.
     */
    public Date getDate() {
        return date;
    }

    /**
     * Returns the ID of the room where the message was sent.
     * 
     * @return the ID of the room where the message was sent.
     */
    public long getRoomID() {
        return roomID;
    }

}