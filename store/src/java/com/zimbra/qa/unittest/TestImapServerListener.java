package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZTag;
import com.zimbra.client.ZTag.Color;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.imap.ImapConfig;
import com.zimbra.cs.imap.ImapCredentials;
import com.zimbra.cs.imap.ImapFolder;
import com.zimbra.cs.imap.ImapHandler;
import com.zimbra.cs.imap.ImapMailboxStore;
import com.zimbra.cs.imap.ImapPath;
import com.zimbra.cs.imap.ImapProxy.ZimbraClientAuthenticator;
import com.zimbra.cs.imap.ImapRemoteSession;
import com.zimbra.cs.imap.ImapServerListener;
import com.zimbra.cs.imap.ImapServerListenerPool;
import com.zimbra.cs.imap.RemoteImapMailboxStore;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.security.sasl.ZimbraAuthenticator;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.AdminDestroyWaitSetRequest;
import com.zimbra.soap.admin.message.AdminDestroyWaitSetResponse;
import com.zimbra.soap.admin.message.QueryWaitSetRequest;
import com.zimbra.soap.admin.message.QueryWaitSetResponse;
import com.zimbra.soap.admin.type.SessionForWaitSet;
import com.zimbra.soap.admin.type.WaitSetInfo;

public class TestImapServerListener {
    private static final String LOCAL_USER_NAME = "TestImapServerListener-localuser";
    private static final String REMOTE_USER_NAME = "TestImapServerListener-remoteuser";
    private static final String REMOTE_USER_NAME2 = "TestImapServerListener-remoteuser2";
    private static final String PASS = "test123";
    private Account remoteAccount;
    private Account localAccount;
    private String[] imapServersForLocalHost = null;
    private String[] imapServersForRemoteHost = null;
    private static Server localServer = null;
    private static Server remoteServer = null;
    private static final Provisioning prov = Provisioning.getInstance();
    private static final SoapProvisioning sp = new SoapProvisioning();
    private static List<Server> servers;
    private static AuthenticatorFactory ZIMBRA_AUTH_FACTORY = new AuthenticatorFactory();
    private ZMailbox mboxStore;
    private ImapServerListener remoteListener;
    @BeforeClass
    public static void beforeClass() throws Exception {
        servers = prov.getAllServers(Provisioning.SERVICE_MAILBOX);
        ZIMBRA_AUTH_FACTORY.register(ZimbraAuthenticator.MECHANISM, ZimbraClientAuthenticator.class);
        localServer = prov.getLocalServer();
        sp.soapSetURI(LC.zimbra_admin_service_scheme.value() + localServer.getServiceHostname() + ":" + localServer.getAdminPort()
                + AdminConstants.ADMIN_SERVICE_URI);
        sp.soapZimbraAdminAuthenticate();
        for(Server s : servers) {
            if(!s.isLocalServer()) {
                remoteServer = s;
                break;
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        cleanup();
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraMailHost, localServer.getServiceHostname());
        localAccount = TestUtil.createAccount(LOCAL_USER_NAME, attrs);
        Provisioning.getInstance().setPassword(localAccount, PASS);
        imapServersForLocalHost = localServer.getReverseProxyUpstreamImapServers();

        if(remoteServer != null) {
            localServer.setReverseProxyUpstreamImapServers(new String[]{remoteServer.getServiceHostname()});
            attrs = Maps.newHashMap();
            attrs.put(Provisioning.A_zimbraMailHost, remoteServer.getServiceHostname());
            remoteAccount = TestUtil.createAccount(REMOTE_USER_NAME, attrs);
            Provisioning.getInstance().setPassword(remoteAccount, PASS);
            imapServersForRemoteHost = remoteServer.getReverseProxyUpstreamImapServers();
            remoteServer.setReverseProxyUpstreamImapServers(new String[]{localServer.getServiceHostname()});
            mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
            remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        }
        sp.flushCache("all", null, true);
    }

    @After
    public void tearDown() throws Exception {
        cleanup();
        if(localServer != null) {
            localServer.setReverseProxyUpstreamImapServers(imapServersForLocalHost);
        }
        if(remoteServer != null) {
            remoteServer.setReverseProxyUpstreamImapServers(imapServersForRemoteHost);
        }
        if(remoteListener != null) {
            remoteListener.shutdown();
        }
        sp.flushCache("all", null, true);
    }

    private void cleanup() throws Exception {
        TestUtil.deleteAccountIfExists(LOCAL_USER_NAME);
        TestUtil.deleteAccountIfExists(REMOTE_USER_NAME);
        TestUtil.deleteAccountIfExists(REMOTE_USER_NAME2);
        remoteAccount = null;
        localAccount = null;
    }

    @Test
    public void testNotifyInbox() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        ZMailbox mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        TestUtil.addMessage(mboxStore, "TestImapServerListener - testNotify - init message", Integer.toString(Mailbox.ID_FOLDER_INBOX));
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        remoteListener.addListener(session);
        //wait for waitset session to be created
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        session.doneSignal = new CountDownLatch(1);
        String subject = "TestImapServerListener - testNotifyInbox - trigger message";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), "randomUserTestImapServerListener@yahoo.com");
        TestUtil.waitForMessages(mboxStore, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered", session.wasTriggered());
        remoteListener.removeListener(session);
    }

    @Test
    public void testNotifyNewFolder() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        ZMailbox mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ZFolder folder = TestUtil.createFolder(mboxStore, "/TestImapServerListenerTestNotifyNewFolder");
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath(folder.getPath(), creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        remoteListener.addListener(session);
        //wait for waitset session to be created
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        session.doneSignal = new CountDownLatch(1);
        String subject = "TestImapServerListener - testNotifyNewFolder";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), "randomUserTestImapServerListener@yahoo.com");
        ZMessage msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox subject:\"%s\"", subject));
        mboxStore.moveMessage(msg.getId(), folder.getId());
        TestUtil.waitForMessage(mboxStore, String.format("in:%s subject:\"%s\"", folder.getName(), subject));
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered", session.wasTriggered());
        remoteListener.removeListener(session);
    }

    @Test
    public void testNotifyReadUnread() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        ZMailbox mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        remoteListener.addListener(session);
        //wait for waitset session to be created
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        session.doneSignal = new CountDownLatch(1);
        String subject = "TestImapServerListener - testNotifyReadUnread";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), "randomUserTestImapServerListener@yahoo.com");
        ZMessage msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox subject:\"%s\"", subject));
        assertTrue("New message should have UNREAD flag", msg.isUnread());
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered after delivering the test message", session.wasTriggered());

        //mark read
        session.doneSignal = new CountDownLatch(1);
        mboxStore.markMessageRead(msg.getId(), true);
        msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox is:read subject:\"%s\"", subject));
        assertFalse("New message should NOT have UNREAD flag", msg.isUnread());
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered after marking test message READ", session.wasTriggered());

        //mark unread
        session.doneSignal = new CountDownLatch(1);
        mboxStore.markMessageRead(msg.getId(), false);
        msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox is:unread subject:\"%s\"", subject));
        assertTrue("New message should have UNREAD flag", msg.isUnread());
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered after marking test message UREAD", session.wasTriggered());
        remoteListener.removeListener(session);
    }

    @Test
    public void testNotifyTagUntag() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        ZMailbox mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        remoteListener.addListener(session);
        //wait for waitset session to be created
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        session.doneSignal = new CountDownLatch(1);
        String subject = "TestImapServerListener - testNotifyTagUntag";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), "randomUserTestImapServerListener@yahoo.com");
        ZMessage msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox subject:\"%s\"", subject));
        assertTrue("New message should have UNREAD flag", msg.isUnread());
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered after delivering the test message", session.wasTriggered());
        ZTag tag = mboxStore.createTag("testNotifyTagUntag", Color.blue);

        //tag
        session.doneSignal = new CountDownLatch(1);
        mboxStore.tagMessage(msg.getId(), tag.getId(), true);
        msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox tag:testNotifyTagUntag subject:\"%s\"", subject));
        assertTrue("Test message should have testNotifyTagUntag tag after tagging", msg.getTagIds().contains(tag.getId()));
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered after tagging test message", session.wasTriggered());

        //untag
        session.doneSignal = new CountDownLatch(1);
        mboxStore.tagMessage(msg.getId(), tag.getId(), false);
        msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox -tag:testNotifyTagUntag subject:\"%s\"", subject));
        assertFalse("Test message should NOT have testNotifyTagUntag tag after untagging", msg.getTagIds().contains(tag.getId()));
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered after untagging test message", session.wasTriggered());
        remoteListener.removeListener(session);
    }

    @Test
    public void testNotifyRenameTag() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        ZMailbox mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        remoteListener.addListener(session);
        //wait for waitset session to be created
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        session.doneSignal = new CountDownLatch(1);
        String subject = "TestImapServerListener - testNotifyRenameTag";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), "randomUserTestImapServerListener@yahoo.com");
        ZMessage msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox subject:\"%s\"", subject));
        assertTrue("New message should have UNREAD flag", msg.isUnread());
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered after delivering the test message", session.wasTriggered());
        ZTag tag = mboxStore.createTag("testNotifyTag", Color.blue);

        //tag
        session.doneSignal = new CountDownLatch(1);
        mboxStore.tagMessage(msg.getId(), tag.getId(), true);
        msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox tag:testNotifyTag subject:\"%s\"", subject));
        assertTrue("Test message should have testNotifyTag tag after tagging", msg.getTagIds().contains(tag.getId()));
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered after tagging test message", session.wasTriggered());

        //rename tag
        session.doneSignal = new CountDownLatch(1);
        mboxStore.renameTag(tag.getId(), "testNotifyRenamedTag");
        msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox tag:testNotifyRenamedTag subject:\"%s\"", subject));
        assertTrue("Test message should have testNotifyRenamedTag tag after renaming the tag", msg.getTagIds().contains(tag.getId()));
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered after renaming the tag", session.wasTriggered());
        remoteListener.removeListener(session);
    }

    @Ignore ("this is failing until WaitSetResponse can process deletes")
    public void testNotifyDeleteItemFromInbox() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        ZMailbox mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        TestUtil.addMessage(mboxStore, "TestImapServerListener - testNotify - init message", Integer.toString(Mailbox.ID_FOLDER_INBOX));
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        String subject = "TestImapServerListener - testNotifyDeleteItemFromInbox - trigger message";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), "randomUserTestImapServerListener@yahoo.com");
        ZMessage msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox subject:\"%s\"", subject));
        remoteListener.addListener(session);
        //wait for waitset session to be created
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        session.doneSignal = new CountDownLatch(1);
        mboxStore.deleteMessage(msg.getId());
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered", session.wasTriggered());
        remoteListener.removeListener(session);
    }

    @Test
    public void testNotifyWrongFolder() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        ZMailbox mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        ZFolder folder = TestUtil.createFolder(mboxStore, "/TestImapServerListener-testNotifyNewFolder");
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        session.doneSignal = new CountDownLatch(1);
        remoteListener.addListener(session);
        //wait for waitset session to be created
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        TestUtil.addMessage(mboxStore, "TestImapServerListener - testNotifyWrongFolder", folder.getId());
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertFalse("Expected session to not be triggered", session.wasTriggered());
        remoteListener.removeListener(session);
    }

    @Test
    public void testRegisterUnregister() throws ServiceException, IOException, HttpException {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        assertNotNull("ImapServerListener instance should not be null", remoteListener);

        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        assertNotNull("ImapMailboxStore instance should not be null", imapStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        assertNotNull("ImapPath instance should not be null", path);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        assertNotNull("ImapFolder instance should not be null", i4folder);
        assertNotNull("ImapFolder.getCredentials() should not return null", i4folder.getCredentials());
        assertNotNull("ImapFolder.getPath() should not return null", i4folder.getPath());

        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        assertNotNull("ImapListener instance should not be null", session);
        assertFalse("Expecting ImapServerListener::isListeningOn to return false before calling addListener", remoteListener.isListeningOn(session.getTargetAccountId(), session.getFolderId()));
        assertNull("Should not have a waitset before registering first listener", remoteListener.getWSId());
        remoteListener.addListener(session);
        assertTrue("Expecting ImapServerListener::isListeningOn to return true after calling addListener", remoteListener.isListeningOn(session.getTargetAccountId(), session.getFolderId()));
        assertNotNull("Should have a waitset after registering first listener", remoteListener.getWSId());
        QueryWaitSetRequest req = new QueryWaitSetRequest(remoteListener.getWSId());
        SoapTransport transport = TestUtil.getAdminSoapTransport(remoteServer);
        QueryWaitSetResponse resp = JaxbUtil.elementToJaxb(transport.invoke(JaxbUtil.jaxbToElement(req)));
        assertNotNull(resp);
        List<WaitSetInfo> wsInfoList = resp.getWaitsets();
        assertNotNull(wsInfoList);
        assertFalse(wsInfoList.isEmpty());
        assertEquals(1, wsInfoList.size());
        WaitSetInfo wsInfo = wsInfoList.get(0);
        assertNotNull(wsInfo);
        assertEquals(remoteListener.getWSId(), wsInfo.getWaitSetId());
        remoteListener.removeListener(session);
        assertFalse("Expecting ImapServerListener::isListeningOn to return false after calling removeListener", remoteListener.isListeningOn(session.getTargetAccountId(), session.getFolderId()));
        assertNull("Should not have a waitset after removing last listener", remoteListener.getWSId());
    }

    @Test
    public void testGetListeners() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        assertNotNull("ImapServerListener instance should not be null", remoteListener);

        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);

        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        assertNotNull("ImapListener instance should not be null", session);
        Set<ImapRemoteSession> sessions = remoteListener.getListeners(remoteAccount.getId(), i4folder.getId());
        assertNotNull("getListeners should not return NULL before adding a listener", sessions);
        assertTrue("expecting an empty list before adding a listener", sessions.isEmpty());
        remoteListener.addListener(session);
        //wait for waitset session to be created
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        sessions = remoteListener.getListeners(remoteAccount.getId(), i4folder.getId());
        assertNotNull("getListeners should not return NULL after adding a listener", sessions);
        assertFalse("expecting a non empty list after adding a listener", sessions.isEmpty());
        remoteListener.removeListener(session);
        sessions = remoteListener.getListeners(remoteAccount.getId(), i4folder.getId());
        assertNotNull("getListeners should not return NULL after removing a listener", sessions);
        assertTrue("expecting an empty list after removing a listener", sessions.isEmpty());
        assertNull("Should not have a waitset after removing last listener", remoteListener.getWSId());
    }

    @Test
    public void testShutdown() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        remoteListener.addListener(session);
        //wait for waitset session to be created
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        assertNotNull("Should have a waitset after addig a listener", remoteListener.getWSId());
        remoteListener.shutdown();
        assertNull("Should not have a waitset after shutting down ImapServerListener", remoteListener.getWSId());
    }

    @Test
    public void testRemoveFolderInterest() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);

        //add listener on INBOX
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener inboxSession = new MockImapListener(imapStore, i4folder, handler);
        remoteListener.addListener(inboxSession);

        //check created waitset
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        assertNotNull(resp);
        List<WaitSetInfo> wsInfoList = resp.getWaitsets();
        assertFalse(wsInfoList.isEmpty());
        assertEquals(1, wsInfoList.size());
        WaitSetInfo wsInfo = wsInfoList.get(0);
        assertNotNull(wsInfo);
        assertEquals(remoteListener.getWSId(), wsInfo.getWaitSetId());
        List<SessionForWaitSet> sessions = wsInfo.getSessions();
        assertNotNull(sessions);
        assertEquals("expected to find 1 session after adding a listener for INBOX", 1, sessions.size());
        SessionForWaitSet s = sessions.get(0);
        Set<Integer> folders = s.getWaitSetSession().getFolderInterestsAsSet();
        assertNotNull("folder interests cannot be NULL", folders);
        assertEquals("should have one folder interest", 1, folders.size());
        assertTrue("folder interests should contain INBOX", folders.contains(Mailbox.ID_FOLDER_INBOX));

        //add listener on DRAFTS
        path = new ImapPath("DRAFTS", creds);
        params = 0;
        i4folder = new ImapFolder(path, params, handler);
        ImapRemoteSession draftsSession = (ImapRemoteSession) imapStore.createListener(i4folder, handler);
        remoteListener.addListener(draftsSession);

        //check that waitset was updated
        resp = TestUtil.waitForSessions(1, 2, 6000, remoteListener.getWSId(), remoteServer);
        assertNotNull(resp);
        wsInfoList = resp.getWaitsets();
        assertFalse(wsInfoList.isEmpty());
        wsInfo = wsInfoList.get(0);
        assertNotNull(wsInfo);
        assertEquals(remoteListener.getWSId(), wsInfo.getWaitSetId());
        sessions = wsInfo.getSessions();
        assertNotNull(sessions);
        assertEquals("expected to find 1 session after adding a listener for DRAFTS", 1, sessions.size());
        s = sessions.get(0);
        folders = s.getWaitSetSession().getFolderInterestsAsSet();
        assertNotNull("folder interests cannot be NULL", folders);
        assertEquals("should have two folder interests", 2, folders.size());
        assertTrue("folder interests should contain DRAFTS", folders.contains(Mailbox.ID_FOLDER_DRAFTS));
        assertTrue("folder interests should contain INBOX", folders.contains(Mailbox.ID_FOLDER_INBOX));
        remoteListener.removeListener(inboxSession);

        //check that waitset was updated after removing a listener
        resp = TestUtil.waitForSessions(1, 1, 6000, remoteListener.getWSId(), remoteServer);
        assertNotNull(resp);
        wsInfoList = resp.getWaitsets();
        assertFalse(wsInfoList.isEmpty());
        wsInfo = wsInfoList.get(0);
        assertNotNull(wsInfo);
        assertEquals(remoteListener.getWSId(), wsInfo.getWaitSetId());
        sessions = wsInfo.getSessions();
        assertNotNull(sessions);
        assertEquals("expected to find 1 session after removing listener for inbox", 1, sessions.size());
        s = sessions.get(0);
        folders = s.getWaitSetSession().getFolderInterestsAsSet();
        assertNotNull("folder interests cannot be NULL", folders);
        assertEquals("should have one folder interest after removing a listener", 1, folders.size());
        assertTrue("folder interests should contain DRAFTS after removing INBOX listener", folders.contains(Mailbox.ID_FOLDER_DRAFTS));
    }

    @Test
    public void testDestroyWaitset() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        ZMailbox mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        TestUtil.addMessage(mboxStore, "TestImapServerListener - testDestroyWaitset - init message", Integer.toString(Mailbox.ID_FOLDER_INBOX));
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        remoteListener.addListener(session);
        //wait for waitset session to be created
        String waitSetId = remoteListener.getWSId();
        TestUtil.waitForSessions(1, 1, 6000, waitSetId, remoteServer);

        //delete waitset
        ZimbraLog.test.debug("Destroying waitset %s", waitSetId);
        AdminDestroyWaitSetRequest destroyReq = new AdminDestroyWaitSetRequest(waitSetId);
        SoapTransport transport = TestUtil.getAdminSoapTransport(remoteServer);
        AdminDestroyWaitSetResponse destroyResp = JaxbUtil.elementToJaxb(transport.invoke(JaxbUtil.jaxbToElement(destroyReq)));
        Assert.assertNotNull("AdminDestroyWaitSetResponse should not be null", destroyResp);
        Assert.assertNotNull("AdminDestroyWaitSetResponse::waitSetId should not be null", destroyResp.getWaitSetId());
        Assert.assertEquals("AdminDestroyWaitSetResponse has wrong waitSetId", waitSetId, destroyResp.getWaitSetId());

        //wait for ImapServerListener to create a new WaitSet
        int maxWait = 5000;
        while(maxWait > 0) {
            if(remoteListener.getWSId() != null && !waitSetId.equalsIgnoreCase(remoteListener.getWSId())) {
                break;
            } else {
                maxWait -= 500;
                Thread.sleep(500);
            }
        }
        Assert.assertFalse("ImapServerListener should have created a new waitset", waitSetId.equalsIgnoreCase(remoteListener.getWSId()));

        //send a message
        session.doneSignal = new CountDownLatch(1);
        String subject = "TestImapServerListener - testDestroyWaitset - trigger message";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), TestUtil.getAddress("TestImapServerListener-testDestroyWaitset"));
        TestUtil.waitForMessages(mboxStore, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to NOT be triggered", session.wasTriggered());
        assertFalse("ImapServerListener should have created a new waitset", remoteListener.getWSId().equalsIgnoreCase(waitSetId));
    }

    @Test
    public void testDeleteAccount() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        ZMailbox mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        TestUtil.addMessage(mboxStore, "TestImapServerListener - testNotify - init message", Integer.toString(Mailbox.ID_FOLDER_INBOX));
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        remoteListener.addListener(session);
        //wait for waitset session to be created
        String waitSetId = remoteListener.getWSId();
        QueryWaitSetResponse resp = TestUtil.waitForSessions(1, 1, 6000, waitSetId, remoteServer);

        //delete account
        TestUtil.deleteAccount(REMOTE_USER_NAME);
        session.killSignal = new CountDownLatch(1);
        try {
            session.killSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertFalse("Expected 1st user's session to NOT be triggered", session.wasTriggered());
        assertTrue("Expected 1st user's session to be unregistered", session.wasUnregistered());

        //wait for ImapServerListener to create a new WaitSet
        int maxWait = 5000;
        while(maxWait > 0) {
            if(remoteListener.getWSId() == null) {
                break;
            } else {
                maxWait -= 500;
                Thread.sleep(500);
            }
        }
        assertNull("ImapServerListener should have deleted the waitset", remoteListener.getWSId());
    }

    @Test
    public void testDeleteOneOutOfTwoAccounts() throws Exception {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);

        //first account
        ZMailbox mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        TestUtil.addMessage(mboxStore, "TestImapServerListener - testNotify - init message 1", Integer.toString(Mailbox.ID_FOLDER_INBOX));
        ImapServerListener remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        MockImapListener session = new MockImapListener(imapStore, i4folder, handler);
        remoteListener.addListener(session);

        //second account (will be deleted)
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraMailHost, remoteServer.getServiceHostname());
        Account remoteAccount2 = TestUtil.createAccount(REMOTE_USER_NAME2, attrs);
        ZMailbox mboxStore2 = TestUtil.getZMailbox(REMOTE_USER_NAME2);
        ZimbraLog.test.debug("Created 2d account " + REMOTE_USER_NAME2 + " with ID " + remoteAccount2.getId());
        TestUtil.addMessage(mboxStore2, "TestImapServerListener - testNotify - init message 2", Integer.toString(Mailbox.ID_FOLDER_INBOX));
        ImapServerListener remoteListener2 = ImapServerListenerPool.getInstance().get(mboxStore2);
        assertEquals("Should be getting the same ImapServerListener for both accounts on the same server", remoteListener2, remoteListener);
        RemoteImapMailboxStore imapStore2 = new RemoteImapMailboxStore(mboxStore2);
        ImapCredentials creds2 = new ImapCredentials(remoteAccount2);
        ImapPath path2 = new ImapPath("INBOX", creds2);
        params = 0;
        ImapHandler handler2 = new MockImapHandler().setCredentials(creds2);
        ImapFolder i4folder2 = new ImapFolder(path2, params, handler2);
        MockImapListener session2 = new MockImapListener(imapStore2, i4folder2, handler2);
        remoteListener2.addListener(session2);

        //wait for waitset sessions to be created
        String waitSetId = remoteListener.getWSId();
        assertTrue("Both listener references should be pointing to the same listener with the same waitset", waitSetId.equalsIgnoreCase(remoteListener2.getWSId()));
        TestUtil.waitForSessions(2, 2, 6000, waitSetId, remoteServer);

        //delete the 2d account
        ZimbraLog.test.debug("Deleting " + REMOTE_USER_NAME2);
        TestUtil.deleteAccount(REMOTE_USER_NAME2);
        session2.killSignal = new CountDownLatch(1);

        //send a message to the 1st account
        session.doneSignal = new CountDownLatch(1);
        String subject = "TestImapServerListener - testDeleteOneOutOfTwoAccounts - trigger message";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), TestUtil.getAddress("TestImapServerListener-testDeleteOneOutOfTwoAccounts"));
        TestUtil.waitForMessages(mboxStore, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);

        //the 2d account's session should be killed
        try {
            session2.killSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }

        //the 1st accounts session should be triggered
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 2), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }

        ZimbraLog.test.debug("Checking triggers");
        assertTrue("Expected the 1st account's session to be triggered", session.wasTriggered());
        assertFalse("Expected the 2d account's session to NOT be triggered", session2.wasTriggered());
        assertTrue("Expected the 2d account's session to be unregistered", session2.wasUnregistered());
        assertFalse("Expected the 1st account's session to NOT be unregistered", session.wasUnregistered());
        assertNotNull("ImapServerListener should NOT have deleted the waitset", remoteListener.getWSId());
        assertTrue("Waitset ID should have remained the same", waitSetId.equalsIgnoreCase(remoteListener.getWSId()));
    }

    class MockImapHandler extends ImapHandler {

        MockImapHandler() {
            super(new ImapConfig(false));
        }

        @Override
        protected String getRemoteIp() {
            return "127.0.0.1";
        }

        @Override
        protected void sendLine(String line, boolean flush) throws IOException {
        }

        @Override
        protected void dropConnection(boolean sendBanner) {
        }

        @Override
        protected void close() {
        }

        @Override
        protected void enableInactivityTimer() throws IOException {
        }

        @Override
        protected void completeAuthentication() throws IOException {
        }

        @Override
        protected boolean doSTARTTLS(String tag) throws IOException {
            return false;
        }

        @Override
        protected InetSocketAddress getLocalAddress() {
            return new InetSocketAddress("localhost", 0);
        }
    }

    class MockImapListener extends ImapRemoteSession {
        private boolean triggered = false;
        private boolean unregistered = false;
        public CountDownLatch doneSignal;
        public CountDownLatch killSignal;
        MockImapListener(ImapMailboxStore store, ImapFolder i4folder, ImapHandler handler) throws ServiceException {
            super(store, i4folder, handler);
        }

        public boolean wasTriggered() {
            return triggered;
        }

        public boolean wasUnregistered() {
            return unregistered;
        }

        @Override
        public void notifyPendingChanges(PendingModifications pnsIn, int changeId, SourceSessionInfo source) {
            triggered = true;
            doneSignal.countDown();
        }

        @Override
        public Session unregister() {
            unregistered = true;
            killSignal.countDown();
            return detach();
        }
    }
}
