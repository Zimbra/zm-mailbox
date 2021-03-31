package com.zimbra.cs.index.queue;

import java.util.List;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;

public class AddMailItemToIndexTask extends AbstractIndexingTasksLocator {
    private final List<MailItem> mailItems;

    // set to TRUE for batch re-indexing tasks
    private final boolean reindex;

    public AddMailItemToIndexTask(List<MailItem> items, String accountID, int mailboxID, int mailboxSchemaGroupId,
            boolean indexAttachments, boolean reIndex) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItems = items;
        reindex = reIndex;
    }

    /**
     * @return MailItem to be indexed
     */
    public List<MailItem> getMailItemsToAdd() {
        return mailItems;
    }

    /**
     * Function to remove the List of MailItem from the state
     * @param items is List<MailItem>
     */
    public void removeMailItems(List<MailItem> items) {
        ZimbraLog.index.debug("AddMailItemToIndexTask - removeMailItems items called with %d item size.", items.size());
        for (MailItem item : items) {
            mailItems.remove(item);
        }
    }

    public boolean isReindex() {
        return reindex;
    }
}
