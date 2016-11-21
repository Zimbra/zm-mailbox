package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Map;

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
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
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
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.IDInfo;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.security.sasl.ZimbraAuthenticator;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.AuthProviderException;
import com.zimbra.cs.util.BuildInfo;

public class TestRemoteImap {
    private static final String USER = "TestRemoteImap-user";
    private static final String PASS = "test123";
    private String[] imapServersForLocalhost = null;
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
        }
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
        TestImap.verifyFolderSubscriptions(listResult);
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
}