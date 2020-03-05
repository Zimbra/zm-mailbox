package com.zimbra.cs.index.queue;

import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.cs.mailbox.MailboxIndex.ItemIndexDeletionInfo;


/**
 * Describes a queued "delete from index" task
 * @author Greg Solovyev
 *
 */
public class DeleteFromIndexTaskLocator extends AbstractIndexingTasksLocator {
    private final List<ItemIndexDeletionInfo> itemIds;

    public DeleteFromIndexTaskLocator(ItemIndexDeletionInfo itemId, String accountID, int mailboxID, int mailboxSchemaGroupID) {
        super(mailboxID, mailboxSchemaGroupID, accountID);
        itemIds = Lists.newArrayList();
        itemIds.add(itemId);
    }

    public DeleteFromIndexTaskLocator(List<ItemIndexDeletionInfo> itemIds, String accountID, int mailboxID, int mailboxSchemaGroupID) {
        super(mailboxID, mailboxSchemaGroupID, accountID);
        this.itemIds = Lists.newArrayList();
        this.itemIds.addAll(itemIds);
    }

    public List<ItemIndexDeletionInfo> getItemIds() {
        return itemIds;
    }
}
