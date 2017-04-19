package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZTag;
import com.zimbra.client.ZTag.Color;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.AccessBoundedRegex;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.imap.AppendMessage;
import com.zimbra.cs.mailclient.imap.AppendResult;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.BodyStructure;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapRequest;
import com.zimbra.cs.mailclient.imap.ImapResponse;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MailboxName;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.ResponseHandler;

/**
 * Definitions of tests used from {@Link TestLocalImapShared} and {@Link TestRemoteImapShared}
 */
public abstract class SharedImapTests {
    static String USER = "SharedImapTests-user";
    private static final String PASS = "test123";
    private Account acc = null;
    private Server imapServer = null;
    private ImapConnection connection;
    private static boolean mIMAPDisplayMailFoldersOnly;

    public void sharedSetUp() throws ServiceException, IOException  {
        imapServer = Provisioning.getInstance().getLocalServer();
        mIMAPDisplayMailFoldersOnly = imapServer.isImapDisplayMailFoldersOnly();
        imapServer.setImapDisplayMailFoldersOnly(false);
        sharedCleanup();
        acc = TestUtil.createAccount(USER);
        Provisioning.getInstance().setPassword(acc, PASS);
    }

    private void sharedCleanup() throws ServiceException {
        if(TestUtil.accountExists(USER)) {
            TestUtil.deleteAccount(USER);
        }
        if (connection != null) {
            connection.close();
        }
    }

    public void sharedTearDown() throws ServiceException  {
        sharedCleanup();
        if (imapServer != null) {
            imapServer.setImapDisplayMailFoldersOnly(mIMAPDisplayMailFoldersOnly);
        }
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

    ImapConnection connectAndSelectInbox() throws IOException {
        ImapConfig config = new ImapConfig(imapServer.getServiceHostname());
        config.setPort(imapServer.getImapBindPort());
        config.setAuthenticationId(USER);
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection connection = new ImapConnection(config);
        connection.connect();
        connection.login(PASS);
        connection.select("INBOX");
        return connection;
    }

    @Test
    public void testListFolderContents() throws IOException, ServiceException, MessagingException {
        String folderName = "SharedImapTests-testOpenFolder";
        String subject = "SharedImapTests-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);
        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE INTERNALDATE BODY BODY.PEEK[])");
        assertEquals("Size of map returned by fetch", 1, mdMap.size());
        MessageData md = mdMap.values().iterator().next();
        assertNotNull("MessageData", md);
        Envelope env = md.getEnvelope();
        assertNotNull("Envelope", env);
        assertEquals("Subject from envelope is wrong", subject, env.getSubject());
        assertNotNull("Internal date was requested and should not be NULL", md.getInternalDate());
        BodyStructure bs = md.getBodyStructure();
        assertNotNull("Body Structure is null", bs);
        if (bs.isMultipart()) {
            BodyStructure[] parts = bs.getParts();
            for (BodyStructure part : parts) {
                assertNotNull(part.getType());
                assertNotNull(part.getSubtype());
            }
        } else {
            assertNotNull("Body structure type", bs.getType());
            assertNotNull("Body structure sub-type", bs.getSubtype());
        }
        Body[] body = md.getBodySections();
        assertNotNull("body sections should not be null", body);
        assertEquals(1, body.length);
    }

    @Test
    public void testListFolderContentsEnvelope() throws IOException, ServiceException, MessagingException {
        String folderName = "SharedImapTests-testOpenFolder";
        String subject = "SharedImapTests-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);
        connection = connect(imapServer);
        connection.login(PASS);
        connection.select(folderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE)");
        assertEquals("Size of map returned by fetch", 1, mdMap.size());
        MessageData md = mdMap.values().iterator().next();
        assertNotNull("MessageData", md);
        Envelope env = md.getEnvelope();
        assertNotNull("Envelope", env);
        assertEquals("Subject from envelope is wrong", subject, env.getSubject());
        assertNull("Internal date was NOT requested and should be NULL", md.getInternalDate());
        BodyStructure bs = md.getBodyStructure();
        assertNull("Body Structure was not requested and should be NULL", bs);
        Body[] body = md.getBodySections();
        assertNull("body sections were not requested and should be null", body);
    }

