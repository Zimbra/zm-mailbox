package com.zimbra.cs.index.queue;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.mailbox.MailItem;

/**
 * Describes a queued indexing tasks
 *
 * @author Greg Solovyev
 *
 */
public class AddToIndexTaskLocator extends AbstractIndexingTasksLocator {
    private final List<MailItemIdentifier> mailItemsToAdd;
    private final boolean reindex; // set to TRUE for batch re-indexing tasks

    public void addMailItem(MailItem mailItem) {
        mailItemsToAdd.add(new MailItemIdentifier(mailItem.getId(), mailItem.getType(), mailItem.inDumpster()));
    }

    public AddToIndexTaskLocator(MailItem item, String accountID, int mailboxID, int mailboxSchemaGroupId) {
        super(mailboxID, mailboxSchemaGroupId, accountID);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        mailItemsToAdd.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        reindex = false;
    }

    public AddToIndexTaskLocator(MailItem item, String accountID, int mailboxID, int mailboxSchemaGroupId,
            boolean indexAttachments) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        mailItemsToAdd.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        reindex = false;
    }

    public AddToIndexTaskLocator(MailItem item, String accountID, int mailboxID, int mailboxSchemaGroupId,
            boolean indexAttachments, boolean reIndex) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        mailItemsToAdd.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        reindex = reIndex;
    }

    public AddToIndexTaskLocator(List<MailItem> items, String accountID, int mailboxID, int mailboxSchemaGroupId,
            boolean indexAttachments) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        for (MailItem item : items) {
            mailItemsToAdd.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        }
        reindex = false;
    }

    public AddToIndexTaskLocator(List<MailItem> items,
            String accountID, int mailboxID,
            int mailboxSchemaGroupId, boolean indexAttachments,
             boolean reIndex) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        for (MailItem item : items) {
            mailItemsToAdd.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        }
        reindex = reIndex;
    }

    public AddToIndexTaskLocator(String accountID, int mailboxID, int mailboxSchemaGroupId, boolean indexAttachments,
            boolean reIndex, List<MailItemIdentifier> items) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        for (MailItemIdentifier item : items) {
            mailItemsToAdd.add(item);
        }
        reindex = reIndex;
    }

    /**
     *
     * @return List<MailItemIdentifier> list of items to be indexed
     */
    public List<MailItemIdentifier> getMailItemsToAdd() {
        return mailItemsToAdd;
    }

    public boolean isReindex() {
        return reindex;
    }

}
