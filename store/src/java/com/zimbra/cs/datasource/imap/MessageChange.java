/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource.imap;

import com.zimbra.cs.mailbox.Message;

final class MessageChange {
    public enum Type {
        ADDED, UPDATED, MOVED, DELETED, MODIFIED
    }

    private final Type type;
    private final int itemId;
    private final Message msg;
    private final ImapMessage tracker;

    public static MessageChange added(Message msg) {
        return new MessageChange(Type.ADDED, msg.getId(), msg, null);
    }

    public static MessageChange updated(Message msg, ImapMessage tracker) {
        return new MessageChange(Type.UPDATED, msg.getId(), msg, tracker);
    }

    public static MessageChange moved(Message msg, ImapMessage tracker) {
        return new MessageChange(Type.MOVED, msg.getId(), msg, tracker);
    }

    public static MessageChange deleted(int itemId, ImapMessage tracker) {
        return new MessageChange(Type.DELETED, itemId, null, tracker);
    }

    public static MessageChange modifiedDraft(Message msg, ImapMessage tracker) {
        return new MessageChange(Type.MODIFIED, msg.getId(), msg, tracker);
    }

    MessageChange(Type type, int itemId, Message msg, ImapMessage tracker) {
        this.type = type;
        this.itemId = itemId;
        this.msg = msg;
        this.tracker = tracker;
    }

    public Type getType() {
        return type;
    }

    public int getItemId() {
        return itemId;
    }

    public Message getMessage() {
        return msg;
    }

    public ImapMessage getTracker() {
        return tracker;
    }

    public boolean isAdded() {
        return type == Type.ADDED;
    }

    public boolean isUpdated() {
        return type == Type.UPDATED;
    }

    public boolean isMoved() {
        return type == Type.MOVED;
    }

    public boolean isDeleted() {
        return type == Type.DELETED;
    }

    public boolean isModified() {
        return type == Type.MODIFIED;
    }

}
