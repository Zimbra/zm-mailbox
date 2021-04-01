package com.zimbra.cs.index.queue;

import java.util.List;

import com.zimbra.common.util.ZimbraLog;

public class AddMailItemIdsToIndexTask extends AbstractIndexingTasksLocator {
    private final List<Integer> mailItemIds;

    protected AddMailItemIdsToIndexTask(List<Integer> items, int mailboxID, int mailboxSchemaGroupID, String accountID,
            boolean indexAttachments) {
        super(mailboxID, mailboxSchemaGroupID, accountID, indexAttachments);
        mailItemIds = items;
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
}
