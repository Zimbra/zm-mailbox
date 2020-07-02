package com.zimbra.cs.index.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;
import com.zimbra.cs.mailbox.MailboxIndex.ItemIndexDeletionInfo;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.MockMailItem;
import com.zimbra.cs.mailbox.ReIndexStatus;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public abstract class AbstractIndexingQueueAdapterTest {

    protected IndexingQueueAdapter adapter;

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
        if (adapter != null) {
            adapter.drain();
            adapter.clearAllTaskCounts();
        }
    }

    @AfterClass
    public static void destroy() throws Exception {
        Account acc = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        acc.deleteAccount();
        //Provisioning.getInstance().getLocalServer().setIndexingQueueProvider("");
    }

    @Test
    public void testPutTakeSingleIndexingTask() throws Exception {
        Assume.assumeTrue(isQueueSourceAvailable());
        // publish a message
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        UnderlyingData underlyingData = new UnderlyingData();
        underlyingData.setSubject("test subject");
        underlyingData.folderId = Mailbox.ID_FOLDER_INBOX;
        underlyingData.name = "name";
        underlyingData.type = MailItem.Type.MESSAGE.toByte();
        underlyingData.uuid = account.getUid();
        underlyingData.setBlobDigest("test digest");
        MailItem item = new MockMailItem(mbox, underlyingData);
        adapter.put(new AddToIndexTaskLocator(item, account.getId(), mbox.getId(), mbox.getSchemaGroupId(), false));

        // verify that message is in the queue
        assertTrue("item queue should not be empty", adapter.hasMoreItems());
        AbstractIndexingTasksLocator queuedItem = adapter.peek();
        assertNotNull("empty queued item", queuedItem);
        assertTrue("task in the queue should be AddToIndexTaskLocator", queuedItem instanceof AddToIndexTaskLocator);
        assertEquals("queued item's mailbox ID is different from test mailbox ID", mbox.getId(),
                queuedItem.getMailboxID());
        assertEquals("queued item's ID is different from test item's ID", item.getId(),
                ((AddToIndexTaskLocator) queuedItem).getMailItemsToAdd().get(0).getId());
        assertEquals("queued item's type is different from test item's type", item.getType(),
                ((AddToIndexTaskLocator) queuedItem).getMailItemsToAdd().get(0).getType());

        // pop the message
        AbstractIndexingTasksLocator nextItem = adapter.take();
        assertNotNull("empty dequeued item", nextItem);
        assertTrue("task taken from queue should be AddToIndexTaskLocator", queuedItem instanceof AddToIndexTaskLocator);
        assertEquals("dequeued item's mailbox ID is different from test mailbox ID", mbox.getId(),
                nextItem.getMailboxID());
        assertEquals("dequeued item's ID is different from test item's ID", item.getId(),
                ((AddToIndexTaskLocator) queuedItem).getMailItemsToAdd().get(0).getId());
        assertEquals("dequeued item's type is different from test item's type", item.getType(),
                ((AddToIndexTaskLocator) queuedItem).getMailItemsToAdd().get(0).getType());

        // verify that there are no more messages
        assertFalse("item queue should be empty", adapter.hasMoreItems());
    }

    @Test
    public void testPutTakeSingleDeleteTask() throws Exception {
        Assume.assumeTrue(isQueueSourceAvailable());
        // publish a message
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        Integer itemId = 1;
        adapter.put(new DeleteFromIndexTaskLocator(new ItemIndexDeletionInfo(itemId, 1, IndexType.MAILBOX), account.getId(), mbox.getId(), mbox.getSchemaGroupId()));

        // verify that message is in the queue
        assertTrue("item queue should not be empty", adapter.hasMoreItems());
        AbstractIndexingTasksLocator queuedItem = adapter.peek();
        assertNotNull("empty queued item", queuedItem);
        assertTrue("task in the queue should be DeleteFromIndexTaskLocator",
                queuedItem instanceof DeleteFromIndexTaskLocator);
        assertEquals("queued item's mailbox ID is different from test mailbox ID", mbox.getId(),
                queuedItem.getMailboxID());
        assertEquals("queued item's ID is different from test item's ID", itemId,
                ((DeleteFromIndexTaskLocator) queuedItem).getItemIds().get(0));

        // pop the message
        AbstractIndexingTasksLocator nextItem = adapter.take();
        assertNotNull("empty dequeued item", nextItem);
        assertTrue("task taken from queue should be DeleteFromIndexTaskLocator",
                queuedItem instanceof DeleteFromIndexTaskLocator);
        assertEquals("dequeued item's mailbox ID is different from test mailbox ID", mbox.getId(),
                nextItem.getMailboxID());
        assertEquals("dequeued item's ID is different from test item's ID", itemId,
                ((DeleteFromIndexTaskLocator) queuedItem).getItemIds().get(0));

        // verify that there are no more messages
        assertFalse("item queue should be empty", adapter.hasMoreItems());
    }

    @Test
    public void testPutTakeMultipleItemsForDelete() throws Exception {
        Assume.assumeTrue(isQueueSourceAvailable());
        // publish a message
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        Integer itemId1 = 1;
        Integer itemId2 = 2;
        ArrayList<ItemIndexDeletionInfo> itemIds = new ArrayList<ItemIndexDeletionInfo>();
        itemIds.add(new ItemIndexDeletionInfo(itemId1, 1, IndexType.MAILBOX));
        itemIds.add(new ItemIndexDeletionInfo(itemId2, 1, IndexType.MAILBOX));
        adapter.put(new DeleteFromIndexTaskLocator(itemIds, account.getId(), mbox.getId(), mbox.getSchemaGroupId()));

        // verify that message is in the queue
        assertTrue("item queue should not be empty", adapter.hasMoreItems());
        AbstractIndexingTasksLocator queuedItem = adapter.peek();
        assertNotNull("empty queued item", queuedItem);
        assertTrue("task in the queue should be DeleteFromIndexTaskLocator",
                queuedItem instanceof DeleteFromIndexTaskLocator);
        assertEquals("queued item's mailbox ID is different from test mailbox ID", mbox.getId(),
                queuedItem.getMailboxID());
        assertEquals("queued item has wrong number of item IDs", itemIds.size(),
                ((DeleteFromIndexTaskLocator) queuedItem).getItemIds().size());
        assertEquals("queued item's ID is different from test item's ID", itemId1,
                ((DeleteFromIndexTaskLocator) queuedItem).getItemIds().get(0));
        assertEquals("queued item's ID is different from test item's ID", itemId2,
                ((DeleteFromIndexTaskLocator) queuedItem).getItemIds().get(1));

        // pop the message
        AbstractIndexingTasksLocator nextItem = adapter.take();
        assertNotNull("empty dequeued item", nextItem);
        assertTrue("task taken from queue should be DeleteFromIndexTaskLocator",
                queuedItem instanceof DeleteFromIndexTaskLocator);
        assertEquals("dequeued item's mailbox ID is different from test mailbox ID", mbox.getId(),
                nextItem.getMailboxID());
        assertEquals("dequeued item has wrong number of item IDs", itemIds.size(),
                ((DeleteFromIndexTaskLocator) queuedItem).getItemIds().size());
        assertEquals("dequeued item's ID is different from test item's ID", itemId1,
                ((DeleteFromIndexTaskLocator) queuedItem).getItemIds().get(0));
        assertEquals("dequeued item's ID is different from test item's ID", itemId2,
                ((DeleteFromIndexTaskLocator) queuedItem).getItemIds().get(1));

        // verify that there are no more messages
        assertFalse("item queue should be empty", adapter.hasMoreItems());
    }

    @Test
    public void testPutTakeMultipleItemsForIndexing() throws Exception {
        Assume.assumeTrue(isQueueSourceAvailable());
        // publish a message
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        UnderlyingData underlyingData1 = new UnderlyingData();
        underlyingData1.id = 100;
        underlyingData1.setSubject("test subject 1");
        underlyingData1.folderId = Mailbox.ID_FOLDER_INBOX;
        underlyingData1.name = "name 1";
        underlyingData1.type = MailItem.Type.MESSAGE.toByte();
        underlyingData1.uuid = "some UUID 1";
        underlyingData1.setBlobDigest("test digest 1");
        underlyingData1.setFlags(Flag.BITMASK_UNCACHED | Flag.BITMASK_IN_DUMPSTER);
        MailItem item1 = new MockMailItem(mbox, underlyingData1);
        assertTrue(item1.inDumpster());

        UnderlyingData underlyingData2 = new UnderlyingData();
        underlyingData2.id = 200;
        underlyingData2.setSubject("test subject 2");
        underlyingData2.folderId = Mailbox.ID_FOLDER_INBOX;
        underlyingData2.name = "name 2";
        underlyingData2.type = MailItem.Type.DOCUMENT.toByte();
        underlyingData2.uuid = "some UUID 2";
        underlyingData2.setBlobDigest("test digest 2");
        underlyingData2.setFlags(Flag.BITMASK_UNCACHED);
        MailItem item2 = new MockMailItem(mbox, underlyingData2);
        assertFalse(item2.inDumpster());

        List<MailItem> items = new ArrayList<MailItem>();
        items.add(item1);
        items.add(item2);
        adapter.put(new AddToIndexTaskLocator(items, account.getId(), mbox.getId(), mbox.getSchemaGroupId(), false));

        // verify that message is in the queue
        assertTrue("item queue should not be empty", adapter.hasMoreItems());
        AbstractIndexingTasksLocator queuedItem = adapter.peek();
        assertNotNull("empty queued item", queuedItem);
        assertTrue("item in queue should be AddToIndexTaskLocator", queuedItem instanceof AddToIndexTaskLocator);
        assertEquals("queued item's mailbox ID is different from test mailbox ID", mbox.getId(),
                queuedItem.getMailboxID());
        assertEquals("queued item's first mail mail item ID is different from test item's ID", item1.getId(),
                ((AddToIndexTaskLocator) queuedItem).getMailItemsToAdd().get(0).getId());
        assertEquals("queued item's first mail item type is different from test item's type", item1.getType(),
                ((AddToIndexTaskLocator) queuedItem).getMailItemsToAdd().get(0).getType());
        assertEquals("queued item's second mail item ID is different from test item's ID", item2.getId(),
                ((AddToIndexTaskLocator) queuedItem).getMailItemsToAdd().get(1).getId());
        assertEquals("queued item's second mail item type is different from test item's type", item2.getType(),
                ((AddToIndexTaskLocator) queuedItem).getMailItemsToAdd().get(1).getType());
        assertTrue("queued item's first mail item dumpster flag should be set", ((AddToIndexTaskLocator) queuedItem)
                .getMailItemsToAdd().get(0).isInDumpster());
        assertFalse("queued item's second mail item dumpster flag should NOT be set",
                ((AddToIndexTaskLocator) queuedItem).getMailItemsToAdd().get(1).isInDumpster());

        // pop the message
        AbstractIndexingTasksLocator nextItem = adapter.take();
        assertNotNull("empty dequeued item", nextItem);
        assertTrue("item taken from queue should be AddToIndexTaskLocator", queuedItem instanceof AddToIndexTaskLocator);
        assertEquals("dequeued item's mailbox ID is different from test mailbox ID", mbox.getId(),
                nextItem.getMailboxID());
        assertEquals("dequeued item's first mail item ID is different from test item's ID", item1.getId(),
                ((AddToIndexTaskLocator) nextItem).getMailItemsToAdd().get(0).getId());
        assertEquals("dequeued item's first mail item type is different from test item's type", item1.getType(),
                ((AddToIndexTaskLocator) nextItem).getMailItemsToAdd().get(0).getType());
        assertEquals("dequeued item's second mail item ID is different from test item's ID", item2.getId(),
                ((AddToIndexTaskLocator) nextItem).getMailItemsToAdd().get(1).getId());
        assertEquals("dequeued item's second mail item type is different from test item's type", item2.getType(),
                ((AddToIndexTaskLocator) nextItem).getMailItemsToAdd().get(1).getType());
        assertTrue("dequeued item's first mail item dumpster flag should be set", ((AddToIndexTaskLocator) nextItem)
                .getMailItemsToAdd().get(0).isInDumpster());
        assertFalse("dequeued item's second mail item dumpster flag should NOT be set",
                ((AddToIndexTaskLocator) nextItem).getMailItemsToAdd().get(1).isInDumpster());

        // verify that there are no more messages
        assertFalse("item queue should be empty", adapter.hasMoreItems());
    }

    @Test
    public void testMailboxTaskCount() throws Exception {
        Assume.assumeTrue(isQueueSourceAvailable());
        adapter.setTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID, 10);
        String anotherUUID = UUID.randomUUID().toString();
        adapter.setTotalMailboxTaskCount(anotherUUID, 101);

        assertEquals("wrong total number of tasks for the default account",
                adapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID), 10);

        assertEquals("wrong total number of tasks for the second account",
                adapter.getTotalMailboxTaskCount(anotherUUID), 101);

        assertEquals("should not have any tasks recorded for '7777-something-else-000-111'",
                adapter.getTotalMailboxTaskCount("7777-something-else-000-111"), 0);

        adapter.setTotalMailboxTaskCount(anotherUUID, -1);
        assertEquals("wrong total number of tasks for the second account after setting to -1",
                adapter.getTotalMailboxTaskCount(anotherUUID), -1);

        adapter.setSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID, 0);
        adapter.setSucceededMailboxTaskCount(anotherUUID, 11);

        assertEquals("completed task count for the default account should be zero", 0,
                adapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("completed task count for the second account should be 1", 11,
                adapter.getSucceededMailboxTaskCount(anotherUUID));

        adapter.incrementSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID, 2);
        assertEquals("completed task count for the default account after incrementing should be 2 ", 2,
                adapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        adapter.incrementSucceededMailboxTaskCount(anotherUUID, 1);
        assertEquals("completed task count for the second account after incrementing should be 12", 12,
                adapter.getSucceededMailboxTaskCount(anotherUUID));

        adapter.deleteMailboxTaskCounts(anotherUUID);
        assertEquals("wrong total number of tasks for the second account after resetting the counters",
                adapter.getTotalMailboxTaskCount(anotherUUID), 0);
        assertEquals("wrong number of completed tasks for the second account after resetting the counters",
                adapter.getSucceededMailboxTaskCount(anotherUUID), 0);
    }

    @Test
    public void testMailboxTaskStatus() throws Exception {
        Assume.assumeTrue(isQueueSourceAvailable());
        adapter.setTaskStatus(MockProvisioning.DEFAULT_ACCOUNT_ID, ReIndexStatus.STATUS_ABORTED);
        String anotherUUID = UUID.randomUUID().toString();
        adapter.setTaskStatus(anotherUUID, ReIndexStatus.STATUS_DONE);

        assertEquals("wrong task status for the default account", ReIndexStatus.STATUS_ABORTED,
                adapter.getTaskStatus(MockProvisioning.DEFAULT_ACCOUNT_ID));

        assertEquals("wrong task status for the second account", ReIndexStatus.STATUS_DONE,
                adapter.getTaskStatus(anotherUUID));

        assertEquals("wrong task status for for '7777-something-else-000-111'",
                adapter.getTaskStatus("7777-something-else-000-111"), ReIndexStatus.STATUS_IDLE);
    }

    protected abstract boolean isQueueSourceAvailable() throws Exception;
}
