package com.zimbra.cs.index.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;
import com.zimbra.cs.mailbox.MailboxIndex.ItemIndexDeletionInfo;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.qa.unittest.TestUtil;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class IndexingServiceTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());

        //use local queue for testing
        //Provisioning.getInstance().getLocalServer().setIndexingQueueProvider("com.zimbra.cs.index.LocalIndexingQueueAdapter");
    }

    @Before
    public void setUp() throws Exception {
        cleanup();
        //indexing service should not be running at the beginning of these tests
        IndexingService.getInstance().shutDown();
    }

    private void cleanup() throws Exception {
        IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        if(queueAdapter != null) {
            queueAdapter.drain();
            queueAdapter.clearAllTaskCounts();
        }
        MailboxTestUtil.clearData();
    }

    @After
    public void tearDown() throws Exception {
        cleanup();
    }

    @AfterClass
    public static void destroy() throws Exception {
        Account acc = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        acc.deleteAccount();
        IndexingService.getInstance().shutDown();
    }

    @Test
    public void testAsyncIndex() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(10);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);

        //the only real-life case when Zimbra is indexing multiple items via a shared queue is re-indexing. Here we will simulate this scenario
        List<MailItem> mailItems = new ArrayList<MailItem>();
        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Shall I compare thee to a summer's day".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Thou art more lovely and more temperate".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Rough winds do shake the darling buds of May".getBytes(), false), dopt, null));

        //MailboxTestUtil.waitForIndexing(mbox);
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(3, ids.size());

        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(0, ids.size());

        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
            assertTrue("MailboxIndex.add should return TRUE", mbox.index.queue(mailItems, false));
        }

        //start indexing service
        IndexingService.getInstance().startUp();

        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals(3, ids.size());
    }

    @Test
    public void testDeletedItem() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(10);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);

        //the only real-life case when Zimbra is indexing multiple items via a shared queue is re-indexing. Here we will simulate this scenario
        List<MailItem> mailItems = new ArrayList<MailItem>();
        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Shall I compare thee to a summer's day".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Thou art more lovely and more temperate".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Rough winds do shake the darling buds of May".getBytes(), false), dopt, null));

        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 3 items", 3, ids.size());

        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(0, ids.size());
        mbox.delete(null, mailItems.get(0).getId(), MailItem.Type.MESSAGE);

        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
            assertTrue("MailboxIndex.add should return TRUE", mbox.index.queue(mailItems, false));
        }

        //start indexing service
        IndexingService.getInstance().startUp();

        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals(2, ids.size());
    }

    @Test
    public void testInvalidItem() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(10);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);

        //the only real-life case when Zimbra is indexing multiple items via a shared queue is re-indexing. Here we will simulate this scenario
        List<MailItem> mailItems = new ArrayList<MailItem>();
        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Shall I compare thee to a summer's day".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Thou art more lovely and more temperate".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Rough winds do shake the darling buds of May".getBytes(), false), dopt, null));

        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 3 items", 3, ids.size());

        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(0, ids.size());
        mbox.delete(null, mailItems.get(0).getId(), MailItem.Type.MESSAGE);
        int [] deletedIds = {mailItems.get(0).getId()};
        mbox.deleteFromDumpster(null, deletedIds);

        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
            assertTrue("MailboxIndex.add should return TRUE", mbox.index.queue(mailItems, false));
        }

        //start indexing service
        IndexingService.getInstance().startUp();

        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals(2, ids.size());
    }

    @Test
    public void testAsyncDeleteAllFromIndex() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(10);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);

        //the only real-life case when Zimbra is indexing multiple items via a shared queue is re-indexing. Here we will simulate this scenario
        List<MailItem> mailItems = new ArrayList<MailItem>();
        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Shall I compare thee to a summer's day".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Thou art more lovely and more temperate".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Rough winds do shake the darling buds of May".getBytes(), false), dopt, null));

        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 3 items", 3, ids.size());

        //queue items for deletion from index
        IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        List<ItemIndexDeletionInfo> deletionInfos = new ArrayList<>(ids.size());
        for (Integer id: ids) {
            deletionInfos.add(new ItemIndexDeletionInfo(id, 1, IndexType.MAILBOX));
        }
        DeleteFromIndexTaskLocator itemLocator = new DeleteFromIndexTaskLocator(deletionInfos, mbox.getAccountId(), mbox.getId(), mbox.getSchemaGroupId());
        queueAdapter.put(itemLocator);

        //start indexing service
        IndexingService.getInstance().startUp();

        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should not be ale to find any items after deletion from indes", 0, ids.size());
    }

    @Test
    public void testAsyncDeleteOneFromIndex() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(10);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);

        //the only real-life case when Zimbra is indexing multiple items via a shared queue is re-indexing. Here we will simulate this scenario
        List<MailItem> mailItems = new ArrayList<MailItem>();
        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Shall I compare thee to a summer's day".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Thou art more lovely and more temperate".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Rough winds do shake the darling buds of May".getBytes(), false), dopt, null));

        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 3 items", 3, ids.size());

        //queue items for deletion from index
        IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        ItemIndexDeletionInfo itemDelete = new ItemIndexDeletionInfo(ids.get(0), 1, IndexType.MAILBOX);
        DeleteFromIndexTaskLocator itemLocator = new DeleteFromIndexTaskLocator(itemDelete, mbox.getAccountId(), mbox.getId(), mbox.getSchemaGroupId());
        queueAdapter.put(itemLocator);
        //start indexing service
        IndexingService.getInstance().startUp();

        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 2 items", 2, ids.size());
    }

    @Test
    public void testAsyncDeleteSomeFromIndex() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(10);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);

        //the only real-life case when Zimbra is indexing multiple items via a shared queue is re-indexing. Here we will simulate this scenario
        List<MailItem> mailItems = new ArrayList<MailItem>();
        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Shall I compare thee to a summer's day".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Thou art more lovely and more temperate".getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(null, new ParsedMessage(
                "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Rough winds do shake the darling buds of May".getBytes(), false), dopt, null));

        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 3 items", 3, ids.size());

        //queue items for deletion from index
        List<ItemIndexDeletionInfo> idsToDelete = Lists.newArrayList();
        idsToDelete.add(new ItemIndexDeletionInfo(ids.get(0), 1, IndexType.MAILBOX));
        idsToDelete.add(new ItemIndexDeletionInfo(ids.get(1), 1, IndexType.MAILBOX));
        IndexingQueueAdapter queueAdapter = IndexingQueueAdapter.getFactory().getAdapter();
        DeleteFromIndexTaskLocator itemLocator = new DeleteFromIndexTaskLocator(idsToDelete, mbox.getAccountId(), mbox.getId(), mbox.getSchemaGroupId());
        queueAdapter.put(itemLocator);

        //start indexing service
        IndexingService.getInstance().startUp();

        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 1 item", 1, ids.size());
    }
}
