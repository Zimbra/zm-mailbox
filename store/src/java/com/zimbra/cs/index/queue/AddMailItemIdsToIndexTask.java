package com.zimbra.cs.index.queue;

import java.util.List;

import com.zimbra.common.util.ZimbraLog;

public class AddMailItemIdsToIndexTask extends AbstractIndexingTasksLocator {
    private final List<Integer> mailItemIds;
    private final boolean mailboxVerified;

    protected AddMailItemIdsToIndexTask(List<Integer> items, boolean mailboxVerified, int mailboxID,
            int mailboxSchemaGroupID, String accountID, boolean indexAttachments) {
        super(mailboxID, mailboxSchemaGroupID, accountID, indexAttachments);
        mailItemIds = items;
        this.mailboxVerified = mailboxVerified;
    }

    /**
     * Function to return list of MailItem ids from the state
     * 
     * @return List<Integer> list of MailItem Ids
     */
    public List<Integer> getMailItemIdsToAdd() {
        return mailItemIds;
    }

    /**
     * Function to remove the List of MailItem ids from the state
     * 
     * @param items
     *            is List<MailItem>
     */
    public void removeMailItems(List<Integer> items) {
        ZimbraLog.index.debug("AddMailItemIdsToIndexTask - removeMailItems items called with %d item size.",
                items.size());
        for (Integer item : items) {
            mailItemIds.remove(item);
        }
    }

    /**
     * Function that returns the state of mailboxVerified
     * @return false if mailbox verification is incomplete else true
     */
    public boolean isMailboxVerified() {
        return mailboxVerified;
    }
}
