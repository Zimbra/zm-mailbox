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
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SoapTransport;
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
import com.zimbra.soap.admin.message.QueryWaitSetRequest;
import com.zimbra.soap.admin.message.QueryWaitSetResponse;
import com.zimbra.soap.admin.type.SessionForWaitSet;
import com.zimbra.soap.admin.type.WaitSetInfo;

public class TestImapServerListener {
    private static final String LOCAL_USER_NAME = "TestImapServerListener-localuser";
    private static final String REMOTE_USER_NAME = "TestImapServerListener-remoteuser";
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
        }
        sp.flushCache("all", null, true);
        mboxStore = TestUtil.getZMailbox(REMOTE_USER_NAME);
        remoteListener = ImapServerListenerPool.getInstance().get(mboxStore);
    }

    @After
    public void tearDown() throws Exception {
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
        if(TestUtil.accountExists(LOCAL_USER_NAME)) {
            TestUtil.deleteAccount(LOCAL_USER_NAME);
        }
        if(TestUtil.accountExists(REMOTE_USER_NAME)) {
            TestUtil.deleteAccount(REMOTE_USER_NAME);
        }
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
        session.doneSignal = new CountDownLatch(1);
        String subject = "TestImapServerListener - testNotify - trigger message";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), "randomUserTestImapServerListener@yahoo.com");
        TestUtil.waitForMessages(mboxStore, String.format("in:inbox is:unread \"%s\"", subject), 1, 1000);
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 1), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered", session.wasTriggered());
        remoteListener.removeListener(session);
    }

    //TODO: this is failing until WaitSetResponse can process modifications
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
        session.doneSignal = new CountDownLatch(1);
        String subject = "TestImapServerListener - testNotifyNewFolder";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), "randomUserTestImapServerListener@yahoo.com");
        ZMessage msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox subject:\"%s\"", subject));
        mboxStore.moveMessage(msg.getId(), folder.getId());
        TestUtil.waitForMessage(mboxStore, String.format("in:%s subject:\"%s\"", folder.getName(), subject));
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 1), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertTrue("Expected session to be triggered", session.wasTriggered());
        remoteListener.removeListener(session);
    }

    //TODO: this is failing until WaitSetResponse can process deletes 
    @Test
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
        String subject = "TestImapServerListener - testNotify - trigger message";
        TestUtil.addMessageLmtp(subject, TestUtil.getAddress(REMOTE_USER_NAME), "randomUserTestImapServerListener@yahoo.com");
        ZMessage msg = TestUtil.waitForMessage(mboxStore, String.format("in:inbox subject:\"%s\"", subject));
        remoteListener.addListener(session);
        session.doneSignal = new CountDownLatch(1);
        mboxStore.deleteMessage(msg.getId());
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 1), TimeUnit.SECONDS);
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
        TestUtil.addMessage(mboxStore, "TestImapServerListener - testNotifyWrongFolder", folder.getId());
        try {
            session.doneSignal.await((LC.zimbra_waitset_nodata_sleep_time.intValue()/1000 + 1), TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Wait interrupted.");
        }
        assertFalse("Expected session to not be triggered", session.wasTriggered());
        remoteListener.removeListener(session);
    }

    @Test
    public void testRegisterUnregister() throws ServiceException, IOException {
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
        
        ImapRemoteSession session = (ImapRemoteSession) imapStore.createListener(i4folder, handler);
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
    public void testGetListeners() throws ServiceException, IOException {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        assertNotNull("ImapServerListener instance should not be null", remoteListener);

        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);

        ImapRemoteSession session = (ImapRemoteSession) imapStore.createListener(i4folder, handler);
        assertNotNull("ImapListener instance should not be null", session);
        List<ImapRemoteSession> sessions = remoteListener.getListeners(remoteAccount.getId(), i4folder.getId());
        assertNotNull("getListeners should not return NULL before adding a listener", sessions);
        assertTrue("expecting an empty list before adding a listener", sessions.isEmpty());
        remoteListener.addListener(session);
        sessions = remoteListener.getListeners(remoteAccount.getId(), i4folder.getId());
        assertNotNull("getListeners should not return NULL after adding a listener", sessions);
        assertFalse("expecting a non empty list after adding a listener", sessions.isEmpty());
        remoteListener.removeListener(session);
        sessions = remoteListener.getListeners(remoteAccount.getId(), i4folder.getId());
        assertNotNull("getListeners should not return NULL after removing a listener", sessions);
        assertTrue("expecting an empty list after removing a listener", sessions.isEmpty());
    }

    @Test
    public void testShutdown() throws ServiceException, IOException {
        Assume.assumeNotNull(remoteServer);
        Assume.assumeNotNull(remoteAccount);
        RemoteImapMailboxStore imapStore = new RemoteImapMailboxStore(mboxStore);
        ImapCredentials creds = new ImapCredentials(remoteAccount);
        ImapPath path = new ImapPath("INBOX", creds);
        byte params = 0;
        ImapHandler handler = new MockImapHandler().setCredentials(creds);
        ImapFolder i4folder = new ImapFolder(path, params, handler);
        ImapRemoteSession session = (ImapRemoteSession) imapStore.createListener(i4folder, handler);
        remoteListener.addListener(session);
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
        ImapRemoteSession inboxSession = (ImapRemoteSession) imapStore.createListener(i4folder, handler);
        remoteListener.addListener(inboxSession);
        Thread.sleep(5000);

        //check created waitset
        QueryWaitSetRequest req = new QueryWaitSetRequest(remoteListener.getWSId());
        SoapTransport transport = TestUtil.getAdminSoapTransport(remoteServer);
        QueryWaitSetResponse resp = JaxbUtil.elementToJaxb(transport.invoke(JaxbUtil.jaxbToElement(req)));
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
        Thread.sleep(5000);
        //check that waitset was updated
        req = new QueryWaitSetRequest(remoteListener.getWSId());
        resp = JaxbUtil.elementToJaxb(transport.invoke(JaxbUtil.jaxbToElement(req)));
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
        Thread.sleep(5000);

        //check that waitset was updated after removing a listener
        req = new QueryWaitSetRequest(remoteListener.getWSId());
        resp = JaxbUtil.elementToJaxb(transport.invoke(JaxbUtil.jaxbToElement(req)));
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
        public CountDownLatch doneSignal;
        MockImapListener(ImapMailboxStore store, ImapFolder i4folder, ImapHandler handler) throws ServiceException {
            super(store, i4folder, handler);
            // TODO Auto-generated constructor stub
        }

        public boolean wasTriggered() {
            return triggered;
        }

        public void resetTrigger() {
            triggered = false;
        }

        public void notifyPendingChanges(PendingModifications pnsIn, int changeId, Session source) {
            triggered = true;
            doneSignal.countDown();
        }
    }
}