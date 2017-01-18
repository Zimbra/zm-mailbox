package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
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
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.MessageData;


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
        // TODO: See if sleep can be replaced with something better
        try { Thread.sleep(3000); } catch (Exception e) {}
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

        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName1);
        connection.select(folderName2);

        TestUtil.addMessage(zmbox, subject1, folder1.getId(), null);
        waitForWaitset();

        connection.select(folderName1);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(FLAGS)");
        assertEquals("Size of map returned by fetch afer reselecting cached folder", 1, mdMap.size());
    }

    private void waitForWaitset() {
        // TODO: See if sleep can be replaced with something better
        try { Thread.sleep(3 * Constants.MILLIS_PER_SECOND); } catch (Exception e) {}

    }
}
