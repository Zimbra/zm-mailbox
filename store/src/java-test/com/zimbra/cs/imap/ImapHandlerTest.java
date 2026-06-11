package com.zimbra.cs.imap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import com.zimbra.cs.mime.ParsedMessage;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.server.ServerThrottle;
import com.zimbra.cs.util.ZTestWatchman;
import com.zimbra.qa.unittest.TestUtil;

import junit.framework.Assert;

public class ImapHandlerTest {
    private static final String LOCAL_USER = "localimaptest@zimbra.com";

    @Rule public TestName testName = new TestName();
    @Rule public MethodRule watchman = new ZTestWatchman();
    
    @BeforeClass
    public static void init() throws Exception {
        LC.imap_use_ehcache.setDefault(false);
        MailboxTestUtil.initServer();
        String[] hosts = {"localhost", "127.0.0.1"};
        ServerThrottle.configureThrottle(new ImapConfig(false).getProtocol(), 100, 100, Arrays.asList(hosts), Arrays.asList(hosts));
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        Provisioning prov = Provisioning.getInstance();
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        attrs.put(Provisioning.A_zimbraFeatureAntispamEnabled , "true");
        prov.createAccount(LOCAL_USER, "secret", attrs);
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testDoCOPYByUID()  {

        try {
       Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
       acct.setFeatureAntispamEnabled(true);
       Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
       Message m1 =  TestUtil.addMessage(mbox, "Message 1");
       Message m2 =  TestUtil.addMessage(mbox, "Message 2");
       Message m3 =  TestUtil.addMessage(mbox, "Message 3");
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m1.getId()).getFolderId());
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m2.getId()).getFolderId());
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m3.getId()).getFolderId());
       ImapHandler handler = new MockImapHandler();
       ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
       ImapPath pathSpam = new MockImapPath(null,mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM), creds);
       ImapPath pathInbox = new MockImapPath(null,mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX), creds);
       handler.setCredentials(creds);
       byte params = 0;
       handler.setSelectedFolder(pathSpam, params);
       String sequenceSet = String.format("%d,%d,%d", m1.getId(), m2.getId(), m3.getId());
       Assert.assertTrue(handler.doCOPY(null, sequenceSet, pathInbox, true));
       List<Integer> newIds = TestUtil.search(mbox, "in:Inbox", MailItem.Type.MESSAGE);
       assertEquals(3, newIds.size());
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, newIds.get(0)).getFolderId());
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, newIds.get(1)).getFolderId());
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, newIds.get(2)).getFolderId());
       /* Note, messages may not be returned in the original order */
       Assert.assertTrue(String.format("message IDs should not have changed 1st ID=%s newIds=%s", m1.getId(), newIds),
               newIds.contains(m1.getId()));
       Assert.assertTrue(String.format("message IDs should not have changed 2nd ID=%s newIds=%s", m2.getId(), newIds),
               newIds.contains(m2.getId()));
       Assert.assertTrue(String.format("message IDs should not have changed 3rd ID=%s newIds=%s", m3.getId(), newIds),
               newIds.contains(m3.getId()));

       handler.setSelectedFolder(pathInbox, params);
       ImapFolder i4folder = handler.getSelectedFolder();
       Assert.assertEquals(3,i4folder.getSize());
       Assert.assertTrue(handler.doCOPY(null, sequenceSet, pathSpam, true));
       newIds = TestUtil.search(mbox, "in:junk", MailItem.Type.MESSAGE);
       assertEquals(3, newIds.size());
       Assert.assertFalse("Message IDs should have changed", newIds.contains(m1.getId()));
       Assert.assertFalse("Message IDs should have changed", newIds.contains(m3.getId()));
       Assert.assertFalse("Message IDs should have changed", newIds.contains(m3.getId()));

       Assert.assertEquals("Message should have been copied to Junk", Mailbox.ID_FOLDER_SPAM, mbox.getMessageById(null, newIds.get(0)).getFolderId());
       Assert.assertEquals("Message should have been copied to Junk", Mailbox.ID_FOLDER_SPAM, mbox.getMessageById(null, newIds.get(1)).getFolderId());
       Assert.assertEquals("Message should have been copied to Junk", Mailbox.ID_FOLDER_SPAM, mbox.getMessageById(null, newIds.get(2)).getFolderId());

       Assert.assertEquals("original messages should have stayed in inbox", Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m1.getId()).getFolderId());
       Assert.assertEquals("original messages should have stayed in inbox", Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m2.getId()).getFolderId());
       Assert.assertEquals("original messages should have stayed in inbox", Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m3.getId()).getFolderId());
        } catch (Exception e) {
            fail("No error should be thrown");
            e.printStackTrace();
        }
    }

    @Test
    public void testDoCOPYByNumber() throws Exception {
        
       Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
       acct.setFeatureAntispamEnabled(true);
       Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
       Message m1 =  TestUtil.addMessage(mbox, "Message 1");
       Message m2 =  TestUtil.addMessage(mbox, "Message 2");
       Message m3 =  TestUtil.addMessage(mbox, "Message 3");
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m1.getId()).getFolderId());
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m2.getId()).getFolderId());
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m3.getId()).getFolderId());
       ImapHandler handler = new MockImapHandler();
       ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
       ImapPath pathSpam = new MockImapPath(null,mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM), creds);
       ImapPath pathInbox = new MockImapPath(null,mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX), creds);
       handler.setCredentials(creds);
       byte params = 0;
       handler.setSelectedFolder(pathSpam, params);
       String sequenceSet = String.format("%d,%d,%d", m1.getId(), m2.getId(), m3.getId());
       boolean thrown = false;
       try {
           handler.doCOPY(null, sequenceSet, pathInbox, false);
       } catch (ImapParseException ex) {
           thrown = true;
       }
       Assert.assertTrue("Should have thrown 'Invalid Message Sequence Number'", thrown);
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m1.getId()).getFolderId());
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m2.getId()).getFolderId());
       Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m3.getId()).getFolderId());

       sequenceSet = "1:3";
       handler.setSelectedFolder(pathInbox, params);
       Assert.assertTrue(handler.doCOPY(null, sequenceSet, pathSpam, true));
       Assert.assertEquals("Original message should have stayed in Inbox", Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m1.getId()).getFolderId());
       Assert.assertEquals("Original message should have stayed in Inbox", Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m2.getId()).getFolderId());
       Assert.assertEquals("Original message should have stayed in Inbox", Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m3.getId()).getFolderId());
       List<Integer> newIds = TestUtil.search(mbox, "in:junk", MailItem.Type.MESSAGE);
       assertEquals("should not have copied anything to Junk with an invalid sequence set", 0, newIds.size());

       ImapFolder i4folder = handler.getSelectedFolder();
       Assert.assertEquals(3, i4folder.getSize());
       Assert.assertTrue(handler.doCOPY(null, sequenceSet, pathSpam, false));
       newIds = TestUtil.search(mbox, "in:junk", MailItem.Type.MESSAGE);
       Assert.assertFalse("Message IDs should have changed", newIds.contains(m1.getId()));
       Assert.assertFalse("Message IDs should have changed", newIds.contains(m3.getId()));
       Assert.assertFalse("Message IDs should have changed", newIds.contains(m3.getId()));
       Assert.assertEquals("Message should have been copied to Junk", Mailbox.ID_FOLDER_SPAM, mbox.getMessageById(null, newIds.get(0)).getFolderId());
       Assert.assertEquals("Message should have been copied to Junk", Mailbox.ID_FOLDER_SPAM, mbox.getMessageById(null, newIds.get(1)).getFolderId());
       Assert.assertEquals("Message should have been copied to Junk", Mailbox.ID_FOLDER_SPAM, mbox.getMessageById(null, newIds.get(2)).getFolderId());

       Assert.assertEquals("original messages should have stayed in inbox", Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m1.getId()).getFolderId());
       Assert.assertEquals("original messages should have stayed in inbox", Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m2.getId()).getFolderId());
       Assert.assertEquals("original messages should have stayed in inbox", Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m3.getId()).getFolderId());

    }

    @Test
    public void testDoMOVEByUID() {
        try {
            Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
            acct.setFeatureAntispamEnabled(true);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            Message m1 = TestUtil.addMessage(mbox, "Message 1");
            Message m2 = TestUtil.addMessage(mbox, "Message 2");
            Message m3 = TestUtil.addMessage(mbox, "Message 3");
            Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m1.getId()).getFolderId());
            Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m2.getId()).getFolderId());
            Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m3.getId()).getFolderId());
            ImapHandler handler = new MockImapHandler();
            ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
            ImapPath pathSpam = new MockImapPath(null, mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM), creds);
            ImapPath pathInbox = new MockImapPath(null, mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX), creds);
            handler.setCredentials(creds);
            byte params = 0;
            handler.setSelectedFolder(pathSpam, params);
            String sequenceSet = String.format("%d,%d,%d", m1.getId(), m2.getId(), m3.getId());
            Assert.assertTrue(handler.doMOVE(null, sequenceSet, pathInbox, true));
            List<Integer> newIds = TestUtil.search(mbox, "in:Inbox", MailItem.Type.MESSAGE);
            assertEquals(3, newIds.size());
            Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, newIds.get(0)).getFolderId());
            Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, newIds.get(1)).getFolderId());
            Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, newIds.get(2)).getFolderId());
            /* Note, messages may not be returned in the original order */
            Assert.assertTrue(
                    String.format("message IDs should not have changed 1st ID=%s newIds=%s", m1.getId(), newIds),
                    newIds.contains(m1.getId()));
            Assert.assertTrue(
                    String.format("message IDs should not have changed 2nd ID=%s newIds=%s", m2.getId(), newIds),
                    newIds.contains(m2.getId()));
            Assert.assertTrue(
                    String.format("message IDs should not have changed 3rd ID=%s newIds=%s", m3.getId(), newIds),
                    newIds.contains(m3.getId()));

            handler.setSelectedFolder(pathInbox, params);
            ImapFolder i4folder = handler.getSelectedFolder();
            Assert.assertEquals(3, i4folder.getSize());
            Assert.assertTrue(handler.doMOVE(null, sequenceSet, pathSpam, true));
            newIds = TestUtil.search(mbox, "in:junk", MailItem.Type.MESSAGE);
            assertEquals(3, newIds.size());
            //CHANGED: assertFalse -> assertTrue since imove keeps same IDs
            Assert.assertTrue("Message IDs should not have changed", newIds.contains(m1.getId()));
            Assert.assertTrue("Message IDs should not have changed", newIds.contains(m2.getId()));
            Assert.assertTrue("Message IDs should not have changed", newIds.contains(m3.getId()));

            Assert.assertEquals("Message should have been moved to Junk", Mailbox.ID_FOLDER_SPAM,
                    mbox.getMessageById(null, newIds.get(0)).getFolderId());
            Assert.assertEquals("Message should have been moved to Junk", Mailbox.ID_FOLDER_SPAM,
                    mbox.getMessageById(null, newIds.get(1)).getFolderId());
            Assert.assertEquals("Message should have been moved to Junk", Mailbox.ID_FOLDER_SPAM,
                    mbox.getMessageById(null, newIds.get(2)).getFolderId());

            //replaced folder ID check on original messages with verifying original message IDs are no longer in Inbox
            List<Integer> inboxIds = TestUtil.search(mbox, "in:Inbox", MailItem.Type.MESSAGE);
            Assert.assertFalse("original message should not be in Inbox anymore", inboxIds.contains(m1.getId()));
            Assert.assertFalse("original message should not be in Inbox anymore", inboxIds.contains(m2.getId()));
            Assert.assertFalse("original message should not be in Inbox anymore", inboxIds.contains(m3.getId()));
        } catch (Exception e) {
            fail("No error should be thrown");
            e.printStackTrace();
        }
    }

    // =====================================================================
    // T2: Special Folder Behaviors
    // Scenario A: unread message moved to Trash should be marked as read
    // Scenario B: message moved out of Spam should be re-indexed
    // =====================================================================
    @Test
    public void testDoMOVESpecialFolderBehaviors() {
        try {
            Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
            acct.setFeatureAntispamEnabled(true);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            ImapHandler handler = new MockImapHandler();
            ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
            ImapPath pathInbox = new MockImapPath(null, mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX), creds);
            ImapPath pathTrash = new MockImapPath(null, mbox.getFolderById(null, Mailbox.ID_FOLDER_TRASH), creds);
            ImapPath pathSpam = new MockImapPath(null, mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM), creds);
            handler.setCredentials(creds);
            byte params = 0;
            // Unread message moved to Trash should be marked as read ---
            // MailItem.imove: if (!inTrash() && target.inTrash()) { alterUnread(false); }
            Message m1 = TestUtil.addMessage(mbox, "Unread Message");
            // Verify message starts as unread
            Assert.assertTrue("Message should be unread initially", mbox.getMessageById(null, m1.getId()).isUnread());
            handler.setSelectedFolder(pathInbox, params);
            Assert.assertTrue(handler.doMOVE(null, String.format("%d", m1.getId()), pathTrash, true));
            // ID should not change
            List<Integer> trashIds = TestUtil.search(mbox, "in:Trash", MailItem.Type.MESSAGE);
            Assert.assertTrue("m1 ID should not change after move to Trash", trashIds.contains(m1.getId()));
            //Message should be in Trash and marked as read
            Assert.assertEquals("Message should be in Trash", Mailbox.ID_FOLDER_TRASH,
                    mbox.getMessageById(null, m1.getId()).getFolderId());
            Assert.assertFalse("Message should be marked as read when moved to Trash",
                    mbox.getMessageById(null, m1.getId()).isUnread());
            // Message should not be in Inbox anymore
            List<Integer> inboxIds = TestUtil.search(mbox, "in:Inbox", MailItem.Type.MESSAGE);
            Assert.assertFalse("m1 should not be in Inbox anymore", inboxIds.contains(m1.getId()));
            // Message moved out of Spam should be re-indexed ---
            Message m2 = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_SPAM, "Spam Message");
            // Verify message starts in Spam
            Assert.assertEquals("Message should start in Spam", Mailbox.ID_FOLDER_SPAM,
                    mbox.getMessageById(null, m2.getId()).getFolderId());
            handler.setSelectedFolder(pathSpam, params);
            Assert.assertTrue(handler.doMOVE(null, String.format("%d", m2.getId()), pathInbox, true));
            //ID should not change
            inboxIds = TestUtil.search(mbox, "in:Inbox", MailItem.Type.MESSAGE);
            Assert.assertTrue("m2 ID should not change after move out of Spam", inboxIds.contains(m2.getId()));
            //Message should be in Inbox and re-indexed
            Assert.assertEquals("Message should be in Inbox", Mailbox.ID_FOLDER_INBOX,
                    mbox.getMessageById(null, m2.getId()).getFolderId());
            // Re-indexing is async so we need to flush the index first
            mbox.index.indexDeferredItems();
            // Now verify message is re-indexed after moving out of Spam
            Assert.assertEquals("Message should be re-indexed after moving out of Spam", MailItem.IndexStatus.DONE,
                    mbox.getMessageById(null, m2.getId()).getIndexStatus());
            //Message should not be in Spam anymore
            List<Integer> spamIds = TestUtil.search(mbox, "in:junk", MailItem.Type.MESSAGE);
            Assert.assertFalse("m2 should not be in Spam anymore", spamIds.contains(m2.getId()));
        } catch (Exception e) {
            fail("No error should be thrown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ====================================
    // T3: Failure/Rejection Cases
    //  Expunged message with byUID=false rejects entire move
    //  Max items exceeded rejects move
    // =======================================
    @Test
    public void testDoMOVEFailureScenarios() {
        try {
            Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            ImapHandler handler = new MockImapHandler();
            ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
            ImapPath pathInbox = new MockImapPath(null, mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX), creds);
            ImapPath pathSpam = new MockImapPath(null, mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM), creds);
            handler.setCredentials(creds);
            byte params = 0;
            // Expunged message with byUID=false should reject entire move ---
            // when byUID=false, sequence numbers are used and expunged messages return null
            handler.setSelectedFolder(pathInbox, params);
            Message m1 = TestUtil.addMessage(mbox, "Message 1");
            Message m2 = TestUtil.addMessage(mbox, "Message 2");
            // Expunge m1 to simulate expunged message in sequence
            mbox.delete(null, m1.getId(), MailItem.Type.MESSAGE);
            // Try move with sequence number (byUID=false) including expunged position, entire move should be rejected because i4set contains null for expunged message
            String sequenceSet = "1:2";
            Assert.assertTrue(handler.doMOVE(null, sequenceSet, pathSpam, false));
            // m2 should still be in Inbox - entire move rejected due to expunged message
            Assert.assertEquals("m2 should still be in Inbox after rejection", Mailbox.ID_FOLDER_INBOX,
                    mbox.getMessageById(null, m2.getId()).getFolderId());
            // Spam should be empty - nothing moved
            List<Integer> spamIds = TestUtil.search(mbox, "in:junk", MailItem.Type.MESSAGE);
            Assert.assertEquals("Spam should be empty after rejection", 0, spamIds.size());
            //Max items exceeded should reject move ---
            // if (i4set.size() > LC.imap_max_items_in_move.intValue()) { sendNO(...); return true; }
            int maxItems = LC.imap_max_items_in_move.intValue();
            StringBuilder maxSequenceSet = new StringBuilder();
            List<Integer> msgIds = new ArrayList<>();
            for (int i = 0; i <= maxItems; i++) {
                Message m = TestUtil.addMessage(mbox, "Message " + i);
                msgIds.add(m.getId());
                if (maxSequenceSet.length() > 0)
                    maxSequenceSet.append(",");
                maxSequenceSet.append(m.getId());
            }
            // Move should be rejected since i4set.size() > LC.imap_max_items_in_move
            Assert.assertTrue(handler.doMOVE(null, maxSequenceSet.toString(), pathSpam, true));
            // All messages should still be in Inbox - nothing moved
            List<Integer> inboxIds = TestUtil.search(mbox, "in:Inbox", MailItem.Type.MESSAGE);
            for (int msgId : msgIds) {
                Assert.assertTrue("Message should still be in Inbox after max items rejection",
                        inboxIds.contains(msgId));
            }
            // Spam should still be empty
            spamIds = TestUtil.search(mbox, "in:junk", MailItem.Type.MESSAGE);
            Assert.assertEquals("Spam should be empty after max items rejection", 0, spamIds.size());
        } catch (Exception e) {
            fail("No error should be thrown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testDoMOVESameMailboxInboxToSpam() {
        try {
            Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
            acct.setFeatureAntispamEnabled(true);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            // Create two messages in same conversation using In-Reply-To threading
            String subject = "Same Subject";
            String msgStr1 = "From: sender@test.com\r\n" + "To: recipient@test.com\r\n" + "Subject: " + subject + "\r\n" + "Message-ID: <msg1@test.com>\r\n" + "\r\nTest body";
            String msgStr2 = "From: sender@test.com\r\n" + "To: recipient@test.com\r\n" + "Subject: Re: " + subject + "\r\n" + "Message-ID: <msg2@test.com>\r\n" + "In-Reply-To: <msg1@test.com>\r\n" + "References: <msg1@test.com>\r\n" + "\r\nTest reply body";
            long timestamp = System.currentTimeMillis();
            ParsedMessage pm1 = new ParsedMessage(msgStr1.getBytes(), timestamp, false);
            ParsedMessage pm2 = new ParsedMessage(msgStr2.getBytes(), timestamp, false);
            DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX)
                    .setFlags(Flag.BITMASK_UNREAD);
            Message m1 = mbox.addMessage(null, pm1, dopt, null);
            Message m2 = mbox.addMessage(null, pm2, dopt, null);
            // Verify both messages are in same conversation
            int conversationId = m1.getConversationId();
            Assert.assertEquals("Messages should be in same conversation", conversationId, m2.getConversationId());
            ImapHandler handler = new MockImapHandler();
            ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
            ImapPath pathInbox = new MockImapPath(null, mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX), creds);
            ImapPath pathSpam = new MockImapPath(null, mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM), creds);
            handler.setCredentials(creds);
            byte params = 0;
            handler.setSelectedFolder(pathInbox, params);
            // Capture state before move
            int oldUIDNEXT = mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM).getImapUIDNEXT();
            long inboxSizeBefore = mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX).getSize();
            long spamSizeBefore = mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM).getSize();
            int originalModSeq = m1.getModifiedSequence();
            // Move only m1 to Spam
            String sequenceSet = String.format("%d", m1.getId());
            Assert.assertTrue(handler.doMOVE(null, sequenceSet, pathSpam, true));
            //ID should NOT change
            // DbMailItem.imove does UPDATE SET folder_id WHERE id=? - id column never changes
            List<Integer> spamIds = TestUtil.search(mbox, "in:junk", MailItem.Type.MESSAGE);
            Assert.assertTrue("Message ID should not change in same mailbox move", spamIds.contains(m1.getId()));
            //Message should be in Spam
            Assert.assertEquals("Message should be in Spam", Mailbox.ID_FOLDER_SPAM,
                    mbox.getMessageById(null, m1.getId()).getFolderId());
            //m1 should not be in Inbox, m2 should still be in Inbox
            List<Integer> inboxIds = TestUtil.search(mbox, "in:Inbox", MailItem.Type.MESSAGE);
            Assert.assertFalse("m1 should not be in Inbox", inboxIds.contains(m1.getId()));
            Assert.assertTrue("m2 should still be in Inbox", inboxIds.contains(m2.getId()));
            // Verify UIDNEXT updated on target folder
            // iMove: if (resetUIDNEXT && oldUIDNEXT == target.getImapUIDNEXT()) target.updateUIDNEXT()
            int newUIDNEXT = mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM).getImapUIDNEXT();
            Assert.assertTrue("UIDNEXT should increase after move", newUIDNEXT > oldUIDNEXT);
            // Verify folder sizes updated
            Assert.assertTrue("Inbox size should decrease",
                    mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX).getSize() < inboxSizeBefore);
            Assert.assertTrue("Spam size should increase",
                    mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM).getSize() > spamSizeBefore);
            // Verify mod_metadata updated in DB
            //SET mod_metadata = ? WHERE id = ?
            Assert.assertTrue("mod_metadata should be updated after move",
                    mbox.getMessageById(null, m1.getId()).getModifiedSequence() > originalModSeq);
            // Verify m1 detached from conversation after moving to Spam
            Assert.assertFalse("m1 should be detached from original conversation after move to Spam",
                    conversationId == mbox.getMessageById(null, m1.getId()).getConversationId());
            // Verify m2 still in original conversation
            Assert.assertEquals("m2 should still be in original conversation", conversationId,
                    mbox.getMessageById(null, m2.getId()).getConversationId());
        } catch (Exception e) {
            fail("No error should be thrown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testDoSearch() throws Exception {
        
        Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        acct.setFeatureAntispamEnabled(true);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        Message m1 =  TestUtil.addMessage(mbox, "Message 1 blue");
        Message m2 =  TestUtil.addMessage(mbox, "Message 2 green red");
        Message m3 =  TestUtil.addMessage(mbox, "Message 3 green white");
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m1.getId()).getFolderId());
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m2.getId()).getFolderId());
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m3.getId()).getFolderId());

        Thread.sleep(500);
        ImapHandler handler = new MockImapHandler();
        ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        ImapPath pathInbox = new MockImapPath(null,mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX), creds);
        handler.setCredentials(creds);
        byte params = 0;
        handler.setSelectedFolder(pathInbox, params);
        Integer options = null;
        boolean byUID = false;
        ImapSearch.LogicalOperation i4srch = new ImapSearch.AndOperation();
        ImapSearch child = new ImapSearch.AndOperation(new ImapSearch.FlagSearch("\\Recent"),
                new ImapSearch.NotOperation(new ImapSearch.FlagSearch("\\Seen")));
        i4srch.addChild(child);
        i4srch.addChild(new ImapSearch.ContentSearch("green"));
        Assert.assertTrue(handler.doSEARCH("searchtag", i4srch, byUID, options));
        ByteArrayOutputStream baos = (ByteArrayOutputStream) handler.output;
        Assert.assertEquals("Output of SEARCH", "* SEARCH 2 3\r\nsearchtag OK SEARCH completed\r\n", baos.toString());
    }

    @Test
    public void testSearchInSearchFolder() throws Exception {
        
        Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        acct.setFeatureAntispamEnabled(true);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        Message m1 =  TestUtil.addMessage(mbox, "Message 1 blue");
        Message m2 =  TestUtil.addMessage(mbox, "Message 2 green red");
        Message m3 =  TestUtil.addMessage(mbox, "Message 3 green white");
        SearchFolder searchFolder = mbox.createSearchFolder(null, Mailbox.ID_FOLDER_USER_ROOT,
                "lookForGreen" /* name */, "green" /* query */, "message", "none", 0, (byte) 0);
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m1.getId()).getFolderId());
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m2.getId()).getFolderId());
        Assert.assertEquals(Mailbox.ID_FOLDER_INBOX, mbox.getMessageById(null, m3.getId()).getFolderId());

        ImapHandler handler = new MockImapHandler();
        ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        ImapPath pathSearchFldr = new MockImapPath(null, searchFolder, creds);
        handler.setCredentials(creds);
        byte params = 0;
        handler.setSelectedFolder(pathSearchFldr, params);
        Integer options = null;
        boolean byUID = false;
        ImapSearch.LogicalOperation i4srch = new ImapSearch.AndOperation();
        ImapSearch child = new ImapSearch.AndOperation(new ImapSearch.FlagSearch("\\Recent"),
                new ImapSearch.NotOperation(new ImapSearch.FlagSearch("\\Seen")));
        i4srch.addChild(child);
        i4srch.addChild(new ImapSearch.ContentSearch("white"));
        Assert.assertTrue(handler.doSEARCH("searchtag", i4srch, byUID, options));
        ByteArrayOutputStream baos = (ByteArrayOutputStream) handler.output;
        Assert.assertEquals("Output of SEARCH", "* SEARCH 2\r\nsearchtag OK SEARCH completed\r\n", baos.toString());
    }

    @Test
    public void testLogin() throws Exception {

        Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        ImapHandler handler = new MockImapHandler();

        acct.setImapEnabled(true);
        acct.setPrefImapEnabled(true);
        handler.setCredentials(null);
        Assert.assertTrue(handler.authenticate(LOCAL_USER, null, "secret", "logintag", null));
        Assert.assertTrue(handler.isAuthenticated());

        acct.setImapEnabled(true);
        acct.setPrefImapEnabled(false);
        handler.setCredentials(null);
        Assert.assertTrue(handler.authenticate(LOCAL_USER, null, "secret", "logintag", null));
        Assert.assertFalse(handler.isAuthenticated());

        acct.setImapEnabled(false);
        acct.setPrefImapEnabled(true);
        handler.setCredentials(null);
        Assert.assertTrue(handler.authenticate(LOCAL_USER, null, "secret", "logintag", null));
        Assert.assertFalse(handler.isAuthenticated());

        acct.setImapEnabled(false);
        acct.setPrefImapEnabled(false);
        handler.setCredentials(null);
        Assert.assertTrue(handler.authenticate(LOCAL_USER, null, "secret", "logintag", null));
        Assert.assertFalse(handler.isAuthenticated());
    }

    @Test
    public void testListFolderWithPlusSign() throws Exception {
        final String troubleName = "BOB+ALICE";
        final ImapHandler handler = new MockImapHandler();

        final Account acct = Provisioning.getInstance().getAccount("12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        final Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        final Folder folder = mbox.createFolder((OperationContext)null, troubleName, new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));

        final ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        handler.setCredentials(creds);

        ImapPath pathFolder = new MockImapPath(null, folder, creds);
        handler.setSelectedFolder(pathFolder, (byte)0);

        final String tag = ".";
        final String referenceName = "";
        final byte selectOptions = 0;
        final byte returnOptions = 0;
        final byte status = 0;

        final Set<String> mailboxNames = new HashSet<>();
        mailboxNames.add(troubleName);

        handler.doLIST(tag, referenceName, mailboxNames, selectOptions, returnOptions, status);

        final ByteArrayOutputStream baos = (ByteArrayOutputStream)handler.output;
        final String expected = String.format("* LIST (\\HasNoChildren) \"/\" \"%s\"\r\n%s OK LIST completed\r\n", troubleName, tag);
        Assert.assertEquals("Output of LIST", expected, baos.toString());
    }

    class MockImapPath extends ImapPath {

        MockImapPath(ImapPath other) {
            super(other);
            // TODO Auto-generated constructor stub
        }

        MockImapPath(String owner, FolderStore folderStore, ImapCredentials creds) throws ServiceException {
            super(owner, folderStore, creds);
        }

        @Override
        protected boolean isSelectable() {
            return true;
        }

        @Override
        protected boolean isWritable() {
            return true;
        }

        @Override
        protected boolean isWritable(short rights) throws ServiceException {
            return true;
        }
    }
}
