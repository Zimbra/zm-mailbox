package com.zimbra.cs.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.MockMailItem;
import com.zimbra.cs.mailbox.ReIndexStatus;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.qa.unittest.TestUtil;

/**
 * @author Greg Solovyev test various re-indexing scenarios (w/o SOAP) These
 *         tests use local (default) indexing queue and EmbeddedSolrServer
 */
public class TestReindex {
    private static int originalReindexBatchSize = 0;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());

        // use local queue for testing
        Provisioning.getInstance().getLocalServer()
                .setIndexingQueueProvider("com.zimbra.cs.index.DefaultIndexingQueueAdapter");

        // save pre-test values
        originalReindexBatchSize = Provisioning.getInstance().getLocalServer().getReindexBatchSize();

    }

    @Before
    public void setUp() throws Exception {
        cleanup();
        // indexing service should not be running at the beginning of these
        // tests
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
    }

    private void cleanup() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(originalReindexBatchSize);
        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
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
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
    }

    @Test
    public void testReIndexAllMessages() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add 4 messages
        create4Messages();

        MailboxTestUtil.waitForIndexing(mbox);
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(4, ids.size());

        // delete the index and verfy that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("Search should not return anything at this point", 0, ids.size());

        mbox.index.startReIndex();

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point total count should be 4", 4,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        assertEquals("at this point should have succeeded re-indexing 4 items", 4,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should be able to find 4 messages in the mailbox after re-indexing", 4, ids.size());
    }

    @Test
    public void testReIndexContacts() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add some messages
        create4Messages();

        MailboxTestUtil.waitForIndexing(mbox);
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("Search should find 3 messages at this point", 4, ids.size());

        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);
        Assert.assertTrue(mbox.index.existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"),
                new InternetAddress("Test <test2@zimbra.com>"))));

        // delete the index and verfy that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("Search should not return anything at this point", 0, ids.size());
        Assert.assertFalse(mbox.index.existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"),
                new InternetAddress("Test <test2@zimbra.com>"))));

        // disable manual commit for re-indexing
        // kick off re-indexing of Contacts only
        mbox.index.startReIndexByType(EnumSet.of(MailItem.Type.CONTACT));

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point total count should be 1", 1,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        assertEquals("at this point total count should still be 1", 1,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 1", 1,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("Should not be able to find messages after re-indexing Contacts only", 0, ids.size());

        Assert.assertTrue("should be able to find the contact after re-indexing Contacts only", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
    }

    @Test
    public void testReIndexMessages() throws Exception {

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add 4 messages
        create4Messages();

        MailboxTestUtil.waitForIndexing(mbox);

        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 4 messsages", 4, ids.size());

        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);

        Assert.assertTrue("should find the new contact", mbox.index.existsInContacts(ImmutableList.of(
                new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress("Test <test2@zimbra.com>"))));

        // delete the index and verify that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should not be able to find any messages at this point", 0, ids.size());
        Assert.assertFalse("should not be able to find contacts at this point", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));

        // disable manual commit for re-indexing
        // Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        mbox.index.startReIndexByType(EnumSet.of(MailItem.Type.MESSAGE));

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point succeeded count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'total' count should be 4", 4,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        // check results
        assertEquals("at this point total count should be  4", 4,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 4", 4,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should find 4 messages after re-indexing Messages only", 4, ids.size());
        Assert.assertFalse("should not be able to find contacts after re-indexing Messages only", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
    }

    @Test
    public void testReIndexAppointments() throws Exception {

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add some messages
        create4Messages();

        // now add some contacts
        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);

        // now add an appointment
        createCalendarItem("Shall we dance?",
                "We need a witness to our lives. There's a billion people on the planet...", "Hollywood");

        MailboxTestUtil.waitForIndexing(mbox);

        // check that new content is indexed
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 4 messsages", 4, ids.size());
        Assert.assertTrue("should find the new contact", mbox.index.existsInContacts(ImmutableList.of(
                new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress("Test <test2@zimbra.com>"))));

        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should find 1 appointment", 1, ids.size());

        // delete the index and verify that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should not be able to find any messages at this point", 0, ids.size());
        Assert.assertFalse("should not be able to find contacts at this point", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should not be able to find an appointment at this point", 0, ids.size());

        // disable manual commit for re-indexing
        // Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        mbox.index.startReIndexByType(EnumSet.of(MailItem.Type.APPOINTMENT));

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point total count should be 1", 1,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        // check search results
        assertEquals("at this point succeeded count should be 1", 1,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point total count should still be 1", 1,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should not be able to find messages after re-indexing appointments only", 0, ids.size());
        Assert.assertFalse("should not be able to find contacts after re-indexing appointments only", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        assertEquals("should find the appointment after re-indexing appointments only", 1, ids.size());
    }

    @Test
    public void testReIndexAll() throws Exception {

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add some messages
        create4Messages();

        // now add some contacts
        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);

        // now add an appointment
        createCalendarItem("Shall we dance?",
                "We need a witness to our lives. There's a billion people on the planet...", "Hollywood");

        MailboxTestUtil.waitForIndexing(mbox);

        // check that new content is indexed
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 4 messsages", 4, ids.size());
        Assert.assertTrue("should find the new contact", mbox.index.existsInContacts(ImmutableList.of(
                new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress("Test <test2@zimbra.com>"))));

        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should find 1 appointment", 1, ids.size());

        // delete the index and verify that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should not be able to find any messages at this point", 0, ids.size());
        Assert.assertFalse("should not be able to find contacts at this point", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should not be able to find an appointment at this point", 0, ids.size());

        // disable manual commit for re-indexing
        // Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        mbox.index.startReIndex();

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point total count should be 6", 6,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        // check search results
        assertEquals("at this point total count should be 6", 6,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 6", 6,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should find messages after re-indexing everything", 4, ids.size());
        Assert.assertTrue("should find contacts after re-indexing everything", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        assertEquals("should find the appointment after re-indexingeverything", 1, ids.size());
    }

    @Test
    public void testReIndexAllIn1Batch() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(6);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add some messages
        create4Messages();

        // now add some contacts
        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);

        // now add an appointment
        createCalendarItem("Shall we dance?",
                "We need a witness to our lives. There's a billion people on the planet...", "Hollywood");

        MailboxTestUtil.waitForIndexing(mbox);

        // check that new content is indexed
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 4 messsages", 4, ids.size());
        Assert.assertTrue("should find the new contact", mbox.index.existsInContacts(ImmutableList.of(
                new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress("Test <test2@zimbra.com>"))));

        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should find 1 appointment", 1, ids.size());

        // delete the index and verify that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should not be able to find any messages at this point", 0, ids.size());
        Assert.assertFalse("should not be able to find contacts at this point", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should not be able to find an appointment at this point", 0, ids.size());

        // disable manual commit for re-indexing
        // Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        mbox.index.startReIndex();

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point total count should be 6", 6,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        // check search results
        assertEquals("at this point total count should be 6", 6,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 6", 6,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should find messages after re-indexing everything", 4, ids.size());
        Assert.assertTrue("should find contacts after re-indexing everything", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        assertEquals("should find the appointment after re-indexingeverything", 1, ids.size());
    }

    @Test
    public void testReIndexAllIn1Batch2() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(10);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add some messages
        create4Messages();

        // now add some contacts
        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);

        // now add an appointment
        createCalendarItem("Shall we dance?",
                "We need a witness to our lives. There's a billion people on the planet...", "Hollywood");

        MailboxTestUtil.waitForIndexing(mbox);

        // check that new content is indexed
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 4 messsages", 4, ids.size());
        Assert.assertTrue("should find the new contact", mbox.index.existsInContacts(ImmutableList.of(
                new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress("Test <test2@zimbra.com>"))));

        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should find 1 appointment", 1, ids.size());

        // delete the index and verify that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should not be able to find any messages at this point", 0, ids.size());
        Assert.assertFalse("should not be able to find contacts at this point", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should not be able to find an appointment at this point", 0, ids.size());

        // disable manual commit for re-indexing
        // Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        mbox.index.startReIndex();

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point total count should be 6", 6,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        // check search results
        assertEquals("at this point total count should be 6", 6,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 6", 6,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should find messages after re-indexing everything", 4, ids.size());
        Assert.assertTrue("should find contacts after re-indexing everything", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        assertEquals("should find the appointment after re-indexingeverything", 1, ids.size());
    }

    @Test
    public void testReIndexAllIn2Batches() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(3);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add some messages
        create4Messages();

        // now add some contacts
        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);

        // now add an appointment
        createCalendarItem("Shall we dance?",
                "We need a witness to our lives. There's a billion people on the planet...", "Hollywood");

        MailboxTestUtil.waitForIndexing(mbox);

        // check that new content is indexed
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 4 messsages", 4, ids.size());
        Assert.assertTrue("should find the new contact", mbox.index.existsInContacts(ImmutableList.of(
                new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress("Test <test2@zimbra.com>"))));

        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should find 1 appointment", 1, ids.size());

        // delete the index and verify that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should not be able to find any messages at this point", 0, ids.size());
        Assert.assertFalse("should not be able to find contacts at this point", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should not be able to find an appointment at this point", 0, ids.size());

        // disable manual commit for re-indexing
        // Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        mbox.index.startReIndex();

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point total count should be 6", 6,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        // check search results
        assertEquals("at this point total count should be 6", 6,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 6", 6,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should find messages after re-indexing everything", 4, ids.size());
        Assert.assertTrue("should find contacts after re-indexing everything", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        assertEquals("should find the appointment after re-indexingeverything", 1, ids.size());
    }

    @Test
    public void testReIndexAllIn3Batches() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(3);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add some messages
        create4Messages();

        // now add some contacts
        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "david.gilmour@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);
        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "waters@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);

        // add appointments
        createCalendarItem("Shall we dance?",
                "We need a witness to our lives. There's a billion people on the planet...", "Hollywood");

        MailboxTestUtil.waitForIndexing(mbox);

        // check that new content is indexed
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 4 messsages", 4, ids.size());
        Assert.assertTrue("should find the new contact", mbox.index.existsInContacts(ImmutableList.of(
                new InternetAddress("David <david.gilmour@zimbra.com>"), new InternetAddress(
                        "Roger <waters@zimbra.com>"))));

        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should find 1 appointment", 1, ids.size());

        // delete the index and verify that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should not be able to find any messages at this point", 0, ids.size());
        Assert.assertFalse("should not be able to find contacts at this point", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should not be able to find an appointment at this point", 0, ids.size());

        mbox.index.startReIndex();

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point total count should be 7", 7,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        // check search results
        assertEquals("at this point total count should be 7", 7,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 7", 7,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should find messages after re-indexing everything", 4, ids.size());
        Assert.assertTrue("should find the new contact", mbox.index.existsInContacts(ImmutableList.of(
                new InternetAddress("David <david.gilmour@zimbra.com>"), new InternetAddress(
                        "Roger <waters@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        assertEquals("should find the appointment after re-indexing everything", 1, ids.size());
    }

    @Test
    public void testQueueFull() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(2);
        int queueMaxSize = Provisioning.getInstance().getLocalServer().getIndexingQueueMaxSize();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add some messages
        create4Messages();

        // now add some contacts
        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "david.gilmour@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);
        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "waters@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);

        // add appointments
        createCalendarItem("Shall we dance?",
                "We need a witness to our lives. There's a billion people on the planet...", "Hollywood");

        MailboxTestUtil.waitForIndexing(mbox);

        // check that new content is indexed
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 4 messsages", 4, ids.size());
        Assert.assertTrue("should find the new contact", mbox.index.existsInContacts(ImmutableList.of(
                new InternetAddress("David <david.gilmour@zimbra.com>"), new InternetAddress(
                        "Roger <waters@zimbra.com>"))));

        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should find 1 appointment", 1, ids.size());

        // delete the index and verify that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should not be able to find any messages at this point", 0, ids.size());
        Assert.assertFalse("should not be able to find contacts at this point", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should not be able to find an appointment at this point", 0, ids.size());

        // fill up the queue
        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point total count should be 0", 0,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        for (int i = 0; i < queueMaxSize; i++) {
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
            queueAdapter.add(new AddToIndexTaskLocator(items, MockProvisioning.DEFAULT_ACCOUNT_ID, mbox.getId(), mbox
                    .getSchemaGroupId(), false));
        }

        mbox.index.startReIndex();

        assertEquals("at this point total count should be 7", 7,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'failed' count should be 7", 7,
                queueAdapter.getFailedMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        // check search results
        assertEquals("at this point total count should still be 7", 7,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should still be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'failed' count should still be 7", 7,
                queueAdapter.getFailedMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should not find messages when re-index batch is 0", 0, ids.size());
        Assert.assertFalse("should not find contacts when re-index batch is 0", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        assertEquals("should not find the appointment when re-index batch is 0", 0, ids.size());
    }

    @Test
    public void testInvalidBatchSize() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(0);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add some messages
        create4Messages();

        // now add some contacts
        mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")),
                Mailbox.ID_FOLDER_CONTACTS, null);

        // now add an appointment
        createCalendarItem("Shall we dance?",
                "We need a witness to our lives. There's a billion people on the planet...", "Hollywood");

        MailboxTestUtil.waitForIndexing(mbox);

        // check that new content is indexed
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should find 4 messsages", 4, ids.size());
        Assert.assertTrue("should find the new contact", mbox.index.existsInContacts(ImmutableList.of(
                new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress("Test <test2@zimbra.com>"))));

        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should find 1 appointment", 1, ids.size());

        // delete the index and verify that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("should not be able to find any messages at this point", 0, ids.size());
        Assert.assertFalse("should not be able to find contacts at this point", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        Assert.assertEquals("should not be able to find an appointment at this point", 0, ids.size());

        // disable manual commit for re-indexing
        // Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        mbox.index.startReIndex();

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point total count should be 6", 6,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        try {
            MailboxTestUtil.waitForIndexing(mbox);
        } catch (ServiceException e) {
            // should time out
            assertTrue("should catch a timeout exception", e.getMessage().indexOf("taking longer") > 0);
        }

        // check search results
        assertEquals("at this point total count should be 6", 6,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should not find messages when re-index batch is 0", 0, ids.size());
        Assert.assertFalse("should not find contacts when re-index batch is 0", mbox.index
                .existsInContacts(ImmutableList.of(new InternetAddress("Test <test1@zimbra.com>"), new InternetAddress(
                        "Test <test2@zimbra.com>"))));
        ids = TestUtil.search(mbox, "dance", MailItem.Type.APPOINTMENT);
        assertEquals("should not find the appointment when re-index batch is 0", 0, ids.size());
    }

    @Test
    public void testReIndexByID() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(10);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add some messages
        List<MailItem> messages = create4Messages();

        MailboxTestUtil.waitForIndexing(mbox);
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(4, ids.size());

        // delete the index and verfy that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("Search should not return anything at this point", 0, ids.size());

        Set<Integer> IDsToReIndex = new HashSet<Integer>();
        IDsToReIndex.add(messages.get(0).getId());
        IDsToReIndex.add(messages.get(2).getId());

        mbox.index.startReIndexById(IDsToReIndex);

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("at this point 'total' count should be 2", 2,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        assertEquals("at this point 'total' count should be 2", 2,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'succeeded' count should be 2", 2,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should be able to find 2 messages in the mailbox after re-indexing", 2, ids.size());
        assertTrue("should have found message 0", ids.contains(messages.get(0).getId()));
        assertTrue("should have found message 2", ids.contains(messages.get(2).getId()));
    }

    @Test
    public void testReindexStatus() throws Exception {
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(10);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add 4 messages
        create4Messages();

        MailboxTestUtil.waitForIndexing(mbox);
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(4, ids.size());

        // delete the index and verfy that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("Search should not return anything at this point", 0, ids.size());

        mbox.index.startReIndex();

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("'total' queue should have 4 items before re-index starts", 4,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("'succeeded' count should have 0 items before re-index starts", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ReIndexStatus status = mbox.index.getReIndexStatus();
        assertNotNull("ReIndexStatus should not be null", status);
        assertEquals("ReIndexStatus should contain 4 total items", 4, status.getTotal());
        assertEquals("ReIndexStatus should contain 0 processed items", 0, status.getSucceeded());
        assertFalse("ReIndexStatus should not be ABORTED", status.getStatus() == ReIndexStatus.STATUS_ABORTED);

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        status = mbox.index.getReIndexStatus();
        assertNotNull("ReIndexStatus should not be null", status);
        assertEquals("ReIndexStatus should contain 4 total items", 4, status.getTotal());
        assertEquals("ReIndexStatus should contain 4 processed items", 4, status.getSucceeded());
        assertFalse("ReIndexStatus should not be ABORTED", status.getStatus() == ReIndexStatus.STATUS_ABORTED);

        assertEquals("'succeeded' count should be 4 after re-index is finished", 4,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("'total' count should be 4 after re-index is finished", 4,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should be able to find 4 messages in the mailbox after re-indexing", 4, ids.size());
    }

    @Test
    public void abortReIndex() throws Exception {
        // Provisioning.getInstance().getLocalServer().setIndexManualCommit(false);
        Provisioning.getInstance().getLocalServer().setReindexBatchSize(10);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // add 4 messages
        create4Messages();

        MailboxTestUtil.waitForIndexing(mbox);
        List<Integer> ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals(4, ids.size());

        // delete the index and verfy that search returns nothing
        mbox.index.deleteIndex();
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        Assert.assertEquals("Search should not return anything at this point", 0, ids.size());

        mbox.index.startReIndex();

        IndexingQueueAdapter queueAdapter = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class);
        assertEquals("'total' queue should have 4 items before re-index starts", 4,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("'succeeded' count should have 0 items before re-index starts", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));

        ReIndexStatus status = mbox.index.getReIndexStatus();
        assertNotNull("ReIndexStatus should not be null", status);
        assertEquals("ReIndexStatus should contain 4 total items", 4, status.getTotal());
        assertEquals("ReIndexStatus should contain 0 processed items", 0, status.getSucceeded());
        assertFalse("ReIndexStatus should not be ABORTED", status.getStatus() == ReIndexStatus.STATUS_ABORTED);

        mbox.index.abortReIndex();

        status = mbox.index.getReIndexStatus();
        assertNotNull("ReIndexStatus should not be null", status);
        assertEquals("ReIndexStatus should contain 4 total items", 4, status.getTotal());
        assertEquals("ReIndexStatus should contain 0 processed items", 0, status.getSucceeded());
        assertTrue("ReIndexStatus should be ABORTED", status.getStatus() == ReIndexStatus.STATUS_ABORTED);

        // start indexing service
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();

        MailboxTestUtil.waitForIndexing(mbox);

        status = mbox.index.getReIndexStatus();
        assertNotNull("ReIndexStatus should not be null", status);
        assertEquals("ReIndexStatus should contain 4 total items", 4, status.getTotal());
        assertEquals("ReIndexStatus should contain 0 processed items", 0, status.getSucceeded());
        assertEquals("ReIndexStatus should be ABORTED", ReIndexStatus.STATUS_ABORTED, status.getStatus());

        assertEquals("at this point 'succeeded' count should be 0", 0,
                queueAdapter.getSucceededMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        assertEquals("at this point 'total' count should be 4", 4,
                queueAdapter.getTotalMailboxTaskCount(MockProvisioning.DEFAULT_ACCOUNT_ID));
        ids = TestUtil.search(mbox, "from:greg", MailItem.Type.MESSAGE);
        assertEquals("should not be able to find messages in the mailbox after cancelled re-indexing", 0, ids.size());
    }

    private void createCalendarItem(String subject, String fragment, String location) throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        ZVCalendar calendar = new ZVCalendar();
        calendar.addDescription("my new calendar", null);
        ZComponent comp = new ZComponent("VEVENT");
        calendar.addComponent(comp);
        Invite invite = MailboxTestUtil.generateInvite(mbox.getAccount(), fragment, calendar);
        invite.setUid(new UUID(10L, 1L).toString());
        invite.setSentByMe(true);
        invite.setName(subject);
        invite.setDescription(fragment, fragment);
        invite.setLocation(location);
        mbox.addInvite(null, invite, Mailbox.ID_FOLDER_CALENDAR);
    }

    private List<MailItem> create4Messages() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        List<MailItem> mailItems = new ArrayList<MailItem>();
        mailItems.add(mbox.addMessage(
                null,
                new ParsedMessage(
                        "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Shall I compare thee to a summer's day"
                                .getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(
                null,
                new ParsedMessage(
                        "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Thou art more lovely and more temperate"
                                .getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(
                null,
                new ParsedMessage(
                        "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: Rough winds do shake the darling buds of May"
                                .getBytes(), false), dopt, null));

        mailItems.add(mbox.addMessage(
                null,
                new ParsedMessage(
                        "From: greg@zimbra.com\r\nTo: test@zimbra.com\r\nSubject: And summer's lease hath all too short a date"
                                .getBytes(), false), dopt, null));

        return mailItems;
    }
}
