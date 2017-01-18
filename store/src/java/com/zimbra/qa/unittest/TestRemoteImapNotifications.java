package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.log4j.BasicConfigurator;
import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZTag;
import com.zimbra.client.ZTag.Color;
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.imap.ImapRemoteSession;
import com.zimbra.cs.imap.ImapServerListener;
import com.zimbra.cs.imap.ImapServerListenerPool;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.imap.Atom;
import com.zimbra.cs.mailclient.imap.BodyStructure;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.session.PendingRemoteModifications;
import com.zimbra.soap.mail.type.DeleteItemNotification;
import com.zimbra.soap.mail.type.PendingFolderModifications;


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
    public static void afterClass() throws ServiceException {
        imapServer.setReverseProxyUpstreamImapServers(saved_imap_servers);
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
        if (imapServer != null) {
            imapServer.setImapDisplayMailFoldersOnly(mIMAPDisplayMailFoldersOnly);
        }
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(saved_imap_always_use_remote_store));
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

    @Test
    public void testNotificationsActiveFolder() throws IOException, ServiceException, MessagingException {
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

        TestUtil.addMessage(zmbox, subject2, folder.getId(), null);
        waitForWaitset();

        // TODO: See if sleep can be replaced with something better
        try { Thread.sleep(3000); } catch (Exception e) {}

        mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch 2", 2, mdMap.size());
    }

    @Test
    public void testNotificationsEmptyActiveFolder() throws IOException, ServiceException, MessagingException {
        String folderName = "testNotificationsEmptyActiveFolder-folder";
        String subject1 = "testNotificationsEmptyActiveFolder-msg";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName);
        TestUtil.addMessage(zmbox, subject1, folder.getId(), null);
        waitForWaitset();

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 1", 1, mdMap.size());
    }

    @Test
    public void testNotificationsCachedFolder() throws IOException, ServiceException, MessagingException {
        String folderName1 = "TestRemoteImapNotifications-folder1";
        String folderName2 = "TestRemoteImapNotifications-folder2";

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

        TestUtil.addMessage(zmbox, subject2, folder1.getId(), null);
        waitForWaitset();
        connection.select(folderName1);
        mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch afer reselecting cached folder", 2, mdMap.size());
    }

    @Test
    public void testNotificationsEmptyCachedFolder() throws IOException, ServiceException, MessagingException {
        String folderName1 = "testNotificationsEmptyCachedFolder-folder1";
        String folderName2 = "testNotificationsEmptyCachedFolder-folder2";

        String subject1 = "testNotificationsEmptyCachedFolder-msg2";

        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder1 = TestUtil.createFolder(zmbox, folderName1);
        TestUtil.createFolder(zmbox, folderName2);
    }

    @Test
    public void testSyntheticDeleteNotifation() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String subject1 = "TestRemoteImapNotifications-testMessage1";
        String subject2 = "TestRemoteImapNotifications-testMessage2";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        int folderId = folder.getFolderIdInOwnerMailbox();
        String msgId1 = TestUtil.addMessage(zmbox, subject1, folder.getId(), null);
        TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 2, mdMap.size());
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(zmbox);
        List<ImapRemoteSession> sessions = remoteListener.getListeners(acc.getId(), folderId);
        PendingFolderModifications folderMods = new PendingFolderModifications(folderId);
        folderMods.addDeletedItem(new DeleteItemNotification(Integer.valueOf(msgId1), MailItem.Type.MESSAGE.toString()));
        PendingRemoteModifications mods = PendingRemoteModifications.fromSOAP(folderMods, folderId, acc.getId());
        for (ImapRemoteSession session: sessions) {
            session.notifyPendingChanges(mods, 0, session);
        }
        mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 2", 2, mdMap.size());
        MessageData md = mdMap.get(1L);
        //this should be a NIL response
        Envelope envelope = md.getEnvelope();
        BodyStructure bs = md.getBodyStructure();
        assertNull(envelope.getSubject());
        assertEquals(0, bs.getSize());
        md = mdMap.get(2L);
        assertEquals(subject2, md.getEnvelope().getSubject());
        connection.expunge();
        mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 3", 1, mdMap.size());
    }

    @Test
    public void testDeleteNotification() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String folderName2 = "TestRemoteImapNotifications-folder2";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        String msgId = TestUtil.addMessage(zmbox, subject, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);
        connection.select(folderName2);

        TestUtil.addMessage(zmbox, subject, folder.getId(), null);
        waitForWaitset();

        zmbox.deleteMessage(msgId);
        connection.select(folderName1);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch afer reselecting cached folder", 1, mdMap.size());
    }

    private void checkNilResponse(MessageData md) {
        Envelope envelope = md.getEnvelope();
        BodyStructure bs = md.getBodyStructure();
        assertNull(envelope.getSubject());
        assertEquals(0, bs.getSize());
    }

    @Test
    public void testSyntheticDeleteMessageNotifationActiveFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String subject1 = "TestRemoteImapNotifications-testMessage1";
        String subject2 = "TestRemoteImapNotifications-testMessage2";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        int folderId = folder.getFolderIdInOwnerMailbox();
        String msgId1 = TestUtil.addMessage(zmbox, subject1, folder.getId(), null);
        TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 2, mdMap.size());
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(zmbox);
        List<ImapRemoteSession> sessions = remoteListener.getListeners(acc.getId(), folderId);
        PendingFolderModifications folderMods = new PendingFolderModifications(folderId);
        folderMods.addDeletedItem(new DeleteItemNotification(Integer.valueOf(msgId1), MailItem.Type.MESSAGE.toString()));
        PendingRemoteModifications mods = PendingRemoteModifications.fromSOAP(folderMods, folderId, acc.getId());
        for (ImapRemoteSession session: sessions) {
            session.notifyPendingChanges(mods, 0, session);
        }
        mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 2", 2, mdMap.size());
        MessageData md = mdMap.get(1L);
        checkNilResponse(md);
        //verify that the second message is correct
        md = mdMap.get(2L);
        assertEquals(subject2, md.getEnvelope().getSubject());

        connection.expunge();
        mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 3", 1, mdMap.size());
    }

    @Test
    public void testSyntheticDeleteMessageNotifationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String folderName2 = "TestRemoteImapNotifications-folder2";
        String subject1 = "TestRemoteImapNotifications-testMessage1";
        String subject2 = "TestRemoteImapNotifications-testMessage2";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        int folderId = folder.getFolderIdInOwnerMailbox();
        TestUtil.createFolder(zmbox, folderName2);
        String msgId1 = TestUtil.addMessage(zmbox, subject1, folder.getId(), null);
        TestUtil.addMessage(zmbox, subject2, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 2, mdMap.size());

        connection.select(folderName2);

        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(zmbox);
        List<ImapRemoteSession> sessions = remoteListener.getListeners(acc.getId(), folderId);
        PendingFolderModifications folderMods = new PendingFolderModifications(folderId);
        folderMods.addDeletedItem(new DeleteItemNotification(Integer.valueOf(msgId1), MailItem.Type.MESSAGE.toString()));
        PendingRemoteModifications mods = PendingRemoteModifications.fromSOAP(folderMods, folderId, acc.getId());
        for (ImapRemoteSession session: sessions) {
            session.notifyPendingChanges(mods, 0, session);
        }

        connection.select(folderName1);

        mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        //messages get expunged when modifications are replayed,
        //so we don't expect a NIL response for the deleted message here
        assertEquals("Size of map returned by fetch 2", 1, mdMap.size());
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
        assertEquals("Size of map returned by initial fetch", 1, mdMap.size());

        zmbox.deleteMessage(msgId);

        waitForWaitset();

        mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 2", 2, mdMap.size());
        MessageData md = mdMap.get(1L);
        checkNilResponse(md);
        //verify that the second message is correct
        md = mdMap.get(2L);
        assertEquals(subject2, md.getEnvelope().getSubject());

        connection.expunge();
        mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by fetch 3", 1, mdMap.size());
    }

    @Test
    public void testSyntheticDeleteTagNotificationActiveFolder() throws Exception {
        String folderName = "TestRemoteImapNotifications-folder";
        String tagName = "TestRemoteImapNotifications-tag";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        int folderId = folder.getFolderIdInOwnerMailbox();
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getId(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect(imapServer);
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName);

        Flags flags = info.getPermanentFlags();
        assertTrue(flags.contains(new Atom(tagName)));

        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(zmbox);
        List<ImapRemoteSession> sessions = remoteListener.getListeners(acc.getId(), folderId);
        PendingFolderModifications folderMods = new PendingFolderModifications(folderId);
        folderMods.addDeletedItem(new DeleteItemNotification(tag.getTagId(), MailItem.Type.TAG.toString()));
        PendingRemoteModifications mods = PendingRemoteModifications.fromSOAP(folderMods, folderId, acc.getId());
        for (ImapRemoteSession session: sessions) {
            session.notifyPendingChanges(mods, 0, session);
        }

        info = connection.select(folderName);
        flags = info.getPermanentFlags();
        assertFalse(flags.contains(new Atom(tagName)));
    }

    @Test
    public void testSyntheticDeleteTagNotificationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String folderName2 ="TestRemoteImapNotifications-folder2";
        String tagName = "TestRemoteImapNotifications-tag";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        int folderId = folder.getFolderIdInOwnerMailbox();
        TestUtil.createFolder(zmbox, folderName2);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getId(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect(imapServer);
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName1);

        Flags flags = info.getPermanentFlags();
        assertTrue(flags.contains(new Atom(tagName)));

        connection.select(folderName2);

        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(zmbox);
        List<ImapRemoteSession> sessions = remoteListener.getListeners(acc.getId(), folderId);
        PendingFolderModifications folderMods = new PendingFolderModifications(folderId);
        folderMods.addDeletedItem(new DeleteItemNotification(tag.getTagId(), MailItem.Type.TAG.toString()));
        PendingRemoteModifications mods = PendingRemoteModifications.fromSOAP(folderMods, folderId, acc.getId());
        for (ImapRemoteSession session: sessions) {
            session.notifyPendingChanges(mods, 0, session);
        }

        info = connection.select(folderName1);
        flags = info.getPermanentFlags();
        assertFalse(flags.contains(new Atom(tagName)));
    }

    @Test
    public void testDeleteTagNotificationActiveFolder() throws Exception {
        String folderName = "TestRemoteImapNotifications-folder";
        String tagName = "TestRemoteImapNotifications-tag";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        ZTag tag = zmbox.createTag(tagName, Color.blue);
        zmbox.addMessage(folder.getId(), null, tag.getName(), 0, TestUtil.getTestMessage(subject), true);

        connection = connect(imapServer);
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName);

        Flags flags = info.getPermanentFlags();
        assertTrue(flags.contains(new Atom(tagName)));

        zmbox.deleteTag(tag.getId());

        waitForWaitset();

        info = connection.select(folderName);
        flags = info.getPermanentFlags();
        assertFalse(flags.contains(new Atom(tagName)));
    }

    @Test
    public void testSyntheticDeleteFolderNotificationActiveFolder() throws Exception {
        String folderName = "TestRemoteImapNotifications-folder";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        int folderId = folder.getFolderIdInOwnerMailbox();
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 1, mdMap.size());

        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(zmbox);
        List<ImapRemoteSession> sessions = remoteListener.getListeners(acc.getId(), folderId);
        PendingFolderModifications folderMods = new PendingFolderModifications(folderId);
        folderMods.addDeletedItem(new DeleteItemNotification(folderId, MailItem.Type.FOLDER.toString()));
        PendingRemoteModifications mods = PendingRemoteModifications.fromSOAP(folderMods, folderId, acc.getId());
        for (ImapRemoteSession session: sessions) {
            session.notifyPendingChanges(mods, 0, session);
        }
        remoteListener.removeDeletedListeners(acc.getId(), folderId);
        try {
            connection.fetch("1:*", "(ENVELOPE BODY)");
            fail("should not be able to connect; connection should be closed");
        } catch (com.zimbra.cs.mailclient.CommandFailedException e) {}
    }

    @Test
    public void testSyntheticDeleteFolderNotificationCachedFolder() throws Exception {
        String folderName1 = "TestRemoteImapNotifications-folder";
        String folderName2 = "TestRemoteImapNotifications-folder2";
        String subject = "TestRemoteImapNotifications-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName1);
        int folderId = folder.getFolderIdInOwnerMailbox();
        TestUtil.createFolder(zmbox, folderName2);
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);

        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY)");
        assertEquals("Size of map returned by initial fetch", 1, mdMap.size());

        connection.select(folderName2);

        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(zmbox);
        List<ImapRemoteSession> sessions = remoteListener.getListeners(acc.getId(), folderId);
        PendingFolderModifications folderMods = new PendingFolderModifications(folderId);
        folderMods.addDeletedItem(new DeleteItemNotification(folderId, MailItem.Type.FOLDER.toString()));
        PendingRemoteModifications mods = PendingRemoteModifications.fromSOAP(folderMods, folderId, acc.getId());
        for (ImapRemoteSession session: sessions) {
            session.notifyPendingChanges(mods, 0, session);
        }
        remoteListener.removeDeletedListeners(acc.getId(), folderId);

        try {
            connection.list("", "*");
        } catch (CommandFailedException e) {
            fail("should be able to connect after deleting a cached folder");
        }
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

        zmbox.deleteFolder(folder.getId());
        connection.select(folderName);
    }

    private void waitForWaitset() {
        // TODO: See if sleep can be replaced with something better
        try { Thread.sleep(3 * Constants.MILLIS_PER_SECOND); } catch (Exception e) {}

    }
}
