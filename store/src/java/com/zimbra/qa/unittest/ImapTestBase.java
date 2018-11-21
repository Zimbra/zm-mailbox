package com.zimbra.qa.unittest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.zimbra.cs.mailclient.MailConfig;
import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.zimbra.common.account.Key.CacheEntryBy;
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.AccessBoundedRegex;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.imap.ImapProxy.ZimbraClientAuthenticator;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.imap.AppendMessage;
import com.zimbra.cs.mailclient.imap.AppendResult;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.security.sasl.ZimbraAuthenticator;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.soap.admin.type.CacheEntryType;

public abstract class ImapTestBase {

    @Rule
    public TestName testInfo = new TestName();
    protected static String USER = null;
    protected static String USER2 = null;
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
    private static boolean saved_imap_server_enabled;
    private static boolean saved_imap_ssl_server_enabled;
    private static String[] saved_imap_servers = null;
    private static String saved_max_message_size = null;

    protected abstract int getImapPort();

    /** expect this to be called by subclass @Before method */
    protected void sharedSetUp() throws ServiceException, IOException  {
        testId = String.format("%s-%s-%d", this.getClass().getSimpleName(), testInfo.getMethodName(), (int)Math.abs(Math.random()*100));
        USER = String.format("%s-user", testId).toLowerCase();
        USER2 = String.format("%s-user2", testId).toLowerCase();
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
            connection = null;
        }
        if (otherConnection != null) {
            otherConnection.close();
            otherConnection = null;
        }
        if (USER != null) {
            TestUtil.deleteAccountIfExists(USER);
            TestUtil.deleteAccountIfExists(SHAREE);
            TestUtil.deleteAccountIfExists(USER2);
        }
    }

    protected static Server getLocalServer() throws ServiceException {
        if (imapServer == null) {
            imapServer = Provisioning.getInstance().getLocalServer();
        }
        return imapServer;
    }

    /** Only need to do this once for each class - the corresponding restore needs to
     *  be done after every test though.
     */
    @BeforeClass
    public static void saveImapConfigSettings()
    throws ServiceException, DocumentException, ConfigException, IOException {
        getLocalServer();
        if (imapServer != null) {
            Provisioning.getInstance().flushCache(CacheEntryType.server,
                    new CacheEntry[]{new CacheEntry(CacheEntryBy.id, imapServer.getId())});
            imapServer = null;
            getLocalServer();
        }
        saved_imap_always_use_remote_store = LC.imap_always_use_remote_store.booleanValue();
        if (imapServer != null) {
            saved_imap_servers = imapServer.getReverseProxyUpstreamImapServers();
            saved_imap_server_enabled = imapServer.isImapServerEnabled();
            saved_imap_ssl_server_enabled = imapServer.isImapSSLServerEnabled();
        }
        ZimbraLog.test.debug("Saved ImapConfigSettings %s=%s %s=%s %s=%s",
                LC.imap_always_use_remote_store.key(), saved_imap_always_use_remote_store,
                Provisioning.A_zimbraImapServerEnabled, saved_imap_server_enabled,
                Provisioning.A_zimbraImapSSLServerEnabled, saved_imap_ssl_server_enabled);
        Provisioning prov = Provisioning.getInstance();
        saved_max_message_size = prov.getConfig().getAttr(Provisioning.A_zimbraMtaMaxMessageSize, null);
    }

    /** restore settings after every test */
    @After
    public void restoreImapConfigSettings()
    throws ServiceException, DocumentException, ConfigException, IOException {
        getLocalServer();
        if (imapServer != null) {
            ZimbraLog.test.debug("Restoring ImapConfigSettings %s=%s %s=%s %s=%s",
                    LC.imap_always_use_remote_store.key(), saved_imap_always_use_remote_store,
                    Provisioning.A_zimbraImapServerEnabled, saved_imap_server_enabled,
                    Provisioning.A_zimbraImapSSLServerEnabled, saved_imap_ssl_server_enabled);
            imapServer.setReverseProxyUpstreamImapServers(saved_imap_servers);
            imapServer.setImapServerEnabled(saved_imap_server_enabled);
            imapServer.setImapSSLServerEnabled(saved_imap_ssl_server_enabled);
            Provisioning.getInstance().flushCache(CacheEntryType.server,
                    new CacheEntry[]{new CacheEntry(CacheEntryBy.id, imapServer.getId())});
        }
        TestUtil.setLCValue(LC.imap_always_use_remote_store,
                String.valueOf(saved_imap_always_use_remote_store));
        TestUtil.setConfigAttr(Provisioning.A_zimbraMtaMaxMessageSize, saved_max_message_size);
    }

    public static void checkConnection(ImapConnection conn) {
        assertNotNull("ImapConnection object should not be null", conn);
        assertFalse("ImapConnection should not be closed", conn.isClosed());
    }

    protected ImapConfig getImapConfig() {
        ImapConfig config = new ImapConfig(imapHostname);
        config.setSecurity(MailConfig.Security.TLS_IF_AVAILABLE);
        return config;
    }

    protected ImapConnection connect(String user) throws IOException {
        ImapConfig config = getImapConfig();
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

    protected ImapConfig getAdminImapConfig() {
        AuthenticatorFactory authFactory = new AuthenticatorFactory();
        authFactory.register(ZimbraAuthenticator.MECHANISM, ZimbraClientAuthenticator.class);
        ImapConfig config = new ImapConfig(imapServer.getServiceHostname());
        config.setMechanism(ZimbraAuthenticator.MECHANISM);
        config.setAuthenticatorFactory(authFactory);
        config.setPort(imapPort);
        config.setAuthenticationId(LC.zimbra_ldap_user.value());
        config.getLogger().setLevel(Log.Level.trace);
        return config;
    }

    protected ImapConnection getAdminConnection() throws Exception {
        ImapConnection conn = new ImapConnection(getAdminImapConfig());
        conn.connect();
        conn.authenticate(AuthProvider.getAdminAuthToken().getEncoded());
        return conn;
    }

    protected void doSelectShouldFail(ImapConnection conn, String folderName) throws IOException {
        checkConnection(conn);
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
        checkConnection(conn);
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

    protected MailboxInfo doExamineShouldSucceed(ImapConnection conn, String folderName) throws IOException {
        checkConnection(conn);
        MailboxInfo mbInfo = null;
        try {
            mbInfo = conn.examine(folderName);
            assertNotNull(String.format("return MailboxInfo for 'EXAMINE %s'", folderName), mbInfo);
            ZimbraLog.test.debug("return MailboxInfo for 'EXAMINE %s' - %s", folderName, mbInfo);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.debug("'EXAMINE %s' failed", folderName, cfe);
            fail(String.format("'EXAMINE %s' failed with '%s'", folderName, cfe.getError()));
        }
        return mbInfo;
    }

    protected static class StatusExecutor {
        private final ImapConnection conn;
        private Long expectedExists = null;
        private Long expectedRecent = null;
        private Long expectedUnseen = null;
        protected MailboxInfo mbInfo = null;
        protected StatusExecutor(ImapConnection imapConn) {
            checkConnection(imapConn);
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
        checkConnection(conn);
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

    protected void doSubscribeShouldSucceed(ImapConnection imapConn, String folderName) {
        checkConnection(imapConn);
        try {
            imapConn.subscribe(folderName);
        } catch (Exception e) {
            fail(String.format("%s %s failed - %s", CAtom.SUBSCRIBE, folderName, e.getMessage()));
        }
    }

    protected void doUnsubscribeShouldSucceed(ImapConnection imapConn, String folderName) {
        checkConnection(imapConn);
        try {
            imapConn.unsubscribe(folderName);
        } catch (Exception e) {
            fail(String.format("%s %s failed - %s", CAtom.UNSUBSCRIBE, folderName, e.getMessage()));
        }
    }

    protected void doListShouldFail(ImapConnection conn, String ref, String mailbox, String expected)
    throws IOException {
        checkConnection(conn);
        try {
            conn.list(ref, mailbox);
            fail(String.format("'LIST \"%s\" \"%s\"' should have failed", ref, mailbox));
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            assertTrue(String.format("'LIST \"%s\" \"%s\"' error should contain '%s' was '%s'",
                    ref, mailbox, expected, err), err.contains(expected));
        }
    }

    /**
     * Note, due to a slightly strange quirk, the expectedMboxNames should be prefixed '/' in the case
     * where the mailboxes are in the Other Users' Namespace (i.e. start with "/home/"), otherwise, they
     * should not have the '/' prefix.  The "/" in that case comes from the Namespace prefix and NOT
     * from the mailbox name.
     *
     * For another way of looking at it, it is worth comparing NAMESPACE and LIST command output from
     * https://tools.ietf.org/html/rfc2342 - IMAP4 Namespace:
     *     C: A001 NAMESPACE
     *     S: * NAMESPACE (("" "/")) (("#Users/" "/")) NIL
     *     S: A001 OK NAMESPACE command completed
     *     C: A002 LIST "" "#Users/Mike/%"
     *     S: * LIST () "/" "#Users/Mike/INBOX"
     *     S: * LIST () "/" "#Users/Mike/Foo"
     *     S: A002 OK LIST command completed.
     *
     * with the Zimbra equivalent:
     *
     *     C: ZIMBRA01 NAMESPACE
     *     S: * NAMESPACE (("" "/")) (("/home/" "/")) NIL
     *     S: ZIMBRA01 OK NAMESPACE completed
     *     C: ZIMBRA02 LIST "" "/home/other-user/*"
     *     S: * LIST (\HasChildren) "/" "/home/other-user/INBOX/shared"
     *     S: * LIST (\HasNoChildren) "/" "/home/other-user/INBOX/shared/subFolder"
     *     S: ZIMBRA02 OK LIST completed
     */
    protected List<ListData> doLSubShouldSucceed(ImapConnection conn, String ref, String mailbox,
            List<String> expectedMboxNames, String testDesc)
    throws IOException {
        checkConnection(conn);
        String cmdDesc = String.format("'%s \"%s\" \"%s\"'", CAtom.LSUB, ref, mailbox);
        try {
            List<ListData> listResult = conn.lsub(ref, mailbox);
            checkListDataList(cmdDesc, testDesc, listResult, expectedMboxNames);
            return listResult;
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            fail(String.format("%s:%s returned error '%s'", testDesc, cmdDesc, err));
            return null;
        }
    }

    protected List<ListData> doListShouldSucceed(ImapConnection conn, String ref, String mailbox,
            List<String> expectedMboxNames, String testDesc)
    throws IOException {
        checkConnection(conn);
        String cmdDesc = String.format("'%s \"%s\" \"%s\"'", CAtom.LIST, ref, mailbox);
        try {
            List<ListData> listResult = conn.list(ref, mailbox);
            checkListDataList(cmdDesc, testDesc, listResult, expectedMboxNames);
            return listResult;
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            fail(String.format("%s:%s returned error '%s'", testDesc, cmdDesc, err));
            return null;
        }
    }

    public void checkListDataList(String cmdDesc, String testDesc, List<ListData> listResult,
            List<String> expectedMboxNames) throws IOException {
        assertNotNull(String.format("%s:list result from %s should not be null", testDesc, cmdDesc),
                listResult);
        List<String> actualMboxNames = mailboxNames(listResult);
        List<String> missingMboxNames = Lists.newArrayList();
        List<String> extraMboxNames = Lists.newArrayList();
        for (String mbox : expectedMboxNames) {
            if (!containsIgnoreCase(actualMboxNames, mbox)) {
                missingMboxNames.add(mbox);
            }
        }
        for (String mbox : actualMboxNames) {
            if (!containsIgnoreCase(expectedMboxNames, mbox)) {
                extraMboxNames.add(mbox);
            }
        }
        if (!missingMboxNames.isEmpty() || !extraMboxNames.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (!missingMboxNames.isEmpty()) {
                sb.append("\nMissing mailbox names:");
                sb.append(Joiner.on("\n    ").join(missingMboxNames));
            }
            if (!extraMboxNames.isEmpty()) {
                sb.append("\nExtra mailbox names:");
                sb.append(Joiner.on("\n    ").join(extraMboxNames));
            }
            fail(String.format("%s:'%s' returned unexpected mailbox names %s\nList Result:%s",
                    testDesc, cmdDesc, sb, listResult));
        }
        // Doubt if can get here but just in case
        assertEquals(String.format( "%s:Number of entries in list returned for %s\n%s",
                testDesc, cmdDesc, listResult), expectedMboxNames.size(), listResult.size());
    }

    public boolean containsIgnoreCase(List<String> mboxNames, String mboxName) {
        if (mboxNames == null) {
            return false;
        }
        for (String mbox: mboxNames) {
            if (mboxName.equalsIgnoreCase(mbox)) {
                return true;
            }
        }
        return false;
    }

    public boolean listDataContains(List<ListData> listData, String mboxName) {
        return containsIgnoreCase(mailboxNames(listData), mboxName);
    }

    public List<String> mailboxNames(List<ListData> listData) {
        if (listData == null) {
            return Collections.emptyList();
        }
        List<String> mboxes = Lists.newArrayListWithExpectedSize(listData.size());
        for (ListData ld: listData) {
            mboxes.add(ld.getMailbox());
        }
        return mboxes;
    }

    protected MessageData fetchMessage(ImapConnection conn, long uid) throws IOException {
        checkConnection(conn);
        MessageData md = conn.uidFetch(uid, "(FLAGS BODY.PEEK[])");
        assertNotNull(String.format(
                "`UID FETCH %s (FLAGS BODY.PEEK[])` returned no data - assume message not found", uid), md);
        assertEquals(String.format("`UID FETCH %s (FLAGS BODY.PEEK[])` returned wrong UID", uid), uid, md.getUid());
        return md;
    }

    protected byte[] getBody(MessageData md) throws IOException {
        Body[] bs = md.getBodySections();
        assertNotNull("body sections should not be NULL", bs);
        assertEquals("expecting 1 body section", 1, bs.length);
        return bs[0].getImapData().getBytes();
    }

    protected static String simpleMessage(String subject, String text) {
        return "Return-Path: dac@zimbra.com\r\n" +
            "Date: Fri, 27 Feb 2004 15:24:43 -0800 (PST)\r\n" +
            "From: dac <dac@zimbra.com>\r\n" +
            "To: bozo <bozo@foo.com>\r\n" +
            "Subject: " + subject + "\r\n\r\n" + text + "\r\n";
    }

    protected static String simpleMessage(String text) {
        return simpleMessage("Foo foo", text);
    }

    protected static interface RunnableTest {
        void run(ImapConnection connection) throws Exception;
    }

    protected void withLiteralPlus(boolean lp, RunnableTest test) throws Exception {
        ImapConfig config = connection.getImapConfig();
        boolean oldLp = config.isUseLiteralPlus();
        config.setUseLiteralPlus(lp);
        try {
            test.run(connection);
        } finally {
            config.setUseLiteralPlus(oldLp);
        }
    }

    public static Literal message(int size) throws IOException {
        File file = File.createTempFile("msg", null);
        file.deleteOnExit();
        FileWriter out = new FileWriter(file);
        try {
            out.write(simpleMessage("test message"));
            for (int i = 0; i < size; i++) {
                out.write('X');
                if (i % 72 == 0) {
                    out.write("\r\n");
                }
            }
        } finally {
            out.close();
        }
        return new Literal(file, true);
    }

    protected static Literal literal(String s) {
        return new Literal(bytes(s));
    }

    protected static byte[] bytes(String s) {
        try {
            return s.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            fail("UTF8 encoding not supported");
        }
        return null;
    }

    protected AppendResult doAppend(ImapConnection conn, String folderName, String subject, String body,
            Flags flags, boolean fetchResult) {
        checkConnection(conn);
        assertTrue("expecting UIDPLUS capability", conn.hasCapability("UIDPLUS"));
        String msg = simpleMessage(subject, body);
        Date date = new Date(System.currentTimeMillis());
        AppendMessage am = new AppendMessage(flags, date, literal(msg));
        try {
            AppendResult res = conn.append(folderName, am);
            assertNotNull("result of append command should not be null", res);
            if (fetchResult) {
                doSelectShouldSucceed(conn, folderName);
                MessageData md = fetchMessage(conn, res.getUid());
                byte[] b = getBody(md);
                assertArrayEquals("FETCH content not same as APPENDed content", msg.getBytes(), b);
            }
            return res;
        } catch (IOException e) {
            ZimbraLog.test.info("Exception thrown trying to append", e);
            fail("Exception thrown trying to append:" + e.getMessage());
        }
        return null;
    }

    protected AppendResult doAppend(ImapConnection conn, String folderName, int size, Flags flags,
            boolean fetchResult) throws IOException {
        checkConnection(conn);
        assertTrue("expecting UIDPLUS capability", conn.hasCapability("UIDPLUS"));
        Date date = new Date(System.currentTimeMillis());
        Literal msg = message(size);
        try {
            AppendResult res = conn.append(folderName, flags, date, msg);
            assertNotNull("result of append command should not be null", res);
            if (fetchResult) {
                doSelectShouldSucceed(conn, folderName);
                MessageData md = fetchMessage(conn, res.getUid());
                byte[] b = getBody(md);
                assertArrayEquals("content mismatch", msg.getBytes(), b);
            }
            return res;
        } finally {
            msg.dispose();
        }
    }

    protected AppendResult doAppend(ImapConnection conn, String folderName, int size, Flags flags)
            throws IOException {
        return doAppend(conn, folderName, size, flags, true);
    }

    public static void verifyFolderList(List<ListData> listResult) {
        verifyFolderList(listResult, false);
    }

    public static void verifyFolderList(List<ListData> listResult, boolean mailOnly) {
        boolean hasContacts = false;
        boolean hasChats = false;
        boolean hasEmailedContacts = false;
        boolean hasTrash = false;
        boolean hasDrafts = false;
        boolean hasInbox = false;
        boolean hasJunk = false;
        boolean hasSent = false;
        for (ListData ld : listResult) {
            if ((ld.getMailbox().equalsIgnoreCase("Contacts"))) {
                hasContacts = true;
            } else if ((ld.getMailbox().equalsIgnoreCase("Chats"))) {
                hasChats = true;
            } else if ((ld.getMailbox().equalsIgnoreCase("Emailed Contacts"))) {
                hasEmailedContacts = true;
            } else if ((ld.getMailbox().equalsIgnoreCase("Trash"))) {
                hasTrash = true;
            } else if ((ld.getMailbox().equalsIgnoreCase("Drafts"))) {
                hasDrafts = true;
            } else if ((ld.getMailbox().equalsIgnoreCase("Inbox"))) {
                hasInbox = true;
            } else if ((ld.getMailbox().equalsIgnoreCase("Sent"))) {
                hasSent = true;
            } else if ((ld.getMailbox().equalsIgnoreCase("Junk"))) {
                hasJunk = true;
            }
        }
        if(mailOnly) {
            assertFalse("mail-only folderList contains Chats", hasChats);
            assertFalse("mail-only folderList contains Contacts", hasContacts);
            assertFalse("mail-only folderList contains Emailed Contacts", hasEmailedContacts);
        } else {
            assertTrue("folderList * does not contain Chats", hasChats);
            assertTrue("folderList * does not contain Contacts", hasContacts);
            assertTrue("folderList * does not contain Emailed Contacts", hasEmailedContacts);
        }
        assertTrue("folderList * does not contain Trash", hasTrash);
        assertTrue("folderList * does not contain Drafts ", hasDrafts);
        assertTrue("folderList * does not contain Inbox", hasInbox);
        assertTrue("folderList * does not contain Sent", hasSent);
        assertTrue("folderList * does not contain Junk", hasJunk);
    }

    protected void checkRegex(String regexPatt, String target, Boolean expected, int maxAccesses, Boolean timeoutOk) {
        try {
            Pattern patt = Pattern.compile(regexPatt);
            AccessBoundedRegex re = new AccessBoundedRegex(patt, maxAccesses);
            assertEquals(String.format("matching '%s' against pattern '%s'", target, patt),
                    expected, Boolean.valueOf(re.matches(target)));
        } catch (AccessBoundedRegex.TooManyAccessesToMatchTargetException se) {
            assertTrue("Throwing exception considered OK", timeoutOk);
        }
    }

}
