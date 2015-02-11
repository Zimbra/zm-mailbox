package com.zimbra.cs.index;

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
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.qa.unittest.TestUtil;
public class TestIndexingService {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        
        //use local queue for testing
        Provisioning.getInstance().getLocalServer().setIndexingQueueProvider("com.zimbra.cs.index.DefaultIndexingQueueAdapter");
    }
    
    @Before
    public void setUp() throws Exception {
        cleanup();
        //indexing service should not be running at the beginning of these tests
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
    }

    private void cleanup() throws Exception {
        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        if(queueAdapter != null) {
            queueAdapter.deleteMailboxTaskCounts(MockProvisioning.DEFAULT_ACCOUNT_ID);
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
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);   
    }

    @Test
    public void testAsyncIndex() throws Exception {
        //Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
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
        
        MailboxTestUtil.waitForIndexing(mbox);
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(3, ids.size());
        
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(0, ids.size());
        
        mbox.lock.lock();
        assertTrue("MailboxIndex.add should return TRUE", mbox.index.add(mailItems));
        mbox.lock.release();
        
        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        queueAdapter.setTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID,3);
        
        //start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);
        
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals(3, ids.size());
    }
    
    @Test
    public void testDeletedItem() throws Exception {
        //Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
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
        
        MailboxTestUtil.waitForIndexing(mbox);
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(3, ids.size());
        
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(0, ids.size());
        mbox.delete(null, mailItems.get(0).getId(), MailItem.Type.MESSAGE);
        mbox.lock.lock();
        assertTrue("MailboxIndex.add should return TRUE", mbox.index.add(mailItems));
        mbox.lock.release();
        
        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        queueAdapter.setTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID,3);
        
        //start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);
        
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals(2, ids.size());
        assertEquals("'total' counter should be 3",3, queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("'succeeded' counter should be 3",3, queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("'failed' counter should be 0",0,queueAdapter.getFailedMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
    }
    
    @Test
    public void testInvalidItem() throws Exception {
        //Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
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
        
        MailboxTestUtil.waitForIndexing(mbox);
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(3, ids.size());
        
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(0, ids.size());
        mbox.delete(null, mailItems.get(0).getId(), MailItem.Type.MESSAGE);
        int [] deletedIds = {mailItems.get(0).getId()};
        mbox.deleteFromDumpster(null, deletedIds);
        mbox.lock.lock();
        assertTrue("MailboxIndex.add should return TRUE", mbox.index.add(mailItems));
        mbox.lock.release();
        
        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        queueAdapter.setTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID,3);
        
        //start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);
        
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals(2, ids.size());
        assertEquals("'total' counter should be 3",3, queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("'succeeded' counter should be 2",2, queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("'failed' counter should be 1",1,queueAdapter.getFailedMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
    }
}
