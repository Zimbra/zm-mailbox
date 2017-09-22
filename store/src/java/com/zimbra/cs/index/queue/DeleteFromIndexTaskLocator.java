package com.zimbra.cs.index.queue;

import java.util.List;

import com.google.common.collect.Lists;


/**
 * Describes a queued "delete from index" task
 * @author Greg Solovyev
 *
 */
public class DeleteFromIndexTaskLocator extends AbstractIndexingTasksLocator {
    private final List<Integer> itemIds;

    public DeleteFromIndexTaskLocator(Integer itemId, String accountID, int mailboxID, int mailboxSchemaGroupID) {
        super(mailboxID, mailboxSchemaGroupID, accountID);
        itemIds = Lists.newArrayList();
        itemIds.add(itemId);
    }

    public DeleteFromIndexTaskLocator(List<Integer> itemIds, String accountID, int mailboxID, int mailboxSchemaGroupID) {
        super(mailboxID, mailboxSchemaGroupID, accountID);
        this.itemIds = Lists.newArrayList();
        this.itemIds.addAll(itemIds);
    }

    public List<Integer> getItemIds() {
        return itemIds;
    }
}
