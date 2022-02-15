package com.zimbra.cs.imap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
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