    @Test
    public void testIdleNotification() throws IOException, ServiceException, MessagingException {
        final ImapConnection connection1 = connect(imapServer);
        connection1.login(PASS);
        connection1.select("INBOX");
        ImapConnection connection2 = connect(imapServer);
        connection2.login(PASS);
        try {
            Flags flags = Flags.fromSpec("afs");
            Date date = new Date(System.currentTimeMillis());
            String subject = "SharedImapTest-testIdleNotification";
            ZMailbox zmbox = TestUtil.getZMailbox(USER);
            TestUtil.addMessage(zmbox, subject, "1", null);
            Literal msg = message(1000);
            final AtomicBoolean gotExists = new AtomicBoolean(false);
            final AtomicBoolean gotRecent = new AtomicBoolean(false);
            final CountDownLatch doneSignal = new CountDownLatch(1);

            // Kick off an IDLE command - which will be processed in another thread until we call stopIdle()
            connection1.idle(new ResponseHandler() {
                @Override
                public void handleResponse(ImapResponse res) {
                    if ("* 1 EXISTS".equals(res.toString())) {
                        gotExists.set(true);
                    }
                    if ("* 1 RECENT".equals(res.toString())) {
                        gotRecent.set(true);
                    }
                    if (gotExists.get() && gotRecent.get()) {
                        doneSignal.countDown();
                    }
                }
            });
            assertTrue("Connection is not idling when it should be", connection1.isIdling());

            try {
                AppendResult res = connection2.append("INBOX", flags, date, msg);
                assertNotNull(res);
            } finally {
                msg.dispose();
            }

            try {
                doneSignal.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                Assert.fail("Wait interrupted. ");
            }
            assertTrue("Connection is not idling when it should be", connection1.isIdling());
            connection1.stopIdle();
            assertTrue("Connection is idling when it should NOT be", !connection1.isIdling());
            MailboxInfo mboxInfo = connection1.getMailboxInfo();
            assertEquals("Connection was not notified of correct number of existing items", 1, mboxInfo.getExists());
            assertEquals("Connection was not notified of correct number of recent items", 1, mboxInfo.getRecent());
        } finally {
            connection1.close();
            connection2.close();
        }
    }
    
    @Test
    public void testSubClauseAndSearch() throws Exception {
        connection = connectAndSelectInbox();
        connection.search((Object[]) new String[] { "OR (FROM yahoo.com) (FROM hotmail.com)" } );
        connection.search((Object[]) new String[] { "(SEEN)"} );
        connection.search((Object[]) new String[] { "(SEEN (ANSWERED UNDELETED))"} );
        connection.search((Object[]) new String[] { "NOT (SEEN UNDELETED)" } );
        connection.search((Object[]) new String[] { "(SEEN UNDELETED)" } );
        connection.search((Object[]) new String[] { "OR ANSWERED (SEEN UNDELETED)" } );
        connection.search((Object[]) new String[] { "OR (SEEN UNDELETED) ANSWERED"} );
        connection.search((Object[]) new String[] { "OR ((SEEN UNDELETED) ANSWERED) DRAFT"} );
    }

    @Test
    public void testNotSearch() throws Exception {
        connection = connectAndSelectInbox();
        connection.search((Object[]) new String[] { "NOT SEEN"} );
        connection.search((Object[]) new String[] { "NOT NOT SEEN"} );
        connection.search((Object[]) new String[] { "NOT NOT NOT SEEN"} );
    }

    @Test
    public void testAndSearch() throws Exception {
        connection = connectAndSelectInbox();
        connection.search((Object[]) new String[] { "HEADER Message-ID z@eg"} );
        connection.search((Object[]) new String[] { "HEADER Message-ID z@eg UNDELETED"} );
        connection.search((Object[]) new String[] { "ANSWERED HEADER Message-ID z@eg UNDELETED"} );
    }

    @Test
    public void testBadOrSearch() throws Exception {
        connection = connectAndSelectInbox();
        try {
            connection.search((Object[]) new String[] { "OR ANSWERED" } );
            Assert.fail("search succeeded in spite of invalid syntax");
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.debug("Got this exception", cfe);
            String es = "SEARCH failed: parse error: unexpected end of line; expected ' '";
            Assert.assertTrue(String.format("Exception '%s' should contain string '%s'", cfe.getMessage(), es),
                    cfe.getMessage().contains(es));
        }
    }

    @Test
    public void testOrSearch() throws Exception {
        connection = connectAndSelectInbox();
        connection.search((Object[]) new String[] { "OR SEEN ANSWERED DELETED"} );
        connection.search((Object[]) new String[] { "SEEN OR ANSWERED DELETED"} );
        connection.search((Object[]) new String[] { "OR DRAFT OR SEEN ANSWERED DELETED"} );
        connection.search((Object[]) new String[] { "OR HEADER Message-ID z@eg UNDELETED"} );
        List<String>terms = Lists.newArrayList();
        terms.add("HEADER");
        terms.add("Message-ID");
        terms.add("a@eg.com");

        for (int cnt = 0;cnt < 3; cnt++) {
            terms.add(0, String.format("b%s@eg.com", cnt));
            terms.add(0, "Message-ID");
            terms.add(0, "HEADER");
            terms.add(0, "OR");
        }
        terms.add("UNDELETED");
        connection.search(terms.toArray());
    }

    @Test
    public void testDeepNestedOrSearch() throws Exception {
        int maxNestingInSearchRequest = LC.imap_max_nesting_in_search_request.intValue();
        connection = connectAndSelectInbox();
        List<String>terms = Lists.newArrayList();
        terms.add("HEADER");
        terms.add("Message-ID");
        terms.add("a@eg.com");

        for (int cnt = 0;cnt < (maxNestingInSearchRequest - 2); cnt++) {
            terms.add(0, String.format("b%s@eg.com", cnt));
            terms.add(0, "Message-ID");
            terms.add(0, "HEADER");
            terms.add(0, "OR");
        }
        terms.add("UNDELETED");
        connection.search(terms.toArray());
    }

