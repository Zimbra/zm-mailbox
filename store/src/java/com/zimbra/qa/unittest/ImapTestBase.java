package com.zimbra.qa.unittest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.dom4j.DocumentException;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.imap.ImapProxy.ZimbraClientAuthenticator;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.security.sasl.ZimbraAuthenticator;
import com.zimbra.cs.service.AuthProvider;

public abstract class ImapTestBase {

    @Rule
    public TestName testInfo = new TestName();
    protected static String USER = null;
    protected static String SHAREE = null;
    protected static final String PASS = "test123";
    protected static Server imapServer = null;
    protected ImapConnection connection = null;
    protected ImapConnection otherConnection = null;
    protected static boolean mIMAPDisplayMailFoldersOnly;
    protected final int LOOP_LIMIT = LC.imap_throttle_command_limit.intValue();
    protected static String imapHostname;
    protected static int imapPort;
    protected String testId;

    private static boolean saved_imap_always_use_remote_store;
    private static String[] saved_imap_servers = null;

    protected abstract int getImapPort();

    /** expect this to be called by subclass @Before method */
    protected void sharedSetUp() throws ServiceException, IOException  {
        testId = String.format("%s-%s-%d", this.getClass().getSimpleName(), testInfo.getMethodName(), (int)Math.abs(Math.random()*100));
        USER = String.format("%s-user", testId).toLowerCase();
        SHAREE = String.format("%s-sharee", testId).toLowerCase();
        getLocalServer();
        mIMAPDisplayMailFoldersOnly = imapServer.isImapDisplayMailFoldersOnly();
        imapServer.setImapDisplayMailFoldersOnly(false);
        sharedCleanup();
        Account acc = TestUtil.createAccount(USER);
        Provisioning.getInstance().setPassword(acc, PASS);
        //find out what hostname or IP IMAP server is listening on
        List<String> addrs = Arrays.asList(imapServer.getImapBindAddress());
        if(addrs.isEmpty()) {
            imapHostname = imapServer.getServiceHostname();
        } else {
            imapHostname = addrs.get(0);
        }
        imapPort = getImapPort();
    }

    /** expect this to be called by subclass @After method */
    protected void sharedTearDown() throws ServiceException  {
        sharedCleanup();
        if (imapServer != null) {
            imapServer.setImapDisplayMailFoldersOnly(mIMAPDisplayMailFoldersOnly);
        }
    }

    private void sharedCleanup() throws ServiceException {
        if (connection != null) {
            connection.close();
        }
        if (otherConnection != null) {
            otherConnection.close();
            otherConnection = null;
        }
        TestUtil.deleteAccountIfExists(USER);
        TestUtil.deleteAccountIfExists(SHAREE);
    }

    protected static Server getLocalServer() throws ServiceException {
        if (imapServer == null) {
            imapServer = Provisioning.getInstance().getLocalServer();
        }
        return imapServer;
    }

    /** expect this to be called by subclass @Before method */
    public static void saveImapConfigSettings()
    throws ServiceException, DocumentException, ConfigException, IOException {
        getLocalServer();
        saved_imap_always_use_remote_store = LC.imap_always_use_remote_store.booleanValue();
        saved_imap_servers = imapServer.getReverseProxyUpstreamImapServers();
    }

    /** expect this to be called by subclass @After method */
    public static void restoreImapConfigSettings()
    throws ServiceException, DocumentException, ConfigException, IOException {
        getLocalServer();
        if (imapServer != null) {
            imapServer.setReverseProxyUpstreamImapServers(saved_imap_servers);
        }
        TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(saved_imap_always_use_remote_store));
    }

    protected ImapConnection connect(String user) throws IOException {
        ImapConfig config = new ImapConfig(imapHostname);
        config.setPort(imapPort);
        config.setAuthenticationId(user);
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection conn = new ImapConnection(config);
        conn.connect();
        return conn;
    }

    protected ImapConnection connect() throws IOException {
        return connect(USER);
    }

    protected ImapConnection connectAndLogin(String user) throws IOException {
        ImapConnection imapConn = connect(user);
        imapConn.login(PASS);
        return imapConn;
    }

    protected ImapConnection connectAndSelectInbox(String user) throws IOException {
        ImapConnection imapConn = connectAndLogin(user);
        imapConn.select("INBOX");
        return imapConn;
    }

    protected ImapConnection connectAndSelectInbox() throws IOException {
        return connectAndSelectInbox(USER);
    }

    protected ImapConnection getAdminConnection() throws Exception {
        AuthenticatorFactory authFactory = new AuthenticatorFactory();
        authFactory.register(ZimbraAuthenticator.MECHANISM, ZimbraClientAuthenticator.class);
        ImapConfig config = new ImapConfig(imapHostname);
        config.setMechanism(ZimbraAuthenticator.MECHANISM);
        config.setAuthenticatorFactory(authFactory);
        config.setPort(imapPort);
        config.setAuthenticationId(LC.zimbra_ldap_user.value());
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection conn = new ImapConnection(config);
        conn.connect();
        conn.authenticate(AuthProvider.getAdminAuthToken().getEncoded());
        return conn;
    }


}
