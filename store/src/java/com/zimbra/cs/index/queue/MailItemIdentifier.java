package com.zimbra.cs.index.queue;

import com.zimbra.cs.mailbox.MailItem;

public class MailItemIdentifier {
    private final MailItem.Type type;
    private final int id;
    private final boolean inDumpster;

    public MailItemIdentifier(int id) {
        this.id = id;
        this.type = MailItem.Type.UNKNOWN;
        this.inDumpster = false;
    }

    public MailItemIdentifier(int id, MailItem.Type type, boolean inDumpster) {
        this.id = id;
        this.type = type;
        this.inDumpster = inDumpster;
    }

    public MailItemIdentifier(MailItem item) {
        this(item.getId(), item.getType(), item.inDumpster());
    }

    public int getId() {
        return id;
    }

    public MailItem.Type getType() {
        return type;
    }

    public boolean isInDumpster() {
        return inDumpster;
    }
}
