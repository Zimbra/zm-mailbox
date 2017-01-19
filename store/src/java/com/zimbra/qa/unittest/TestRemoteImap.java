package com.zimbra.qa.unittest;

import static com.zimbra.common.util.TaskUtil.newDaemonThreadFactory;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import javax.mail.MessagingException;
import javax.security.auth.login.LoginException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.imap.ImapProxy.ZimbraClientAuthenticator;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.imap.AppendResult;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.BodyStructure;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.IDInfo;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapResponse;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.ResponseHandler;
import com.zimbra.cs.security.sasl.ZimbraAuthenticator;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.AuthProviderException;
import com.zimbra.cs.util.BuildInfo;

public class TestRemoteImap {
    private static final String USER = "TestRemoteImap-user";
    private static final String PASS = "test123";
    private String[] imapServersForLocalhost = null;
    private static boolean mHomeDisplayMailFoldersOnly;
    private static boolean mIMAPDisplayMailFoldersOnly;
    private Server homeServer = null;
    private Server imapServer = null;
    private Account acc = null;
    private ImapConnection connection;
    private static final Provisioning prov = Provisioning.getInstance();
    private static final SoapProvisioning sp = new SoapProvisioning();
    private static List<Server> servers;
    private static AuthenticatorFactory ZIMBRA_AUTH_FACTORY = new AuthenticatorFactory();

    @BeforeClass
    public static void beforeClass() throws Exception {
        servers = prov.getAllServers(Provisioning.SERVICE_MAILBOX);
        ZIMBRA_AUTH_FACTORY.register(ZimbraAuthenticator.MECHANISM, ZimbraClientAuthenticator.class);
        sp.soapSetURI(LC.zimbra_admin_service_scheme.value() + servers.get(0).getServiceHostname() + ":" + servers.get(0).getAdminPort()
                + AdminConstants.ADMIN_SERVICE_URI);
        sp.soapZimbraAdminAuthenticate();
    }

    @Before
    public void setUp() throws ServiceException, IOException  {
        Assume.assumeTrue(servers.size() > 1);
        cleanup();
        homeServer = servers.get(0);
        imapServer = servers.get(1);
        mHomeDisplayMailFoldersOnly = homeServer.isImapDisplayMailFoldersOnly();
        mIMAPDisplayMailFoldersOnly = imapServer.isImapDisplayMailFoldersOnly();
        homeServer.setImapDisplayMailFoldersOnly(false);
        imapServer.setImapDisplayMailFoldersOnly(false);
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraMailHost, homeServer.getServiceHostname());
        attrs.put(Provisioning.A_zimbraFeatureIMEnabled, ProvisioningConstants.TRUE);
        acc = TestUtil.createAccount(USER, attrs);
        Provisioning.getInstance().setPassword(acc, PASS);

        //preserve original setting, so we can set it back on tearDown
        imapServersForLocalhost = homeServer.getReverseProxyUpstreamImapServers();

        //homeServer will be the mailbox server and imapServer will be the remote IMAP
        homeServer.setReverseProxyUpstreamImapServers(new String[]{imapServer.getServiceHostname()});

