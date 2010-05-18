package com.zimbra.cs.datasource.imap;

import com.zimbra.cs.mailbox.Message;

final class MessageChange {
    public enum Type {
        ADDED, UPDATED, MOVED, DELETED
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
}