    @Test
    public void testTooDeepNestedOrSearch() throws Exception {
        int maxNestingInSearchRequest = LC.imap_max_nesting_in_search_request.intValue();
        connection = connectAndSelectInbox();
        List<String>terms = Lists.newArrayList();
        terms.add("HEADER");
        terms.add("Message-ID");
        terms.add("a@eg.com");

        for (int cnt = 0;cnt < (maxNestingInSearchRequest); cnt++) {
            terms.add(0, String.format("b%s@eg.com", cnt));
            terms.add(0, "Message-ID");
            terms.add(0, "HEADER");
            terms.add(0, "OR");
        }
        terms.add("UNDELETED");
        try {
            connection.search(terms.toArray());
            Assert.fail("Expected search to fail due to complexity");
        } catch (CommandFailedException cfe) {
            String es = "parse error: Search query too complex";
            Assert.assertTrue(String.format("Exception '%s' should contain string '%s'", cfe.getMessage(), es),
                    cfe.getMessage().contains(es));
        }
    }

    @Test
    public void testDeepNestedAndSearch() throws Exception {
        int nesting = LC.imap_max_nesting_in_search_request.intValue() - 1;
        connection = connectAndSelectInbox();
        connection.search((Object[]) new String[] {
                StringUtils.repeat("(", nesting) + "ANSWERED UNDELETED" +
                StringUtils.repeat(")", nesting) } );
    }

    @Test
    public void testTooDeepNestedAndSearch() throws Exception {
        int nesting = LC.imap_max_nesting_in_search_request.intValue();
        connection = connectAndSelectInbox();
        try {
            connection.search((Object[]) new String[] {
                    StringUtils.repeat("(", nesting) + "ANSWERED UNDELETED" +
                    StringUtils.repeat(")", nesting) } );
            Assert.fail("Expected search to fail due to complexity");
        } catch (CommandFailedException cfe) {
            String es = "parse error: Search query too complex";
            Assert.assertTrue(String.format("Exception '%s' should contain string '%s'", cfe.getMessage(), es),
                    cfe.getMessage().contains(es));
        }
    }

    /**
     * Noted that when running from RunUnitTests where InterruptableRegex did NOT use an InterruptibleCharSequence
     * this would leave a dangling thread consuming resources long after RunUnitTests had completed.
     */
    @Test
    public void testList93114DOSRegex() throws ServiceException, InterruptedException {
        StringBuilder regexPatt = new StringBuilder();
        for (int cnt = 1;cnt < 64; cnt++) {
            regexPatt.append(".*");
        }
        regexPatt.append(" HELLO");
        checkRegex(regexPatt.toString(), "EMAILED CONTACTS", false, 5000000, true /* expecting regex to take too long */);
    }

    @Test
    public void testList93114OkishRegex() throws ServiceException, InterruptedException {
        StringBuilder regexPatt = new StringBuilder();
        for (int cnt = 1;cnt < 10; cnt++) {
            regexPatt.append(".*");
        }
        regexPatt.append(" HELLO");
        // Takes 3356913 accesses
        checkRegex(regexPatt.toString(), "EMAILED CONTACTS", false, 10000000, false);
    }

    @Test
    public void testList93114StarRegex() throws ServiceException, InterruptedException {
        checkRegex(".*", "EMAILED CONTACTS", true, 1000, false);
    }

    @Test
    public void testList93114EndingACTSRegex() throws ServiceException, InterruptedException {
        checkRegex(".*ACTS", "EMAILED CONTACTS", true, 1000, false);
        checkRegex(".*ACTS", "INBOX", false, 1000, false);
    }

    @Test
    public void testList93114MatchingEmailedContactsRegex() throws ServiceException, InterruptedException {
        String target = "EMAILED CONTACTS";
        checkRegex(target, target, true, 1000, false);
    }

    @Test
    public void testList93114DosWithWildcards() throws Exception {
        connection = connectAndSelectInbox();
        try {
            List<ListData> listResult = connection.list("", "**************** HELLO");
            Assert.assertNotNull(listResult);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.info("Expected CommandFailedException", cfe);
        }
    }

    @Test
    public void testList93114DosWithPercents() throws Exception {
        connection = connectAndSelectInbox();
        try {
            List<ListData> listResult = connection.list("", "%%%%%%%%%%%%%%%% HELLO");
            Assert.assertNotNull(listResult);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.info("Expected CommandFailedException", cfe);
        }
    }

    @Test
    public void testList93114DosStarPercentRepeats() throws Exception {
        connection = connectAndSelectInbox();
        StringBuilder mboxPatt = new StringBuilder();
        for (int cnt = 1;cnt < 50; cnt++) {
            mboxPatt.append("*%");
        }
        mboxPatt.append(" HELLO");
        try {
            List<ListData> listResult = connection.list("", mboxPatt.toString());
            Assert.assertNotNull(listResult);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.info("Expected CommandFailedException", cfe);
        }
    }

    @Test
    public void testListInbox() throws Exception {
        connection = connectAndSelectInbox();
        List<ListData> listResult = connection.list("", "INBOX");
        Assert.assertNotNull(listResult);
        Assert.assertEquals("List result should have this number of entries", 1, listResult.size());
    }