        sp.flushCache("all", null, true);
    }

    private void cleanup() throws ServiceException {
        if(TestUtil.accountExists(USER)) {
            TestUtil.deleteAccount(USER);
        }
    }

    @After
    public void tearDown() throws ServiceException  {
        cleanup();
        if(homeServer != null) {
            homeServer.setReverseProxyUpstreamImapServers(imapServersForLocalhost);
            homeServer.setImapDisplayMailFoldersOnly(mHomeDisplayMailFoldersOnly);
        }
        if(imapServer != null) {
            imapServer.setImapDisplayMailFoldersOnly(mIMAPDisplayMailFoldersOnly);
        }
        sp.flushCache("all", null, true);
    }

    private ImapConnection connect(Server server) throws IOException {
        ImapConfig config = new ImapConfig(server.getServiceHostname());
        config.setPort(server.getImapBindPort());
        config.setAuthenticationId(USER);
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection connection = new ImapConnection(config);
        connection.connect();
        return connection;
    }

    private ImapConnection connectAsProxy(Server server) throws IOException {
        ImapConfig config = new ImapConfig(server.getServiceHostname());
        config.setPort(server.getImapBindPort());
        config.setAuthenticationId(USER);
        config.setAuthenticatorFactory(ZIMBRA_AUTH_FACTORY);
        config.setMechanism(ZimbraAuthenticator.MECHANISM);
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection connection = new ImapConnection(config);
        connection.connect();
        return connection;
    }

    @Test
    public void testAUTHENTICATEToIMAPHost() throws IOException  {
        Assume.assumeTrue(servers.size() > 1);
        connection = connect(imapServer);
        connection.login(PASS);
        assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
    }

    @Test
    public void testAUTHENTICATEToHomeHost() throws IOException  {
        Assume.assumeTrue(servers.size() > 1);
        connection = connect(homeServer);
        try {
            connection.login(PASS);
        } catch (CommandFailedException e) {
            assertNotNull(e);
            assertTrue("Should toget 'LOGIN failed' error from home server", e.getMessage().indexOf("LOGIN failed") > -1);
        }
        assertFalse("IMAP connection should not be authenticated", connection.isAuthenticated());
    }

    @Test
    public void testAuthenticateAsProxyToIMAPHost() throws IOException, LoginException, AuthProviderException, AuthTokenException  {
        Assume.assumeTrue(servers.size() > 1);
        connection = connectAsProxy(imapServer);
        IDInfo id = new IDInfo();
        id.put(IDInfo.NAME, "ZCS");
        id.put(IDInfo.VERSION, BuildInfo.VERSION);
        connection.id(id);
        connection.authenticate(AuthProvider.getAuthToken(acc).getEncoded());
        assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
    }

    @Test
    public void testAuthenticateAsProxyToHomeHost() throws IOException, LoginException, AuthProviderException, AuthTokenException  {
        Assume.assumeTrue(servers.size() > 1);
        connection = connectAsProxy(homeServer);
        IDInfo id = new IDInfo();
        id.put(IDInfo.NAME, "ZCS");
        id.put(IDInfo.VERSION, BuildInfo.VERSION);
        connection.id(id);
        connection.authenticate(AuthProvider.getAuthToken(acc).getEncoded());
        assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
    }

    @Test
    public void testFolderList() throws IOException  {
        Assume.assumeTrue(servers.size() > 1);
        connection = connect(imapServer);
        connection.login(PASS);
        Assert.assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
        List<ListData> listResult = connection.list("", "*");
        assertNotNull(listResult);
        Assert.assertTrue("Should have at least 5 subscriptions ", listResult.size() >= 5);
        TestImap.verifyFolderList(listResult);
    }

    @Test
    public void testMailOnlyFolderList() throws IOException, ServiceException  {
        Assume.assumeTrue(servers.size() > 1);
        imapServer.setImapDisplayMailFoldersOnly(true);
        connection = connect(imapServer);
        connection.login(PASS);
        Assert.assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
        List<ListData> listResult = connection.list("", "*");
        assertNotNull(listResult);
        Assert.assertTrue("Should have at least 5 subscriptions ", listResult.size() >= 5);
        TestImap.verifyFolderList(listResult, true);
    }

    @Test
    public void testListInbox() throws Exception {
        Assume.assumeTrue(servers.size() > 1);
        connection = connect(imapServer);
        connection.login(PASS);
        Assert.assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
        List<ListData> listResult = connection.list("", "INBOX");
        assertNotNull(listResult);
        assertEquals("List result should have this number of entries", 1, listResult.size());
    }

    @Ignore("requires SELECT to work")
    public void testAppend() throws Exception {
        Assume.assumeTrue(servers.size() > 1);
        connection = connect(imapServer);
        connection.login(PASS);
        Assert.assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
        assertTrue(connection.hasCapability("UIDPLUS"));
        Flags flags = Flags.fromSpec("afs");
        Date date = new Date(System.currentTimeMillis());
        Literal msg = TestImap.message(100000);
        try {
            AppendResult res = connection.append("INBOX", flags, date, msg);
            assertNotNull(res);
            byte[] b = fetchBody(res.getUid());
            assertArrayEquals("content mismatch", msg.getBytes(), b);
        } finally {
            msg.dispose();
        }
    }

    @Test
    public void testSubscribe() throws IOException, ServiceException {
        Assume.assumeTrue(servers.size() > 1);
        String folderName = "TestRemoteImap-testSubscribe";
        TestUtil.createFolder(TestUtil.getZMailbox(USER), folderName);
        connection = connect(imapServer);
        connection.login(PASS);
        Assert.assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
        List<ListData> listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        Assert.assertEquals("Should have 0 subscriptions at this point", 0, listResult.size());
        try {
            connection.subscribe(folderName);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        Assert.assertEquals(1, listResult.size());
        Assert.assertTrue("Should be subscribed to " + folderName + ". Instead got " + listResult.get(0).getMailbox(), folderName.equalsIgnoreCase(listResult.get(0).getMailbox()));
    }

    @Test
    public void testSubscribeNested() throws IOException, ServiceException {
        Assume.assumeTrue(servers.size() > 1);
        String folderName = "TestRemoteImap-testSubscribe";
        ZFolder folder = TestUtil.createFolder(TestUtil.getZMailbox(USER), Integer.toString(Mailbox.ID_FOLDER_INBOX), folderName);
        connection = connect(imapServer);
        connection.login(PASS);
        Assert.assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
        List<ListData> listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        Assert.assertEquals("Should have 0 subscriptions before subscribing", 0, listResult.size());
        try {
            connection.subscribe(folder.getPath());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        Assert.assertEquals("Should have found 1 subscription after subscribing", 1, listResult.size());
        Assert.assertTrue("Should be subscribed to " + folder.getPath().substring(1) +". Instead got " + listResult.get(0).getMailbox(), folder.getPath().substring(1).equalsIgnoreCase(listResult.get(0).getMailbox()));
    }

    @Test
    public void testUnSubscribe() throws IOException, ServiceException {
        Assume.assumeTrue(servers.size() > 1);
        String folderName = "TestRemoteImap-testSubscribe";
        TestUtil.createFolder(TestUtil.getZMailbox(USER), folderName);
        connection = connect(imapServer);
        connection.login(PASS);
        Assert.assertTrue("IMAP connection is not authenticated", connection.isAuthenticated());
        List<ListData> listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        Assert.assertEquals("Should have 0 subscriptions before subscribing", 0, listResult.size());
        try {
            connection.subscribe(folderName);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        Assert.assertEquals(1, listResult.size());
        Assert.assertTrue("Should be subscribed to " + folderName + ". Instead got " + listResult.get(0).getMailbox(), folderName.equalsIgnoreCase(listResult.get(0).getMailbox()));
        try {
            connection.unsubscribe(folderName);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        Assert.assertEquals("Should have 0 subscriptions after unsubscribing", 0, listResult.size());
    }

    @Test
    public void testCreate() throws Exception {
        Assume.assumeTrue(servers.size() > 1);
        String folderName = "TestRemoteImap-testCreate";
        connection = connect(imapServer);
        connection.login(PASS);
        Assert.assertFalse(connection.exists(folderName));
        connection.create(folderName);
        Assert.assertTrue(connection.exists(folderName));
    }

    private byte[] fetchBody(long uid) throws IOException {
        MessageData md = connection.uidFetch(uid, "(BODY.PEEK[])");
        assertNotNull("message not found", md);
        assertEquals(uid, md.getUid());
        Body[] bs = md.getBodySections();
        assertNotNull(bs);
        assertEquals(1, bs.length);
        return bs[0].getImapData().getBytes();
    }

    @Test
    public void testLOGOUTNotLoggedIn() throws IOException {
        Assume.assumeTrue(servers.size() > 1);
        connection = connect(imapServer);
        connection.logout();
        assertFalse("IMAP connection should not be authenticated after sending LOGOUT", connection.isAuthenticated());
    }

    @Test
    public void testLOGOUTHomeServer() throws IOException {
        Assume.assumeTrue(servers.size() > 1);
        connection = connect(homeServer);
        connection.logout();
        assertFalse("IMAP connection should not be authenticated after sending LOGOUT", connection.isAuthenticated());
    }

    @Test
    public void testLOGOUT() throws IOException {
        Assume.assumeTrue(servers.size() > 1);
        connection = connect(imapServer);
        connection.login(PASS);
        Assert.assertTrue("IMAP should be authenticated after logging in", connection.isAuthenticated());
        connection.logout();
        assertFalse("IMAP connection should not be authenticated after sending LOGOUT", connection.isAuthenticated());
    }

    @Test
    public void testListFolderContents() throws IOException, ServiceException, MessagingException {
        Assume.assumeTrue(servers.size() > 1);
        String folderName = "TestRemoteImap-testOpenFolder";
        String subject = "TestRemoteImap-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        String msgId = TestUtil.addMessage(zmbox, subject, folder.getId(), null);
        ZMessage msg = zmbox.getMessageById(msgId);
        connection = connect(imapServer);
        connection.login(PASS);
        MailboxInfo info = connection.select(folderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE INTERNALDATE BODY BODY[TEXT])");
        assertEquals(1, mdMap.size());
        MessageData md = mdMap.values().iterator().next();
        assertNotNull(md);
        Envelope env = md.getEnvelope();
        assertNotNull(env);
        assertEquals(msg.getSubject(), env.getSubject());
        assertEquals(msg.getDate(), md.getInternalDate().getTime());
        BodyStructure bs = md.getBodyStructure();
        assertNotNull(bs);
        assertEquals("TEXT", bs.getType());
        assertEquals("PLAIN", bs.getSubtype());
        assertNotNull(md.getBodySections());
        assertEquals(1, md.getBodySections().length);
        Body body = md.getBodySections()[0];
        assertNotNull(body);
        Literal imapData = (Literal) body.getData();
        assertEquals(MessageBuilder.DEFAULT_MESSAGE_BODY, imapData.toString());
    }

    @Test
    public void testIdleNotification() throws Exception {
        ImapConnection connection1 = connect(imapServer);
        connection1.select("INBOX");

        ImapConnection connection2 = connect(imapServer);

        Flags flags = Flags.fromSpec("afs");
        Date date = new Date(System.currentTimeMillis());
        String subject = "TestRemoteImap-testIdleNotification";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        TestUtil.addMessage(zmbox, subject, "1", null);
        Literal msg = TestImap.message(100000);
        final AtomicLong exists = new AtomicLong(-1);
        final AtomicLong recent = new AtomicLong(-1);
        final CountDownLatch doneSignal = new CountDownLatch(1);

        connection1.idle(new ResponseHandler() {
            @Override
            public void handleResponse(ImapResponse res) {
                if (res.getCCode() == CAtom.EXISTS) {
                    exists.set(res.getNumber());
                }
                if (res.getCCode() == CAtom.RECENT) {
                    recent.set(1);
                }
            }
        });
        Assert.assertTrue("Connection is not idling when it should be", connection1.isIdling());

        try {
	        AppendResult res = connection2.append("INBOX", flags, date, msg);
	        Assert.assertNotNull(res);

	        try {
	            doneSignal.await(5, TimeUnit.SECONDS);
	        } catch (Exception e) {
	            Assert.fail("Wait interrupted. ");
	        }

	        Assert.assertEquals("Connection was not notified of new items", 1, recent.get());
	        Assert.assertEquals("Connection was not notified of existing items", 1, exists.get());

        } finally {
            msg.dispose();
        }
    }

    @Test
    public void testMultipleSelect() throws Exception {
        Assume.assumeTrue(servers.size() > 1);
        String folderName = "TestRemoteImap-testSelectFolder";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        TestUtil.createFolder(zmbox, folderName);
        connection = connect(imapServer);
        connection.login(PASS);
        ExecutorService executor = newCachedThreadPool(newDaemonThreadFactory("TestRemoteImap"));
        for (int i=0; i<3; i++) {
            Future<MailboxInfo> future = executor.submit(new Callable<MailboxInfo>() {

                @Override
                public MailboxInfo call() throws Exception {
                    return connection.select(folderName);
                }
            });
            try {
                MailboxInfo info = future.get(5, TimeUnit.SECONDS);
                assertNotNull(info);
            } catch (TimeoutException e) {
                Assert.fail("failed getting a response from SELECT command within 5 seconds");
            }

        }
    }
}