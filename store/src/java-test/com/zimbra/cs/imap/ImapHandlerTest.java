package com.zimbra.cs.imap;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.server.ServerThrottle;
import com.zimbra.cs.service.util.ItemId;

public class ImapHandlerTest {
    private static final String LOCAL_USER = "localimaptest@zimbra.com";
    private Account acct = null;
    private Mailbox mbox = null;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        String[] hosts = {"localhost", "127.0.0.1"};
        ServerThrottle.configureThrottle(new ImapConfig(false).getProtocol(), 100, 100, Arrays.asList(hosts), Arrays.asList(hosts));
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f");
        acct = prov.createAccount(LOCAL_USER, "secret", attrs);
        mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        acct.setFeatureAntispamEnabled(true);
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testDoCOPYByUID() throws Exception {
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
       Assert.assertTrue("message IDs should not have changed", m1.getId() == newIds.get(0));
       Assert.assertTrue("message IDs should not have changed", m2.getId() == newIds.get(1));
       Assert.assertTrue("message IDs should not have changed", m3.getId() == newIds.get(2));
       
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
    }

    @Test
    public void testDoCOPYByNumber() throws Exception {
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

    class MockImapPath extends ImapPath {

        MockImapPath(ImapPath other) {
            super(other);
            // TODO Auto-generated constructor stub
        }
        
        MockImapPath(String owner, FolderStore folderStore, ImapCredentials creds) throws ServiceException {
            super(owner, folderStore, creds);
        }

        @Override
        boolean isSelectable() {
            return true;
        }

        @Override
        boolean isWritable() {
            return true;
        }

        @Override
        boolean isWritable(short rights) throws ServiceException {
            return true;
        }
    }
}