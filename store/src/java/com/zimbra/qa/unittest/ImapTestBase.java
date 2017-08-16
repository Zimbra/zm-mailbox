package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.DocumentException;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.google.common.base.Joiner;
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.imap.ImapProxy.ZimbraClientAuthenticator;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MessageData;
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
        if (USER != null) {
            TestUtil.deleteAccountIfExists(USER);
            TestUtil.deleteAccountIfExists(SHAREE);
        }
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
        ImapConfig config = new ImapConfig(imapServer.getServiceHostname());
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

    protected void doSelectShouldFail(ImapConnection conn, String folderName) throws IOException {
        MailboxInfo mbInfo = null;
        try {
            mbInfo = conn.select(folderName);
            fail(String.format("'SELECT %s' succeeded - should have failed mbInfo=%s", folderName, mbInfo));
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            String expected = "SELECT failed";
            assertTrue(String.format("CommandFailedException error should contain '%s', was '%s'", err, expected),
                    err.contains(expected));
        }
    }

    protected MailboxInfo doSelectShouldSucceed(ImapConnection conn, String folderName) throws IOException {
        MailboxInfo mbInfo = null;
        try {
            mbInfo = conn.select(folderName);
            assertNotNull(String.format("return MailboxInfo for 'SELECT %s'", folderName), mbInfo);
            ZimbraLog.test.debug("return MailboxInfo for 'SELECT %s' - %s", folderName, mbInfo);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.debug("'SELECT %s' failed", folderName, cfe);
            fail(String.format("'SELECT %s' failed with '%s'", folderName, cfe.getError()));
        }
        return mbInfo;
    }

    protected void doSelectShouldFail(String folderName) throws IOException {
        doSelectShouldFail(connection, folderName);
    }

    protected MailboxInfo doSelectShouldSucceed(String folderName) throws IOException {
        return doSelectShouldSucceed(connection, folderName);
    }

    protected static class StatusExecutor {
        private final ImapConnection conn;
        private Long expectedExists = null;
        private Long expectedRecent = null;
        private Long expectedUnseen = null;
        protected MailboxInfo mbInfo = null;
        protected StatusExecutor(ImapConnection imapConn) {
            conn = imapConn;
        }
        protected StatusExecutor setExists(long expected) { expectedExists = expected; return this;};
        protected StatusExecutor setRecent(long expected) { expectedRecent = expected; return this;};
        protected StatusExecutor setUnseen(long expected) { expectedUnseen = expected; return this;};

        protected MailboxInfo execShouldSucceed(String folderName, Object... params) throws IOException {
        String pr = Joiner.on(",").join(params);
        try {
            mbInfo = conn.status(folderName, params);
            assertNotNull(String.format("return MailboxInfo for 'STATUS %s (%s)'", folderName, pr), mbInfo);
            if (expectedExists != null) {
                assertEquals(String.format("Count of EXISTS for 'STATUS %s (%s)'", folderName, pr),
                        expectedExists.longValue(), mbInfo.getExists());
            }
            if (expectedRecent != null) {
                assertEquals(String.format("Count of RECENT for 'STATUS %s (%s)'", folderName, pr),
                        expectedRecent.longValue(), mbInfo.getRecent());
            }
            if (expectedUnseen != null) {
                assertEquals(String.format("Count of UNSEEN for 'STATUS %s (%s)'", folderName, pr),
                        expectedUnseen.longValue(), mbInfo.getUnseen());
            }
        } catch (CommandFailedException cfe) {
            fail(String.format("'STATUS %s (%s)' failed with '%s'", folderName, pr, cfe.getError()));
        }
        return mbInfo;
        }
    }

    protected Map<Long, MessageData> doFetchShouldSucceed(ImapConnection conn, String range, String what,
            List<String> expectedSubjects)
    throws IOException {
        try {
            Map<Long, MessageData> mdMap = conn.fetch(range, what);
            assertNotNull(String.format("map returned by 'FETCH %s %s' should not be null", range, what), mdMap);
            assertEquals(String.format("Size of map returned by 'FETCH %s %s'", range, what),
                    expectedSubjects.size(), mdMap.size());
            int cnt = 0;
            Iterator<MessageData> iter = mdMap.values().iterator();
            while (iter.hasNext()) {
                MessageData md = iter.next();
                assertNotNull("MessageData should not be null", md);
                Envelope env = md.getEnvelope();
                assertNotNull(String.format(
                        "Envelope for MessageData for %s item in results of 'FETCH %s %s' should not be null",
                        cnt, range, what), env);
                assertEquals(String.format(
                        "Subject for %s item in results of 'FETCH %s %s'",
                        cnt, range, what), expectedSubjects.get(cnt), env.getSubject());
                cnt++;
            }
            return mdMap;
        } catch (CommandFailedException cfe) {
            fail(String.format("'FETCH %s %s' failed with '%s'", range, what, cfe.getError()));
            return null;
        }
    }

    protected void doRenameShouldFail(String origFolderName, String newFolderName) throws IOException {
        try {
            connection.rename(origFolderName, newFolderName);
            fail(String.format("2nd attempt to RENAME %s %s succeeded when it shouldn't have'",
                    origFolderName, newFolderName));
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            String expected = "RENAME failed";
            assertTrue(String.format("CommandFailedException error should contain '%s', was '%s'", err, expected),
                    err.contains(expected));
        }
    }

    protected void doRenameShouldSucceed(String origFolderName, String newFolderName) throws IOException {
        try {
            connection.rename(origFolderName, newFolderName);
        } catch (CommandFailedException cfe) {
            fail(String.format("attempt to 'RENAME %s %s' failed - %s", origFolderName, newFolderName, cfe.getError()));
        }
    }

    protected void doListShouldFail(ImapConnection conn, String ref, String mailbox, String expected)
    throws IOException {
        try {
            conn.list(ref, mailbox);
            fail(String.format("'LIST \"%s\" \"%s\"' should have failed", ref, mailbox));
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            assertTrue(String.format("'LIST \"%s\" \"%s\"' error should contain '%s' was '%s'",
                    ref, mailbox, expected, err), err.contains(expected));
        }
    }

    protected List<ListData> doListShouldSucceed(ImapConnection conn, String ref, String mailbox, int expected)
    throws IOException {
        try {
            List<ListData> listResult = conn.list(ref, mailbox);
            assertNotNull(String.format("list result 'list \"%s\" \"%s\"' should not be null",
                    ref, mailbox), listResult);
            assertEquals(String.format( "Number of entries in list returned for 'list \"%s\" \"%s\"'",
                ref, mailbox), expected, listResult.size());
            return listResult;
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            fail(String.format("'LIST \"%s\" \"%s\"' returned error '%s'", ref, mailbox, err));
            return null;
        }
    }


}
