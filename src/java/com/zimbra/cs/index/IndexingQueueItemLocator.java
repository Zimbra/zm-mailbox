package com.zimbra.cs.index;

import com.zimbra.cs.mailbox.MailItem;

/**
 *
 * @author Greg Solovyev
 *
 */
public class IndexingQueueItemLocator {
    private final int mailboxID;
    private final int mailItemID;
    private final MailItem.Type mailItemType;

    public int getMailboxID() {
        return mailboxID;
    }

    public int getMailItemID() {
        return mailItemID;
    }

    public MailItem.Type getMailItemType() {
        return mailItemType;
    }

    public IndexingQueueItemLocator(int mbxId, int itemId, MailItem.Type itemType) {
        mailboxID = mbxId;
        mailItemID = itemId;
        mailItemType = itemType;
    }
}
