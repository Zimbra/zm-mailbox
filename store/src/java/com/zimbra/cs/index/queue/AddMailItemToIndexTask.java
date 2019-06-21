package com.zimbra.cs.index.queue;

import com.zimbra.cs.mailbox.MailItem;

public class AddMailItemToIndexTask extends AbstractIndexingTasksLocator {
    private final MailItem mailItem;

    // set to TRUE for batch re-indexing tasks
    private final boolean reindex;

    public AddMailItemToIndexTask(MailItem item, String accountID, int mailboxID, int mailboxSchemaGroupId,
            boolean indexAttachments, boolean reIndex) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItem = item;
        reindex = reIndex;
    }

    /**
     * @return MailItem to be indexed
     */
    public MailItem getMailItemToAdd() {
        return mailItem;
    }

    public boolean isReindex() {
        return reindex;
    }
}
