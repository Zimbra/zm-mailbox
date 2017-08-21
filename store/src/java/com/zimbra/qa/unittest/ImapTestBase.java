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
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.dom4j.DocumentException;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.google.common.base.Joiner;
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.AccessBoundedRegex;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.imap.ImapProxy.ZimbraClientAuthenticator;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
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

    /** expect this to be called by subclass @Before method */
    public static void saveImapConfigSettings()
    throws ServiceException, DocumentException, ConfigException, IOException {
        getLocalServer();
        saved_imap_always_use_remote_store = LC.imap_always_use_remote_store.booleanValue();
        saved_imap_servers = imapServer.getReverseProxyUpstreamImapServers();
        saved_imap_server_enabled = imapServer.isImapServerEnabled();
        saved_imap_ssl_server_enabled = imapServer.isImapSSLServerEnabled();
    }

    /** expect this to be called by subclass @After method */
    public static void restoreImapConfigSettings()
    throws ServiceException, DocumentException, ConfigException, IOException {
        getLocalServer();
        if (imapServer != null) {
            imapServer.setReverseProxyUpstreamImapServers(saved_imap_servers);
            imapServer.setImapServerEnabled(saved_imap_server_enabled);
            imapServer.setImapSSLServerEnabled(saved_imap_ssl_server_enabled);
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

    protected void doSubscribeShouldSucceed(ImapConnection imapConn, String folderName) {
        try {
            imapConn.subscribe(folderName);
        } catch (Exception e) {
            fail(String.format("%s %s failed - %s", CAtom.SUBSCRIBE, folderName, e.getMessage()));
        }
    }

    protected void doUnsubscribeShouldSucceed(ImapConnection imapConn, String folderName) {
        try {
            imapConn.unsubscribe(folderName);
        } catch (Exception e) {
            fail(String.format("%s %s failed - %s", CAtom.UNSUBSCRIBE, folderName, e.getMessage()));
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
        String cmdDesc = String.format("'%s \"%s\" \"%s\"'", CAtom.LIST, ref, mailbox);
        try {
            List<ListData> listResult = conn.list(ref, mailbox);
            assertNotNull(String.format("list result %s should not be null", cmdDesc), listResult);
            assertEquals(String.format( "Number of entries in list returned for %s", cmdDesc),
                    expected, listResult.size());
            return listResult;
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            fail(String.format("cmdDesc returned error '%s'", cmdDesc, err));
            return null;
        }
    }

    protected List<ListData> doLSubShouldSucceed(ImapConnection conn, String ref, String mailbox,
            List<String> expectedMboxNames, String testDesc)
    throws IOException {
        String cmdDesc = String.format("'%s \"%s\" \"%s\"'", CAtom.LSUB, ref, mailbox);
        try {
            List<ListData> listResult = conn.lsub(ref, mailbox);
            assertNotNull(String.format("%s:list result from %s should not be null", testDesc, cmdDesc), listResult);
            assertEquals(String.format( "%s:Number of entries in list returned for %s", testDesc, cmdDesc),
                    expectedMboxNames.size(), listResult.size());
            for (String mbox : expectedMboxNames) {
                String tMbox = (mbox.startsWith("/")) ? mbox.substring(1) : mbox;
                assertTrue(String.format("%s:Mailbox '%s' NOT in list returned by %s", testDesc, tMbox, cmdDesc),
                        listContains(listResult, tMbox));
            }
            return listResult;
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            fail(String.format("%s:%s returned error '%s'", testDesc, cmdDesc, err));
            return null;
        }
    }

    protected boolean listContains(List<ListData> listData, String folderName) {
        for (ListData ld: listData) {
            if (ld.getMailbox().equalsIgnoreCase(folderName)) {
                return true;
            }
        }
        return false;
    }

    protected MessageData fetchMessage(ImapConnection conn, long uid) throws IOException {
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

    protected void doAppend(ImapConnection conn, String folderName, int size, Flags flags, boolean fetchResult)
            throws IOException {
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
        } finally {
            msg.dispose();
        }
    }

    protected void doAppend(ImapConnection conn, String folderName, int size, Flags flags) throws IOException {
        doAppend(conn, folderName, size, flags, true);
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
