package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZTag;
import com.zimbra.client.ZTag.Color;
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.imap.ImapServerListener;
import com.zimbra.cs.imap.ImapServerListenerPool;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.imap.Atom;
import com.zimbra.cs.mailclient.imap.BodyStructure;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.session.SomeAccountsWaitSet;
import com.zimbra.cs.session.WaitSetMgr;

public class TestRemoteImapNotifications {
    private static final String USER = "TestRemoteImapNotifications-user";
    private static final String PASS = "test123";
    private Account acc = null;
    private static Server imapServer = null;
    private ImapConnection connection;
    private static boolean mIMAPDisplayMailFoldersOnly;
    private static boolean saved_imap_always_use_remote_store;
    private static String[] saved_imap_servers = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        BasicConfigurator.configure();


        saved_imap_always_use_remote_store = LC.imap_always_use_remote_store.booleanValue();
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(true));

        imapServer = Provisioning.getInstance().getLocalServer();
        mIMAPDisplayMailFoldersOnly = imapServer.isImapDisplayMailFoldersOnly();
        imapServer.setImapDisplayMailFoldersOnly(false);


        //preserve settings
        saved_imap_servers = imapServer.getReverseProxyUpstreamImapServers();
        imapServer.setReverseProxyUpstreamImapServers(new String[] {});
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (imapServer != null) {
            imapServer.setReverseProxyUpstreamImapServers(saved_imap_servers);
            imapServer.setImapDisplayMailFoldersOnly(mIMAPDisplayMailFoldersOnly);
        }
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(saved_imap_always_use_remote_store));
    }

    @Before
    public void setUp() throws ServiceException, IOException, DocumentException, ConfigException  {
        sharedCleanup();
        acc = TestUtil.createAccount(USER);
        Provisioning.getInstance().setPassword(acc, PASS);
    }

    private void sharedCleanup() throws ServiceException {
        if(TestUtil.accountExists(USER)) {
            TestUtil.deleteAccount(USER);
        }
    }

    @After
    public void tearDown() throws ServiceException, DocumentException, ConfigException, IOException  {
        sharedCleanup();
    }

    private ImapConnection connect(Server server) throws IOException {
        ImapConfig config = new ImapConfig(server.getServiceHostname());
        config.setPort(server.getImapBindPort());
        config.setAuthenticationId(USER);
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection conn = new ImapConnection(config);
        conn.connect();
        return conn;
    }

    protected void runOp(ZMailbox mbox, MailboxOperation op) throws Exception {

        ImapServerListener listener = ImapServerListenerPool.getInstance().get(mbox);
        String wsID = listener.getWSId();
        SomeAccountsWaitSet ws = (SomeAccountsWaitSet)(WaitSetMgr.lookup(wsID));
        long lastSequence = ws.getCurrentSeqNo();
        op.run();
        boolean applied = false;
        int timeout = 6000;
        while(timeout > 0) {
            if(listener.getLastKnownSequenceNumber() > lastSequence) {
                applied = true;
                break;
            }
            timeout -= 500;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }
        assertTrue("operation not applied within 6 seconds", applied);
    }

    @Test
    public void testNotificationsActiveFolder() throws Exception {
        String folderName = "testNotificationsActiveFolder-folder";
        String subject1 = "testNotificationsActiveFolder-msg1";
        String subject2 = "testNotificationsActiveFolder-msg2";

        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject1, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 1", 1, mdMap.size());

        MailboxOperation addMessage = new MailboxOperation() {
            @Override
            void run() throws Exception {
                TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

            }
        };

        runOp(zmbox, addMessage);

        mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch 2", 2, mdMap.size());
    }

    @Test
    public void testNotificationsEmptyActiveFolder() throws Exception {
        String folderName = "testNotificationsEmptyActiveFolder-folder";
        String subject1 = "testNotificationsEmptyActiveFolder-msg";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName);

        MailboxOperation addMessage = new MailboxOperation() {
            @Override
            void run() throws Exception {
                TestUtil.addMessage(zmbox, subject1, folder.getId(), null);

            }
        };

        runOp(zmbox, addMessage);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 1", 1, mdMap.size());
    }

    @Test
    public void testNotificationsCachedFolder() throws Exception {
        String folderName1 = "testNotificationsCachedFolder-folder1";
        String folderName2 = "testNotificationsCachedFolder-folder2";

        String subject1 = "TestRemoteImapNotifications-testMessage1";
        String subject2 = "TestRemoteImapNotifications-testMessage2";

        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder1 = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        TestUtil.addMessage(zmbox, subject1, folder1.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 1, mdMap.size());

        connection.select(folderName2);

        MailboxOperation addMessage = new MailboxOperation() {
            @Override
            void run() throws Exception {
                TestUtil.addMessage(zmbox, subject2, folder1.getId(), null);

            }
        };

        runOp(zmbox, addMessage);

        connection.select(folderName1);
        mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch afer reselecting cached folder", 2, mdMap.size());
    }

    private void checkNilResponse(MessageData md) {
        Envelope envelope = md.getEnvelope();
        BodyStructure bs = md.getBodyStructure();
        assertNull(envelope.getSubject());
        assertEquals(0, bs.getSize());
    }

    @Test
    public void testDeleteMessageNotificationActiveFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String subject1 = "TestRemoteImapNotifications-testMessage1";
        String subject2 = "TestRemoteImapNotifications-testMessage2";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        String msgId = TestUtil.addMessage(zmbox, subject1, folder.getId(), null);
        TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 2, mdMap.size());

        MailboxOperation deleteMessage = new MailboxOperation() {
            @Override
            void run() throws Exception {
                zmbox.deleteMessage(msgId);
            }
        };

        runOp(zmbox, deleteMessage);

        mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 2", 2, mdMap.size());
        MessageData md = mdMap.get(1L);
        //verify that the deleted message has a NIL response
        checkNilResponse(md);
        //verify that the second message is correct
        md = mdMap.get(2L);
        assertEquals(subject2, md.getEnvelope().getSubject());

        connection.expunge();
        mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 3", 1, mdMap.size());
    }

    @Test
    public void testDeleteMessageNotificationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String folderName2 = "TestRemoteImapNotifications-folder2";
        String subject1 = "TestRemoteImapNotifications-testMessage1";
        String subject2 = "TestRemoteImapNotifications-testMessage2";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        String msgId = TestUtil.addMessage(zmbox, subject1, folder.getId(), null);
        TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 2, mdMap.size());

        connection.select(folderName2);

        MailboxOperation deleteMessage = new MailboxOperation() {
            @Override
            void run() throws Exception {
                zmbox.deleteMessage(msgId);
            }
        };

        runOp(zmbox, deleteMessage);

        connection.select(folderName1);
        mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        //expunged messages are not returned as NIL responses if folder is re-selected
        assertEquals("Size of map returned by fetch 2", 1, mdMap.size());
    }

    @Ignore("TODO - support for delete tag notifications")
    @Test
    public void testDeleteTagNotificationActiveFolder() throws Exception {
        String folderName = "TestRemoteImapNotifications-folder";
        String tagName = "TestRemoteImapNotifications-tag";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getId(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect(imapServer);
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName);

        Flags flags = info.getPermanentFlags();
        assertTrue(flags.contains(new Atom(tagName)));

        MailboxOperation deleteTag = new MailboxOperation() {
            @Override
            void run() throws Exception {
                zmbox.deleteTag(tag.getId());
            }
        };

        runOp(zmbox, deleteTag);

        info = connection.select(folderName);
        flags = info.getPermanentFlags();
        assertFalse(flags.contains(new Atom(tagName)));
    }

    @Ignore("TODO - support for delete tag notifications")
    @Test
    public void testDeleteTagNotificationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String folderName2 ="TestRemoteImapNotifications-folder2";
        String tagName = "TestRemoteImapNotifications-tag";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getId(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect(imapServer);
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName1);

        Flags flags = info.getPermanentFlags();
        assertTrue(flags.contains(new Atom(tagName)));

        connection.select(folderName2);

        MailboxOperation deleteTag = new MailboxOperation() {
            @Override
            void run() throws Exception {
                zmbox.deleteTag(tag.getId());
            }
        };

        runOp(zmbox, deleteTag);

        info = connection.select(folderName1);
        flags = info.getPermanentFlags();
        assertFalse(flags.contains(new Atom(tagName)));
    }

    @Test
    public void testDeleteFolderNotificationActiveFolder() throws Exception {
        String folderName = "TestRemoteImapNotifications-folder";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 1, mdMap.size());

        MailboxOperation deleteFolder = new MailboxOperation() {
            @Override
            void run() throws Exception {
                zmbox.deleteFolder(folder.getId());
            }
        };

        runOp(zmbox, deleteFolder);

        try {
            connection.fetch("1:*", "(ENVELOPE BODY)");
            fail("should not be able to connect; connection should be closed");
        } catch (CommandFailedException e) {}
    }

    @Test
    public void testDeleteFolderNotificationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String folderName2 = "TestRemoteImapNotifications-folder2";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 1, mdMap.size());

        connection.select(folderName2);

        MailboxOperation deleteFolder = new MailboxOperation() {
            @Override
            void run() throws Exception {
                zmbox.deleteFolder(folder.getId());
            }
        };

        runOp(zmbox, deleteFolder);

        try {
            connection.list("", "*");
        } catch (CommandFailedException e) {
            fail("should be able to connect after deleting a cached folder");
        }
    }

    @Test
    public void testRenameTagNotificationActiveFolder() throws Exception {
        String folderName = "TestRemoteImapNotifications-folder";
        String tagName = "TestRemoteImapNotifications-tag";
        String newTagName = "TestRemoteImapNotifications-tag2";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getId(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect(imapServer);
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName);

        Flags flags = info.getPermanentFlags();
        assertTrue(flags.contains(new Atom(tagName)));

        MailboxOperation renameTag = new MailboxOperation() {
            @Override
            void run() throws Exception {
                zmbox.renameTag(tag.getId(), newTagName);
            }
        };

        runOp(zmbox, renameTag);

        info = connection.select(folderName);
        flags = info.getPermanentFlags();
        assertFalse(flags.contains(new Atom(tagName)));
        assertTrue(flags.contains(new Atom(newTagName)));
    }

    @Test
    public void testRenameTagNotificationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder1";
        String folderName2 = "TestRemoteImapNotifications-folder2";
        String tagName = "TestRemoteImapNotifications-tag";
        String newTagName = "TestRemoteImapNotifications-tag2";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getId(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect(imapServer);
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName1);

        Flags flags = info.getPermanentFlags();
        assertTrue(flags.contains(new Atom(tagName)));

        info = connection.select(folderName2);

        MailboxOperation renameTag = new MailboxOperation() {
            @Override
            void run() throws Exception {
                zmbox.renameTag(tag.getId(), newTagName);
            }
        };

        runOp(zmbox, renameTag);

        info = connection.select(folderName1);
        flags = info.getPermanentFlags();
        assertFalse(flags.contains(new Atom(tagName)));
        assertTrue(flags.contains(new Atom(newTagName)));
    }

    @Test
    public void testDeleteMessageBySubjectNotifications() throws Exception {
        String folderName = "testDeleteMessageBySubjectNotifications-folder";
        String subject1 = "testDeleteMessageBySubjectNotifications-msg1";
        String subject2 = "testDeleteMessageBySubjectNotifications-msg2";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject1, folder.getId(), null);
        TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 1", 2, mdMap.size());

        MailboxOperation deleteBySubject = new MailboxOperation() {
            @Override
            void run() throws Exception {
                TestUtil.deleteMessages(zmbox, "subject: " + subject2);
            }
        };

        runOp(zmbox, deleteBySubject);

        mdMap = connection.fetch("1:*", "(ENVELOPE)");
        mdMap.entrySet().removeIf(e ->  e.getValue().getEnvelope() == null || e.getValue().getEnvelope().getSubject() == null);

        assertEquals("Size of map returned by fetch 2", 1, mdMap.size());
    }


    @Test
    public void testModifyItemNotificationActiveFolder() throws Exception {
        String folderName = "testNotificationsActiveFolder-folder";
        String subject = "testNotificationsActiveFolder-msg1";
        String tagName = "testNotificationsActiveFolder-tag";

        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        String msgId = TestUtil.addMessage(zmbox, subject, folder.getId(), null);
        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch 1", 1, mdMap.size());
        //sanity check - make sure the tag is not already set on the message
        Flags flags = mdMap.get(1L).getFlags();
        assertFalse(flags.contains(new Atom(tag.getName())));

        MailboxOperation tagMessage = new MailboxOperation() {
            @Override
            void run() throws Exception {
                zmbox.tagMessage(msgId, tag.getId(), true);
            }
        };

        runOp(zmbox, tagMessage);

        mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch 2", 1, mdMap.size());
        flags = mdMap.get(1L).getFlags();
        assertTrue(flags.contains(new Atom(tag.getName())));
    }

    @Test
    public void testModifyItemNotificationCachedFolder() throws Exception {
        String folderName1 = "testNotificationsActiveFolder-folder1";
        String folderName2 = "testNotificationsActiveFolder-folder2";
        String subject = "testNotificationsActiveFolder-msg1";
        String tagName = "testNotificationsActiveFolder-tag";

        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        String msgId = TestUtil.addMessage(zmbox, subject, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch 1", 1, mdMap.size());
        //sanity check - make sure the tag is not already set on the message
        Flags flags = mdMap.get(1L).getFlags();
        assertFalse(flags.contains(new Atom(tag.getName())));
        connection.select(folderName2);

        MailboxOperation tagMessage = new MailboxOperation() {
            @Override
            void run() throws Exception {
                zmbox.tagMessage(msgId, tag.getId(), true);
            }
        };

        runOp(zmbox, tagMessage);

        connection.select(folderName1);
        mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch 2", 1, mdMap.size());
        flags = mdMap.get(1L).getFlags();
        assertTrue(flags.contains(new Atom(tag.getName())));
    }

    /**
     * This class encapsulates some mailbox operation run from an IMAP server.
     * It is used to test IMAP notifications via response headers and waitsets.
     */
    private static abstract class MailboxOperation {
        abstract void run() throws Exception;
    }

}
