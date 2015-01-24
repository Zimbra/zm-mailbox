package com.zimbra.cs.index;

import com.zimbra.cs.mailbox.MailItem;

/**
 *
 * @author Greg Solovyev
 *
 */
public class IndexingQueueItemLocator {
    private final int mailboxID;
    private final int mailboxSchemaGroupID;
    private final int mailItemID;
    private final MailItem.Type mailItemType;
    private final String accountID;
    private final boolean inDumpster;
    private final boolean indexAttachments;

    public int getMailboxID() {
        return mailboxID;
    }

    public int getMailItemID() {
        return mailItemID;
    }

    public MailItem.Type getMailItemType() {
        return mailItemType;
    }

    public String getAccountID() {
        return accountID;
    }

    public int getMailboxSchemaGroupID() {
        return mailboxSchemaGroupID;
    }

    public boolean isInDumpster() {
        return inDumpster;
    }

    public boolean attachmentIndexingEnabled() {
        return indexAttachments;
    }

    public IndexingQueueItemLocator(int mbxId, int mailboxSchemaGroupId, int itemId, MailItem.Type itemType, String accountId, boolean dumpster, boolean attachments) {
        mailboxID = mbxId;
        mailboxSchemaGroupID = mailboxSchemaGroupId;
        mailItemID = itemId;
        mailItemType = itemType;
        accountID = accountId;
        inDumpster = dumpster;
        indexAttachments = attachments;
    }
}