    @Test
    public void testMailfoldersOnlyList() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        String folderName = "newfolder1";
        mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", folderName, ZFolder.View.unknown, ZFolder.Color.DEFAULTCOLOR, null, null);
        Provisioning.getInstance().getLocalServer().setImapDisplayMailFoldersOnly(true);
        connection = connectAndSelectInbox();
        List<ListData> listResult = connection.list("", "*");
        Assert.assertNotNull(listResult);
        Assert.assertTrue("List result should have at least 5  entries", listResult.size() >= 5);
        boolean hasContacts = false;
        boolean hasChats = false;
        boolean hasEmailedContacts = false;
        boolean hasTrash = false;
        boolean hasDrafts = false;
        boolean hasInbox = false;
        boolean hasJunk = false;
        boolean hasSent = false;
        boolean hasUnknown = false;
        for (ListData ld : listResult) {
            if((ld.getMailbox().equalsIgnoreCase("Contacts"))){
        hasContacts = true;
        }
        else if((ld.getMailbox().equalsIgnoreCase("Chats"))){
                hasChats = true;
        }
        else if((ld.getMailbox().equalsIgnoreCase("Emailed Contacts"))){
            hasEmailedContacts = true;
        }
        else if((ld.getMailbox().equalsIgnoreCase("newfolder1"))){
        hasUnknown = true;
        }
        else if((ld.getMailbox().equalsIgnoreCase("Trash"))){
        hasTrash = true;
        }
        else if((ld.getMailbox().equalsIgnoreCase("Drafts"))){
        hasDrafts= true;
        }
        else if((ld.getMailbox().equalsIgnoreCase("Inbox"))){
        hasInbox= true;
        }
        else if((ld.getMailbox().equalsIgnoreCase("Sent"))){
        hasSent= true;
        }
        else if((ld.getMailbox().equalsIgnoreCase("Junk"))){
            hasJunk= true;
        }
        }
        Assert.assertFalse("MailonlyfolderList * contains chats",hasChats);
        Assert.assertFalse("MailonlyfolderList * contains contacts",hasContacts);
        Assert.assertFalse("MailonlyfolderList * contains emailed contacts",hasEmailedContacts);
        Assert.assertTrue("MailonlyfolderList * contains Trash",hasTrash);
        Assert.assertTrue("MailonlyfolderList * contains Drafts ",hasDrafts);
        Assert.assertTrue("MailonlyfolderList * contains Inbox",hasInbox);
        Assert.assertTrue("MailonlyfolderList * contains Sent",hasSent);
        Assert.assertTrue("MailonlyfolderList * contains Junk",hasJunk);
        Assert.assertTrue("MailonlyfolderList * contains unknown sub folders",hasUnknown);
    }
    @Test
    public void testFoldersList() throws Exception {
        connection = connectAndSelectInbox();
        List<ListData> listResult = connection.list("", "*");
        Assert.assertNotNull(listResult);
        Assert.assertTrue("List result should have at least 8 entries. Got " + listResult.size(), listResult.size() >= 8);
        verifyFolderList(listResult);
    }

    @Test
    public void testListContacts() throws Exception {
        connection = connectAndSelectInbox();
        List<ListData> listResult = connection.list("", "*Contacts*");
         Assert.assertNotNull(listResult);
         // 'Contacts' and 'Emailed Contacts'
         Assert.assertTrue("List result should have at least 2 entries. Got " + listResult.size(), listResult.size() >= 2);
         for (ListData le : listResult) {
            Assert.assertTrue(String.format("mailbox '%s' contains 'Contacts'", le.getMailbox()),
                    le.getMailbox().contains("Contacts"));
        }
    }

    @Test
    public void testAppend() throws Exception {
        connection = connectAndSelectInbox();
        Assert.assertTrue(connection.hasCapability("UIDPLUS"));
        Flags flags = Flags.fromSpec("afs");
        Date date = new Date(System.currentTimeMillis());
        Literal msg = message(100000);
        try {
            AppendResult res = connection.append("INBOX", flags, date, msg);
            Assert.assertNotNull(res);
            byte[] b = fetchBody(res.getUid());
            Assert.assertArrayEquals("content mismatch", msg.getBytes(), b);
        } finally {
            msg.dispose();
        }
    }

    @Test
    public void testOverflowAppend() throws Exception {
        connection = connectAndSelectInbox();
        Assert.assertTrue(connection.hasCapability("UIDPLUS"));
        int oldReadTimeout = connection.getConfig().getReadTimeout();
        try {
            connection.setReadTimeout(10);
            ImapRequest req = connection.newRequest(CAtom.APPEND, new MailboxName("INBOX"));
            req.addParam("{"+((long)(Integer.MAX_VALUE)+1)+"+}");
            ImapResponse resp = req.send();
            Assert.assertTrue(resp.isNO() || resp.isBAD());

            req = connection.newRequest(CAtom.APPEND, new MailboxName("INBOX"));
            req.addParam("{"+((long)(Integer.MAX_VALUE)+1)+"}");
            resp = req.send();
            Assert.assertTrue(resp.isNO() || resp.isBAD());
        } finally {
            connection.setReadTimeout(oldReadTimeout);
        }
    }

    @Test
    public void testOverflowNotAppend() throws Exception {
        connection = connectAndSelectInbox();
        int oldReadTimeout = connection.getConfig().getReadTimeout();
        try {
            connection.setReadTimeout(10);
            ImapRequest req = connection.newRequest(CAtom.FETCH, "1:*");
            req.addParam("{"+((long)(Integer.MAX_VALUE)+1)+"+}");
            ImapResponse resp = req.send();
            Assert.assertTrue(resp.isNO() || resp.isBAD());
        } finally {
            connection.setReadTimeout(oldReadTimeout);
        }
    }

    @Test
    public void testAppendNoLiteralPlus() throws Exception {
        connection = connectAndSelectInbox();
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run() throws Exception {
                testAppend();
            }
        });
    }

    @Test
    public void testStoreTags() throws Exception {
        connection = connectAndSelectInbox();
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        List<ZTag> tags = mbox.getAllTags();
        Assert.assertTrue(tags == null || tags.size() == 0);

        String tagName = "T1";
        ZTag tag = mbox.getTag(tagName);
        if (tag == null) {
            tag = mbox.createTag(tagName, Color.blue);
        }
        tags = mbox.getAllTags();
        Assert.assertTrue(tags != null && tags.size() == 1);
        Assert.assertEquals("T1", tags.get(0).getName());

        String folderName = "newfolder1";
        ZFolder folder = mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", folderName, ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);
        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", tag.getId(), System.currentTimeMillis(), simpleMessage("foo1"), true);
        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", "", System.currentTimeMillis(), simpleMessage("foo2"), true);

        MailboxInfo info = connection.select("INBOX");
        Assert.assertTrue("INBOX does not contain expected flag "+tagName, info.getFlags().isSet(tagName));

        Map<Long, MessageData> data = connection.fetch("1:*", "FLAGS");
        Assert.assertEquals(2, data.size());
        Iterator<Long> it = data.keySet().iterator();
        Long seq = it.next();
        Assert.assertTrue("flag not set on first message", data.get(seq).getFlags().isSet(tagName));

        seq = it.next();
        Assert.assertFalse("flag unexpectedly set on second message", data.get(seq).getFlags().isSet(tagName));

        connection.store(seq+"", "+FLAGS", tagName);
        data = connection.fetch(seq+"", "FLAGS");
        Assert.assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        Assert.assertTrue("flag not set after STORE in INBOX", data.get(seq).getFlags().isSet(tagName));

        mbox.addMessage(folder.getId(), "u", "", System.currentTimeMillis(), simpleMessage("bar"), true);
        info = connection.select(folderName);
        Assert.assertFalse(folderName+" contains unexpected flag "+tagName, info.getFlags().isSet(tagName));

        data = connection.fetch("*", "FLAGS");
        Assert.assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        Assert.assertFalse("flag unexpectedly set on message in "+folderName, data.get(seq).getFlags().isSet(tagName));

        connection.store(seq+"", "+FLAGS", tagName);
        data = connection.fetch(seq+"", "FLAGS");
        Assert.assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        Assert.assertTrue("flag not set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName));

        info = connection.select(folderName);
        Assert.assertTrue("old tag not set in new folder", info.getFlags().isSet(tagName));

        String tagName2 = "T2";
        connection.store(seq+"", "+FLAGS", tagName2);
        data = connection.fetch(seq+"", "FLAGS");
        Assert.assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        Assert.assertTrue("flag not set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName));
        Assert.assertTrue("flag not set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName2));

        info = connection.select(folderName);
        Assert.assertTrue("old tag not set in new folder", info.getFlags().isSet(tagName));
        Assert.assertTrue("new tag not set in new folder", info.getFlags().isSet(tagName2));

        tags = mbox.getAllTags(); //should not have created T2 as a visible tag
        Assert.assertTrue(tags != null && tags.size() == 1);
        Assert.assertEquals("T1", tags.get(0).getName());

        String tagName3 = "T3";
        connection.store(seq+"", "FLAGS", tagName3);
        data = connection.fetch(seq+"", "FLAGS");
        Assert.assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        Assert.assertFalse("flag unexpectedly set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName));
        Assert.assertFalse("flag unexpectedly set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName2));
        Assert.assertTrue("flag not set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName3));

        info = connection.select(folderName);
        Assert.assertTrue("new tag not set in new folder", info.getFlags().isSet(tagName3));
        Assert.assertFalse("old tag unexpectedly set in new folder", info.getFlags().isSet(tagName));
        Assert.assertFalse("old tag unexpectedly set in new folder", info.getFlags().isSet(tagName2));

        tags = mbox.getAllTags(); //should not have created T2 or T3 as a visible tag
        Assert.assertTrue(tags != null && tags.size() == 1);
        Assert.assertEquals("T1", tags.get(0).getName());


        connection.store(seq+"", "-FLAGS", tagName3);
        data = connection.fetch(seq+"", "FLAGS");
        Assert.assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        Assert.assertTrue("flags unexpectedly set after STORE on message in "+folderName, data.get(seq).getFlags().isEmpty());

        info = connection.select("INBOX");
        Assert.assertTrue("old tag not set in new folder", info.getFlags().isSet(tagName));
        Assert.assertFalse("new tag unexpectedly set in new folder", info.getFlags().isSet(tagName2));
    }

    private void storeInvalidFlag(String flag, Long seq) throws IOException {
        connection = connectAndSelectInbox();
        try {
            connection.store(seq+"", "FLAGS", flag);
            Assert.fail("server allowed client to set system flag "+flag);
        } catch (CommandFailedException e) {
            //expected
        }

        Map<Long, MessageData> data = connection.fetch(seq+":"+seq, "FLAGS");
        Assert.assertFalse(data.get(seq).getFlags().isSet(flag));
        try {
            connection.store(seq+"", "+FLAGS", flag);
            Assert.fail("server allowed client to set system flag "+flag);
        } catch (CommandFailedException e) {
            //expected
        }
        data = connection.fetch(seq+":"+seq, "FLAGS");
        Assert.assertFalse(data.get(seq).getFlags().isSet(flag));
    }

    @Test
    public void testStoreInvalidSystemFlag() throws Exception {
        connection = connectAndSelectInbox();
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", "", System.currentTimeMillis(), simpleMessage("foo"), true);
        connection.select("INBOX");
        Map<Long, MessageData> data = connection.fetch("1:*", "FLAGS");
        Assert.assertEquals(1, data.size());
        Iterator<Long> it = data.keySet().iterator();
        Long seq = it.next();

        storeInvalidFlag("\\Bulk", seq);
        storeInvalidFlag("\\Unread", seq);
        storeInvalidFlag("\\Forwarded", seq);
    }

    @Test
    public void testStoreTagsDirty() throws Exception {
        connection = connectAndSelectInbox();
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        List<ZTag> tags = mbox.getAllTags();
        Assert.assertTrue(tags == null || tags.size() == 0);

        String tagName = "T1";
        final String tagName2 = "T2";
        ZTag tag = mbox.getTag(tagName);
        if (tag == null) {
            tag = mbox.createTag(tagName, Color.blue);
        }
        tags = mbox.getAllTags();
        Assert.assertTrue(tags != null && tags.size() == 1);
        Assert.assertEquals("T1", tags.get(0).getName());

        String folderName = "newfolder1";
        mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", folderName, ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);
        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", tag.getId(), System.currentTimeMillis(), simpleMessage("foo1"), true);

        MailboxInfo info = connection.select("INBOX");
        Assert.assertTrue("INBOX does not contain expected flag "+tagName, info.getFlags().isSet(tagName));
        Assert.assertFalse("INBOX contain unexpected flag "+tagName2, info.getFlags().isSet(tagName2));

        Map<Long, MessageData> data = connection.fetch("1:*", "FLAGS");
        Assert.assertEquals(1, data.size());
        Iterator<Long> it = data.keySet().iterator();
        Long seq = it.next();
        Assert.assertTrue("flag not set on first message", data.get(seq).getFlags().isSet(tagName));

        ImapRequest req = connection.newRequest("STORE", seq+"", "+FLAGS", tagName2);
        req.setResponseHandler(new ResponseHandler() {
            @Override
            public void handleResponse(ImapResponse res) throws Exception {
                if (res.isUntagged() && res.getCCode() == CAtom.FLAGS) {
                    Flags flags = (Flags) res.getData();
                    Assert.assertTrue(flags.isSet(tagName2));
                }
            }
        });
        req.sendCheckStatus();
    }

    @Test
    public void testAppendTags() throws Exception {
        connection = connectAndSelectInbox();
        Flags flags = Flags.fromSpec("afs");
        String tag1 = "APPENDTAG1"; //new tag; does not exist in mbox
        flags.set(tag1);
        Date date = new Date(System.currentTimeMillis());
        Literal msg = message(10);
        try {
            AppendResult res = connection.append("INBOX", flags, date, msg);
            MessageData data = connection.uidFetch(res.getUid(), "FLAGS");
            Assert.assertTrue(data.getFlags().isSet(tag1));
        } finally {
            msg.dispose();
        }

        //should not have created a visible tag
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        List<ZTag> tags = mbox.getAllTags();
        Assert.assertTrue("APPEND created new visible tag", tags == null || tags.size() == 0);

        //now create a visible tag, add it to a message in inbox then try append to message in different folder
        String tag2 = "APPENDTAG2";
        ZTag tag = mbox.getTag(tag2);
        if (tag == null) {
            tag = mbox.createTag(tag2, Color.blue);
        }
        tags = mbox.getAllTags();
        Assert.assertTrue(tags != null && tags.size() == 1);
        Assert.assertEquals(tag2, tags.get(0).getName());

        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", tag.getId(), System.currentTimeMillis(), simpleMessage("foo1"), true);
        MailboxInfo info = connection.select("INBOX");
        Assert.assertTrue("INBOX does not contain expected flag "+tag2, info.getFlags().isSet(tag2));

        String folderName = "newfolder1";
        mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", folderName, ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);

        info = connection.select(folderName);
        Assert.assertFalse("new tag unexpectedly set in new folder", info.getFlags().isSet(tag2));

        msg = message(10);
        flags = Flags.fromSpec("afs");
        flags.set(tag2);
        try {
            AppendResult res = connection.append(folderName, flags, date, msg);
            MessageData data = connection.uidFetch(res.getUid(), "FLAGS");
            Assert.assertTrue(data.getFlags().isSet(tag2));
        } finally {
            msg.dispose();
        }

        info = connection.select(folderName);
        Assert.assertTrue("new tag not set in new folder", info.getFlags().isSet(tag2));
    }

    private void appendInvalidFlag(String flag) throws IOException {
        connection = connectAndSelectInbox();
        Literal msg = message(10);
        Flags flags = Flags.fromSpec("afs");
        flags.set(flag);
        Date date = new Date(System.currentTimeMillis());
        try {
            connection.append("INBOX", flags, date, msg);
            Assert.fail("server allowed client to set system flag "+flag);
        } catch (CommandFailedException e) {
            //expected
        } finally {
            msg.dispose();
        }
        connection.noop(); //do a no-op so we don't hit max consecutive error limit
    }

    @Test
    public void testAppendInvalidSystemFlag() throws Exception {
        connection = connectAndSelectInbox();
        //basic case - append with new tag
        appendInvalidFlag("\\Bulk");
        appendInvalidFlag("\\Unread");
        appendInvalidFlag("\\Forwarded");
    }

    @Test
    public void testAppendTagsDirty() throws Exception {
        connection = connectAndSelectInbox();
        Flags flags = Flags.fromSpec("afs");
        final String tag1 = "NEWDIRTYTAG"; //new tag; does not exist in mbox
        MailboxInfo info = connection.select("INBOX");
        Assert.assertFalse("INBOX contains unexpected flag "+tag1, info.getFlags().isSet(tag1));

        flags.set(tag1);
        Date date = new Date(System.currentTimeMillis());
        Literal msg = message(10);
        try {
            ImapRequest req = connection.newRequest("APPEND", "INBOX", flags, date, msg);
            req.setResponseHandler(new ResponseHandler() {
                @Override
                public void handleResponse(ImapResponse res) throws Exception {
                    if (res.isUntagged() && res.getCCode() == CAtom.FLAGS) {
                        Flags flags = (Flags) res.getData();
                        Assert.assertTrue(flags.isSet(tag1));
                    }
                }
            });
            req.sendCheckStatus();
        } finally {
            msg.dispose();
        }

    }

    @Test
    public void testCatenateSimple() throws Exception {
        connection = connectAndSelectInbox();
        Assert.assertTrue(connection.hasCapability("CATENATE"));
        Assert.assertTrue(connection.hasCapability("UIDPLUS"));
        String part1 = simpleMessage("test message");
        String part2 = "more text\r\n";
        AppendMessage am = new AppendMessage(
            null, null, literal(part1), literal(part2));
        AppendResult res = connection.append("INBOX", am);
        connection.select("INBOX");
        byte[] body = fetchBody(res.getUid());
        Assert.assertArrayEquals("content mismatch", bytes(part1 + part2), body);
    }

    @Test
    public void testCatenateSimpleNoLiteralPlus() throws Exception {
        connection = connectAndSelectInbox();
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run() throws Exception {
                testCatenateSimple();
            }
        });
    }

    @Test
    public void testCatenateUrl() throws Exception {
        connection = connectAndSelectInbox();
        Assert.assertTrue(connection.hasCapability("CATENATE"));
        Assert.assertTrue(connection.hasCapability("UIDPLUS"));
        String msg1 = simpleMessage("test message");
        AppendResult res1 = connection.append("INBOX", null, null, literal(msg1));
        String s1 = "first part\r\n";
        String s2 = "second part\r\n";
        String msg2 = msg1 + s1 + s2;
        AppendMessage am = new AppendMessage(
            null, null, url("INBOX", res1), literal(s1), literal(s2));
        AppendResult res2 = connection.append("INBOX", am);
        connection.select("INBOX");
        byte[] b2 = fetchBody(res2.getUid());
        Assert.assertArrayEquals("content mismatch", bytes(msg2), b2);
    }

    @Test
    public void testMultiappend() throws Exception {
        connection = connectAndSelectInbox();
        Assert.assertTrue(connection.hasCapability("MULTIAPPEND"));
        Assert.assertTrue(connection.hasCapability("UIDPLUS"));
        AppendMessage msg1 = new AppendMessage(null, null, literal("test 1"));
        AppendMessage msg2 = new AppendMessage(null, null, literal("test 2"));
        AppendResult res = connection.append("INBOX", msg1, msg2);
        Assert.assertNotNull(res);
        Assert.assertEquals("expecting 2 uids", 2, res.getUids().length);
    }

    @Test
    public void testSubscribe() throws IOException, ServiceException {
        connection = connectAndSelectInbox();
        String folderName = "TestImap-testSubscribe";
        TestUtil.createFolder(TestUtil.getZMailbox(USER), folderName);
        List<ListData> listResult = connection.lsub("", "*");
        Assert.assertNotNull(listResult);
        Assert.assertEquals("Should have 0 subscriptions at this point", 0, listResult.size());
        try {
            connection.subscribe(folderName);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        Assert.assertNotNull(listResult);
        Assert.assertEquals(1, listResult.size());
        Assert.assertTrue("Should be subscribed to " + folderName + ". Instead got " + listResult.get(0).getMailbox(), folderName.equalsIgnoreCase(listResult.get(0).getMailbox()));
    }

    @Test
    public void testSubscribeNested() throws IOException, ServiceException {
        connection = connectAndSelectInbox();
        String folderName = "TestImap-testSubscribeNested";
        ZFolder folder = TestUtil.createFolder(TestUtil.getZMailbox(USER),Integer.toString(Mailbox.ID_FOLDER_INBOX), folderName);
        List<ListData> listResult = connection.lsub("", "*");
        Assert.assertNotNull(listResult);
        Assert.assertEquals("Should have 0 subscriptions before subscribing", 0, listResult.size());
        try {
            connection.subscribe(folder.getPath());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        Assert.assertNotNull(listResult);
        Assert.assertEquals("Should have 1 subscription after subscribing", 1, listResult.size());
        Assert.assertTrue("Should be subscribed to " + folder.getPath().substring(1) + ". Instead got " + listResult.get(0).getMailbox(), folder.getPath().substring(1).equalsIgnoreCase(listResult.get(0).getMailbox()));
    }

    @Test
    public void testUnSubscribe() throws IOException, ServiceException {
        connection = connectAndSelectInbox();
        String folderName = "TestImap-testUnSubscribe";
        TestUtil.createFolder(TestUtil.getZMailbox(USER), folderName);
        List<ListData> listResult = connection.lsub("", "*");
        Assert.assertNotNull(listResult);
        Assert.assertEquals("Should have 0 subscriptions before subscribing", 0, listResult.size());
        try {
            connection.subscribe(folderName);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        Assert.assertNotNull(listResult);
        Assert.assertEquals("Should have 1 subscription after subscribing", 1, listResult.size());
        Assert.assertTrue("Should be subscribed to " + folderName + ". Instead got " + listResult.get(0).getMailbox(), folderName.equalsIgnoreCase(listResult.get(0).getMailbox()));
        try {
            connection.unsubscribe(folderName);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        Assert.assertNotNull(listResult);
        Assert.assertEquals("Should have 0 subscriptions after unsubscribing", 0, listResult.size());
    }

    @Test
    public void testMultiappendNoLiteralPlus() throws Exception {
        connection = connectAndSelectInbox();
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run() throws Exception {
                testMultiappend();
            }
        });
    }

    @Test
    public void testCreate() throws Exception {
        connection = connectAndSelectInbox();
        String folderName = "TestImap-testCreate";
        Assert.assertFalse(connection.exists(folderName));
        connection.create(folderName);
        Assert.assertTrue(connection.exists(folderName));

    }

    private String url(String mbox, AppendResult res) {
        return String.format("/%s;UIDVALIDITY=%d/;UID=%d",
                             mbox, res.getUidValidity(), res.getUid());
    }

    private static Literal literal(String s) {
        return new Literal(bytes(s));
    }

    private static byte[] bytes(String s) {
        try {
            return s.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UTF8 encoding not supported");
        }
        return null;
    }

    private byte[] fetchBody(long uid) throws IOException {
        MessageData md = connection.uidFetch(uid, "(BODY.PEEK[])");
        Assert.assertNotNull("message not found", md);
        Assert.assertEquals(uid, md.getUid());
        Body[] bs = md.getBodySections();
        Assert.assertNotNull(bs);
        Assert.assertEquals(1, bs.length);
        return bs[0].getImapData().getBytes();
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

    private static String simpleMessage(String text) {
        return "Return-Path: dac@zimbra.com\r\n" +
            "Date: Fri, 27 Feb 2004 15:24:43 -0800 (PST)\r\n" +
            "From: dac <dac@zimbra.com>\r\n" +
            "To: bozo <bozo@foo.com>\r\n" +
            "Subject: Foo foo\r\n\r\n" + text + "\r\n";
    }

    private void withLiteralPlus(boolean lp, RunnableTest test) throws Exception {
        ImapConfig config = connection.getImapConfig();
        boolean oldLp = config.isUseLiteralPlus();
        config.setUseLiteralPlus(lp);
        try {
            test.run();
        } finally {
            config.setUseLiteralPlus(oldLp);
        }
    }

    private static interface RunnableTest {
        void run() throws Exception;
    }

    public static void amain(String[] args) throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestImapImport.class);
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
            Assert.assertFalse("mail-only folderList contains Chats", hasChats);
            Assert.assertFalse("mail-only folderList contains Contacts", hasContacts);
            Assert.assertFalse("mail-only folderList contains Emailed Contacts", hasEmailedContacts);
        } else {
            Assert.assertTrue("folderList * does not contain Chats", hasChats);
            Assert.assertTrue("folderList * does not contain Contacts", hasContacts);
            Assert.assertTrue("folderList * does not contain Emailed Contacts", hasEmailedContacts);
        }
        Assert.assertTrue("folderList * does not contain Trash", hasTrash);
        Assert.assertTrue("folderList * does not contain Drafts ", hasDrafts);
        Assert.assertTrue("folderList * does not contain Inbox", hasInbox);
        Assert.assertTrue("folderList * does not contain Sent", hasSent);
        Assert.assertTrue("folderList * does not contain Junk", hasJunk);
    }

    private void checkRegex(String regexPatt, String target, Boolean expected, int maxAccesses, Boolean timeoutOk) {
        try {
            Pattern patt = Pattern.compile(regexPatt);
            AccessBoundedRegex re = new AccessBoundedRegex(patt, maxAccesses);
            Assert.assertEquals(String.format("matching '%s' against pattern '%s'", target, patt),
                    expected, new Boolean(re.matches(target)));
        } catch (AccessBoundedRegex.TooManyAccessesToMatchTargetException se) {
            Assert.assertTrue("Throwing exception considered OK", timeoutOk);
        }
    }
}
