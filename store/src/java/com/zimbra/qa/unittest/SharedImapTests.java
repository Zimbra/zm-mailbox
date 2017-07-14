package com.zimbra.qa.unittest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZTag;
import com.zimbra.client.ZTag.Color;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.AccessBoundedRegex;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.imap.AppendMessage;
import com.zimbra.cs.mailclient.imap.AppendResult;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.BodyStructure;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.CopyResult;
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
import com.zimbra.cs.service.formatter.VCard;

/**
 * Definitions of tests used from {@Link TestLocalImapShared} and {@Link TestRemoteImapShared}
 */
public abstract class SharedImapTests extends ImapTestBase {

    private void doSelectShouldFail(ImapConnection conn, String folderName) throws IOException {
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

    private MailboxInfo doSelectShouldSucceed(ImapConnection conn, String folderName) throws IOException {
        MailboxInfo mbInfo = null;
        try {
            mbInfo = conn.select(folderName);
            assertNotNull(String.format("return MailboxInfo for 'SELECT %s'", folderName), mbInfo);
            ZimbraLog.test.debug("return MailboxInfo for 'SELECT %s' - %s", folderName, mbInfo);
        } catch (CommandFailedException cfe) {
            fail(String.format("'SELECT %s' failed with '%s'", folderName, cfe.getError()));
        }
        return mbInfo;
    }

    private void doSelectShouldFail(String folderName) throws IOException {
        doSelectShouldFail(connection, folderName);
    }

    private MailboxInfo doSelectShouldSucceed(String folderName) throws IOException {
        return doSelectShouldSucceed(connection, folderName);
    }

    private void doRenameShouldFail(String origFolderName, String newFolderName) throws IOException {
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

    private void doRenameShouldSucceed(String origFolderName, String newFolderName) throws IOException {
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

    @Test(timeout=100000)
    public void testListFolderContents() throws IOException, ServiceException, MessagingException {
        String folderName = "SharedImapTests-testOpenFolder";
        String subject = "SharedImapTests-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);
        connection = connect();
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
                assertNotNull("part type should not be null", part.getType());
                assertNotNull("part subType should not be null", part.getSubtype());
            }
        } else {
            assertNotNull("Body structure type", bs.getType());
            assertNotNull("Body structure sub-type", bs.getSubtype());
        }
        Body[] body = md.getBodySections();
        assertNotNull("body sections should not be null", body);
        assertEquals("expecting one body section. Got " + body.length, 1, body.length);
    }

    @Test(timeout=100000)
    public void testListFolderContentsEnvelope() throws IOException, ServiceException, MessagingException {
        String folderName = "SharedImapTests-testOpenFolder";
        String subject = "SharedImapTests-testMessage";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(zmbox, folderName);
        TestUtil.addMessage(zmbox, subject, folder.getId(), null);
        connection = connect();
        connection.login(PASS);
        connection.select(folderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE)");
        assertEquals("Size of map returned by fetch", 1, mdMap.size());
        MessageData md = mdMap.values().iterator().next();
        assertNotNull("MessageData should not be null", md);
        Envelope env = md.getEnvelope();
        assertNotNull("Envelope should not be null", env);
        assertEquals("Subject from envelope is wrong", subject, env.getSubject());
        assertNull("Internal date was NOT requested and should be NULL", md.getInternalDate());
        BodyStructure bs = md.getBodyStructure();
        assertNull("Body Structure was not requested and should be NULL", bs);
        Body[] body = md.getBodySections();
        assertNull("body sections were not requested and should be null", body);
    }

    @Test(timeout=100000)
    public void testListContactsContents() throws IOException, ServiceException, MessagingException {
        //create a contact
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        Map<String, String> attrs = new HashMap<String, String>();
        String contactName = "testListContactsContents";
        attrs.put("fullName", contactName);
        zmbox.createContact(Integer.toString(Mailbox.ID_FOLDER_CONTACTS), null, attrs);

        //connect to IMAP
        String folderName = "Contacts";
        connection = connect();
        connection.login(PASS);
        connection.select(folderName);
        //fetch
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE BODY BODY.PEEK[])");

        //verify
        assertEquals("Size of map returned by fetch", 1, mdMap.size());
        MessageData md = mdMap.values().iterator().next();
        assertNotNull("MessageData should not be null", md);
        Envelope env = md.getEnvelope();
        assertNotNull("Envelope should not be null", env);
        BodyStructure bs = md.getBodyStructure();
        assertNotNull("Body Structure should not be null", bs);
        Body[] body = md.getBodySections();
        assertNotNull("body sections should not be null", body);
        assertEquals("Expecting 1 body section. Found " + body.length, 1, body.length);
        assertEquals("Envelope subject is wrong", contactName, env.getSubject());
        assertEquals("Body type should be TEXT", "TEXT", bs.getType());
        assertEquals("Body subtype should be X-VCARD", "X-VCARD", bs.getSubtype());

        //fetch one contact
        List<Long> uids = connection.getUids("1:*");
        assertNotNull("uids should not be null", uids);
        assertEquals("expecting to find 1 UID", 1, uids.size());
        byte[] b = getBody(fetchMessage(uids.get(0)));
        assertNotNull("fetched body should not be null", b);
        List<VCard> cards = VCard.parseVCard(new String(b, MimeConstants.P_CHARSET_UTF8));
        assertNotNull("parsed vcards list should not be null", cards);
        assertEquals("expecting to find 1 Vcard", 1, cards.size());
        assertNotNull("parsed vcard should not be null", cards.get(0));
        assertEquals("VCArd's full name is wrong", contactName, cards.get(0).fn);
    }

    @Test(timeout=100000)
    public void testIdleNotification() throws IOException, ServiceException, MessagingException {
        final ImapConnection connection1 = connect();
        connection1.login(PASS);
        connection1.select("INBOX");
        ImapConnection connection2 = connect();
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
                assertNotNull("Result of APPEND call should not be null", res);
            } finally {
                msg.dispose();
            }

            try {
                doneSignal.await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Wait interrupted. ");
            }
            assertTrue("Connection is not idling when it should be", connection1.isIdling());
            connection1.stopIdle();
            assertFalse("Connection is idling when it should NOT be", connection1.isIdling());
            MailboxInfo mboxInfo = connection1.getMailboxInfo();
            assertEquals("Connection was not notified of correct number of existing items", 1, mboxInfo.getExists());
            assertEquals("Connection was not notified of correct number of recent items", 1, mboxInfo.getRecent());
        } finally {
            connection1.close();
            connection2.close();
        }
    }

    @Test(timeout=100000)
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

    @Test(timeout=100000)
    public void testNotSearch() throws Exception {
        connection = connectAndSelectInbox();
        connection.search((Object[]) new String[] { "NOT SEEN"} );
        connection.search((Object[]) new String[] { "NOT NOT SEEN"} );
        connection.search((Object[]) new String[] { "NOT NOT NOT SEEN"} );
    }

    @Test(timeout=100000)
    public void testAndSearch() throws Exception {
        connection = connectAndSelectInbox();
        connection.search((Object[]) new String[] { "HEADER Message-ID z@eg"} );
        connection.search((Object[]) new String[] { "HEADER Message-ID z@eg UNDELETED"} );
        connection.search((Object[]) new String[] { "ANSWERED HEADER Message-ID z@eg UNDELETED"} );
    }

    @Test(timeout=100000)
    public void testBadOrSearch() throws Exception {
        connection = connectAndSelectInbox();
        try {
            connection.search((Object[]) new String[] { "OR ANSWERED" } );
            fail("search succeeded in spite of invalid syntax");
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.debug("Got this exception", cfe);
            String es = "SEARCH failed: parse error: unexpected end of line; expected ' '";
            assertTrue(String.format("Exception '%s' should contain string '%s'", cfe.getMessage(), es),
                    cfe.getMessage().contains(es));
        }
    }

    @Test(timeout=100000)
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

    @Test(timeout=100000)
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

    @Test(timeout=100000)
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
            fail("Expected search to fail due to complexity");
        } catch (CommandFailedException cfe) {
            String es = "parse error: Search query too complex";
            assertTrue(String.format("Exception '%s' should contain string '%s'", cfe.getMessage(), es),
                    cfe.getMessage().contains(es));
        }
    }

    @Test(timeout=100000)
    public void testDeepNestedAndSearch() throws Exception {
        int nesting = LC.imap_max_nesting_in_search_request.intValue() - 1;
        connection = connectAndSelectInbox();
        connection.search((Object[]) new String[] {
                StringUtils.repeat("(", nesting) + "ANSWERED UNDELETED" +
                StringUtils.repeat(")", nesting) } );
    }

    @Test(timeout=100000)
    public void testTooDeepNestedAndSearch() throws Exception {
        int nesting = LC.imap_max_nesting_in_search_request.intValue();
        connection = connectAndSelectInbox();
        try {
            connection.search((Object[]) new String[] {
                    StringUtils.repeat("(", nesting) + "ANSWERED UNDELETED" +
                    StringUtils.repeat(")", nesting) } );
            fail("Expected search to fail due to complexity");
        } catch (CommandFailedException cfe) {
            String es = "parse error: Search query too complex";
            assertTrue(String.format("Exception '%s' should contain string '%s'", cfe.getMessage(), es),
                    cfe.getMessage().contains(es));
        }
    }

    /**
     * Noted that when running from RunUnitTests where InterruptableRegex did NOT use an InterruptibleCharSequence
     * this would leave a dangling thread consuming resources long after RunUnitTests had completed.
     */
    @Test(timeout=100000)
    public void testList93114DOSRegex() throws ServiceException, InterruptedException {
        StringBuilder regexPatt = new StringBuilder();
        for (int cnt = 1;cnt < 64; cnt++) {
            regexPatt.append(".*");
        }
        regexPatt.append(" HELLO");
        checkRegex(regexPatt.toString(), "EMAILED CONTACTS", false, 5000000, true /* expecting regex to take too long */);
    }

    @Test(timeout=100000)
    public void testList93114OkishRegex() throws ServiceException, InterruptedException {
        StringBuilder regexPatt = new StringBuilder();
        for (int cnt = 1;cnt < 10; cnt++) {
            regexPatt.append(".*");
        }
        regexPatt.append(" HELLO");
        // Takes 3356913 accesses
        checkRegex(regexPatt.toString(), "EMAILED CONTACTS", false, 10000000, false);
    }

    @Test(timeout=100000)
    public void testList93114StarRegex() throws ServiceException, InterruptedException {
        checkRegex(".*", "EMAILED CONTACTS", true, 1000, false);
    }

    @Test(timeout=100000)
    public void testList93114EndingACTSRegex() throws ServiceException, InterruptedException {
        checkRegex(".*ACTS", "EMAILED CONTACTS", true, 1000, false);
        checkRegex(".*ACTS", "INBOX", false, 1000, false);
    }

    @Test(timeout=100000)
    public void testList93114MatchingEmailedContactsRegex() throws ServiceException, InterruptedException {
        String target = "EMAILED CONTACTS";
        checkRegex(target, target, true, 1000, false);
    }

    @Test(timeout=100000)
    public void testList93114DosWithWildcards() throws Exception {
        connection = connectAndSelectInbox();
        try {
            List<ListData> listResult = connection.list("", "**************** HELLO");
            assertNotNull("list result should not be null", listResult);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.info("Expected CommandFailedException", cfe);
        }
    }

    @Test(timeout=100000)
    public void testList93114DosWithPercents() throws Exception {
        connection = connectAndSelectInbox();
        try {
            List<ListData> listResult = connection.list("", "%%%%%%%%%%%%%%%% HELLO");
            assertNotNull("list result should not be null", listResult);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.info("Expected CommandFailedException", cfe);
        }
    }

    @Test(timeout=100000)
    public void testList93114DosStarPercentRepeats() throws Exception {
        connection = connectAndSelectInbox();
        StringBuilder mboxPatt = new StringBuilder();
        for (int cnt = 1;cnt < 50; cnt++) {
            mboxPatt.append("*%");
        }
        mboxPatt.append(" HELLO");
        try {
            List<ListData> listResult = connection.list("", mboxPatt.toString());
            assertNotNull("list result should not be null", listResult);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.info("Expected CommandFailedException", cfe);
        }
    }

    @Test(timeout=100000)
    public void testListInbox() throws Exception {
        connection = connectAndSelectInbox();
        List<ListData> listResult = connection.list("", "INBOX");
        assertNotNull("list result should not be null", listResult);
        assertEquals("List result should have this number of entries", 1, listResult.size());
    }

    @Test(timeout=100000)
    public void testMailfoldersOnlyList() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        String folderName = "newfolder1";
        mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", folderName, ZFolder.View.unknown, ZFolder.Color.DEFAULTCOLOR, null, null);
        Provisioning.getInstance().getLocalServer().setImapDisplayMailFoldersOnly(true);
        flushCacheIfNecessary();
        connection = connectAndSelectInbox();
        List<ListData> listResult = connection.list("", "*");
        assertNotNull("list result should not be null", listResult);
        assertTrue("List result should have at least 5  entries", listResult.size() >= 5);
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
        assertFalse("MailonlyfolderList * contains chats",hasChats);
        assertFalse("MailonlyfolderList * contains contacts",hasContacts);
        assertFalse("MailonlyfolderList * contains emailed contacts",hasEmailedContacts);
        assertTrue("MailonlyfolderList * contains Trash",hasTrash);
        assertTrue("MailonlyfolderList * contains Drafts ",hasDrafts);
        assertTrue("MailonlyfolderList * contains Inbox",hasInbox);
        assertTrue("MailonlyfolderList * contains Sent",hasSent);
        assertTrue("MailonlyfolderList * contains Junk",hasJunk);
        assertTrue("MailonlyfolderList * contains unknown sub folders",hasUnknown);
    }
    @Test(timeout=100000)
    public void testFoldersList() throws Exception {
        connection = connectAndSelectInbox();
        List<ListData> listResult = connection.list("", "*");
        assertNotNull("list result should not be null", listResult);
        assertTrue("List result should have at least 8 entries. Got " + listResult.size(), listResult.size() >= 8);
        verifyFolderList(listResult);
    }

    @Test(timeout=100000)
    public void testListContacts() throws Exception {
        connection = connectAndSelectInbox();
        List<ListData> listResult = connection.list("", "*Contacts*");
         assertNotNull("list result should not be null", listResult);
         // 'Contacts' and 'Emailed Contacts'
         assertTrue("List result should have at least 2 entries. Got " + listResult.size(), listResult.size() >= 2);
         for (ListData le : listResult) {
            assertTrue(String.format("mailbox '%s' contains 'Contacts'", le.getMailbox()),
                    le.getMailbox().contains("Contacts"));
        }
    }

    private void doAppend(ImapConnection connection) throws Exception {
        assertTrue("expecting UIDPLUS capability", connection.hasCapability("UIDPLUS"));
        Date date = new Date(System.currentTimeMillis());
        Literal msg = message(100000);
        try {
            AppendResult res = connection.append("INBOX", null, date, msg);
            assertNotNull("result of append command should not be null", res);
            MessageData md = fetchMessage(res.getUid());
            byte[] b = getBody(md);
            assertArrayEquals("content mismatch", msg.getBytes(), b);
        } finally {
            msg.dispose();
        }
    }

    @Test(timeout=100000)
    public void testAppend() throws Exception {
        connection = connectAndSelectInbox();
        doAppend(connection);
    }

    @Test(timeout=100000)
    public void testAppendAndCount() throws Exception {
        connection = connectAndSelectInbox();
        Date date = new Date(System.currentTimeMillis());
        Literal msg = message(100000);
        try {
            MailboxInfo mi = connection.status("Sent", "MESSAGES", "UIDNEXT", "UIDVALIDITY", "UNSEEN", "HIGHESTMODSEQ");
            long oldCount = mi.getExists();
            AppendResult res = connection.append("SENT", null, date, msg);
            assertNotNull("result of append command should not be null", res);
            mi = connection.status("Sent", "MESSAGES", "UIDNEXT", "UIDVALIDITY", "UNSEEN", "HIGHESTMODSEQ");
            long newCount = mi.getExists();
            assertEquals("message count should have increased by one", oldCount, newCount - 1);
        } finally {
            msg.dispose();
        }
    }

    @Test(timeout=100000)
    public void testAppendFlags() throws Exception {
        connection = connectAndSelectInbox();
        assertTrue("expecting UIDPLUS capability", connection.hasCapability("UIDPLUS"));
        Flags flags = Flags.fromSpec("afs");
        Date date = new Date(System.currentTimeMillis());
        Literal msg = message(100000);
        try {
            AppendResult res = connection.append("INBOX", flags, date, msg);
            assertNotNull("result of append command should not be null", res);
            MessageData md = fetchMessage(res.getUid());
            Flags msgFlags = md.getFlags();
            assertTrue("expecting isAnswered flag", msgFlags.isAnswered());
            assertTrue("expecting isFlagged flag", msgFlags.isFlagged());
            assertTrue("expecting isSeen flag", msgFlags.isSeen());
            byte[] b = getBody(md);
            assertArrayEquals("content mismatch", msg.getBytes(), b);
        } finally {
            msg.dispose();
        }
    }

    @Test(timeout=100000)
    public void testOverflowAppend() throws Exception {
        connection = connectAndSelectInbox();
        assertTrue(connection.hasCapability("UIDPLUS"));
        int oldReadTimeout = connection.getConfig().getReadTimeout();
        try {
            connection.setReadTimeout(10);
            ImapRequest req = connection.newRequest(CAtom.APPEND, new MailboxName("INBOX"));
            req.addParam("{"+((long)(Integer.MAX_VALUE)+1)+"+}");
            ImapResponse resp = req.send();
            assertTrue("response should be NO or BAD", resp.isNO() || resp.isBAD());

            req = connection.newRequest(CAtom.APPEND, new MailboxName("INBOX"));
            req.addParam("{"+((long)(Integer.MAX_VALUE)+1)+"}");
            resp = req.send();
            assertTrue("response should be NO or BAD", resp.isNO() || resp.isBAD());
        } finally {
            connection.setReadTimeout(oldReadTimeout);
        }
    }

    @Test(timeout=100000)
    public void testOverflowNotAppend() throws Exception {
        connection = connectAndSelectInbox();
        int oldReadTimeout = connection.getConfig().getReadTimeout();
        try {
            connection.setReadTimeout(10);
            ImapRequest req = connection.newRequest(CAtom.FETCH, "1:*");
            req.addParam("{"+((long)(Integer.MAX_VALUE)+1)+"+}");
            ImapResponse resp = req.send();
            assertTrue("response should be NO or BAD", resp.isNO() || resp.isBAD());
        } finally {
            connection.setReadTimeout(oldReadTimeout);
        }
    }

    @Test(timeout=100000)
    public void testAppendNoLiteralPlus() throws Exception {
        connection = connectAndSelectInbox();
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run(ImapConnection connection) throws Exception {
                doAppend(connection);
            }
        });
    }

    @Test(timeout=100000)
    public void testZCS1781() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        TestUtil.addMessage(mbox, "test for ZCS-1781");
        connection = connectAndSelectInbox();
        final AtomicReference<MessageData> storeResponse = new AtomicReference<MessageData>();
        final CountDownLatch doneSignal = new CountDownLatch(1);
        connection.store("1", "FLAGS", "(\\Deleted)", new ResponseHandler() {
            @Override
            public void handleResponse(ImapResponse res) {
                storeResponse.set((MessageData)(res.getData()));
                doneSignal.countDown();
            }
        });
        try {
            doneSignal.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Wait interrupted. ");
        }
        MessageData data = storeResponse.get();
        assertNotNull("data in IMAP response should not be null", data);
        Flags flags = data.getFlags();
        assertNotNull("flags in IMAP response should not be null", flags);
        assertTrue("should have \\Deleted flag", flags.isDeleted());
    }

    @Test(timeout=100000)
    public void testStoreTags() throws Exception {
        connection = connectAndSelectInbox();
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        List<ZTag> tags = mbox.getAllTags();
        assertTrue(tags == null || tags.size() == 0);

        String tagName = "T1";
        ZTag tag = mbox.getTag(tagName);
        if (tag == null) {
            tag = mbox.createTag(tagName, Color.blue);
        }
        tags = mbox.getAllTags();
        assertTrue(tags != null && tags.size() == 1);
        assertEquals("T1", tags.get(0).getName());

        String folderName = "newfolder1";
        ZFolder folder = mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", folderName, ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);
        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", tag.getId(), System.currentTimeMillis(), simpleMessage("foo1"), true);
        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", "", System.currentTimeMillis(), simpleMessage("foo2"), true);

        MailboxInfo info = connection.select("INBOX");
        assertTrue("INBOX does not contain expected flag "+tagName, info.getFlags().isSet(tagName));

        Map<Long, MessageData> data = connection.fetch("1:*", "FLAGS");
        assertEquals(2, data.size());
        Iterator<Long> it = data.keySet().iterator();
        Long seq = it.next();
        assertTrue("flag not set on first message", data.get(seq).getFlags().isSet(tagName));

        seq = it.next();
        assertFalse("flag unexpectedly set on second message", data.get(seq).getFlags().isSet(tagName));

        connection.store(seq+"", "+FLAGS", tagName);
        data = connection.fetch(seq+"", "FLAGS");
        assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        assertTrue("flag not set after STORE in INBOX", data.get(seq).getFlags().isSet(tagName));

        mbox.addMessage(folder.getId(), "u", "", System.currentTimeMillis(), simpleMessage("bar"), true);
        info = connection.select(folderName);
        assertFalse(folderName+" contains unexpected flag "+tagName, info.getFlags().isSet(tagName));

        data = connection.fetch("*", "FLAGS");
        assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        assertFalse("flag unexpectedly set on message in "+folderName, data.get(seq).getFlags().isSet(tagName));

        connection.store(seq+"", "+FLAGS", tagName);
        data = connection.fetch(seq+"", "FLAGS");
        assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        assertTrue("flag not set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName));

        info = connection.select(folderName);
        assertTrue("old tag not set in new folder", info.getFlags().isSet(tagName));

        String tagName2 = "T2";
        connection.store(seq+"", "+FLAGS", tagName2);
        data = connection.fetch(seq+"", "FLAGS");
        assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        assertTrue("flag not set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName));
        assertTrue("flag not set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName2));

        info = connection.select(folderName);
        assertTrue("old tag not set in new folder", info.getFlags().isSet(tagName));
        assertTrue("new tag not set in new folder", info.getFlags().isSet(tagName2));

        tags = mbox.getAllTags(); //should not have created T2 as a visible tag
        assertTrue(tags != null && tags.size() == 1);
        assertEquals("T1", tags.get(0).getName());

        String tagName3 = "T3";
        connection.store(seq+"", "FLAGS", tagName3);
        data = connection.fetch(seq+"", "FLAGS");
        assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        assertFalse("flag unexpectedly set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName));
        assertFalse("flag unexpectedly set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName2));
        assertTrue("flag not set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName3));

        info = connection.select(folderName);
        assertTrue("new tag not set in new folder", info.getFlags().isSet(tagName3));
        assertFalse("old tag unexpectedly set in new folder", info.getFlags().isSet(tagName));
        assertFalse("old tag unexpectedly set in new folder", info.getFlags().isSet(tagName2));

        tags = mbox.getAllTags(); //should not have created T2 or T3 as a visible tag
        assertTrue(tags != null && tags.size() == 1);
        assertEquals("T1", tags.get(0).getName());


        connection.store(seq+"", "-FLAGS", tagName3);
        data = connection.fetch(seq+"", "FLAGS");
        assertEquals(1, data.size());
        seq = data.keySet().iterator().next();
        assertFalse("flag unexpectedly set after STORE on message in "+folderName, data.get(seq).getFlags().isSet(tagName3));

        info = connection.select("INBOX");
        assertTrue("old tag not set in new folder", info.getFlags().isSet(tagName));
        assertFalse("new tag unexpectedly set in new folder", info.getFlags().isSet(tagName2));
    }

    private void storeInvalidFlag(String flag, Long seq) throws IOException {
        connection = connectAndSelectInbox();
        try {
            connection.store(seq+"", "FLAGS", flag);
            fail("server allowed client to set system flag "+flag);
        } catch (CommandFailedException e) {
            //expected
        }

        Map<Long, MessageData> data = connection.fetch(seq+":"+seq, "FLAGS");
        assertFalse(data.get(seq).getFlags().isSet(flag));
        try {
            connection.store(seq+"", "+FLAGS", flag);
            fail("server allowed client to set system flag "+flag);
        } catch (CommandFailedException e) {
            //expected
        }
        data = connection.fetch(seq+":"+seq, "FLAGS");
        assertFalse(data.get(seq).getFlags().isSet(flag));
    }

    @Test(timeout=100000)
    public void testCreateAndRenameFolder() throws IOException, ServiceException, MessagingException {
        String origFolderName = "SharedImapTests-originalFolderName";
        String newFolderName = "SharedImapTests-newFolderName";
        connection = connect();
        connection.login(PASS);
        connection.create(origFolderName);
        MailboxInfo origMbInfo = connection.select(origFolderName);
        assertNotNull(String.format("return MailboxInfo for 'SELECT %s'", origFolderName), origMbInfo);
        ZimbraLog.test.debug("return MailboxInfo for 'SELECT %s' - %s", origFolderName, origMbInfo);
        doRenameShouldSucceed(origFolderName, newFolderName);
        doSelectShouldFail(origFolderName);
        doSelectShouldSucceed(newFolderName);
        doRenameShouldFail(origFolderName, newFolderName);
    }

    @Test(timeout=100000)
    public void testNonExistentFolder() throws IOException, ServiceException, MessagingException {
        String nonExistentFolderName = "SharedImapTests-NonExistentFolder";
        connection = connect();
        connection.login(PASS);
        try {
            connection.select(nonExistentFolderName);
            fail(String.format("'SELECT %s succeeded when it shouldn't have'", nonExistentFolderName));
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            String expected = "SELECT failed";
            assertTrue(String.format("CommandFailedException error should contain '%s', was '%s'", err, expected),
                    err.contains(expected));
        }
    }

    @Test(timeout=100000)
    public void testRenameNonExistentFolder() throws IOException, ServiceException, MessagingException {
        String nonExistentFolderName = "SharedImapTests-nonExistentFolderName";
        String newFolderName = "SharedImapTests-newFolderName";
        connection = connect();
        connection.login(PASS);
        try {
            connection.rename(nonExistentFolderName, newFolderName);
            fail(String.format("'RENAME %s %s succeeded when it shouldn't have'", nonExistentFolderName, newFolderName));
        } catch (CommandFailedException cfe) {
            String err = cfe.getError();
            String expected = "RENAME failed";
            assertTrue(String.format("CommandFailedException error should contain '%s', was '%s'", err, expected),
                    err.contains(expected));
        }
        doSelectShouldFail(nonExistentFolderName);
        doSelectShouldFail(newFolderName);
    }

    @Test(timeout=100000)
    public void testStoreInvalidSystemFlag() throws Exception {
        connection = connectAndSelectInbox();
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", "", System.currentTimeMillis(), simpleMessage("foo"), true);
        connection.select("INBOX");
        Map<Long, MessageData> data = connection.fetch("1:*", "FLAGS");
        assertEquals(1, data.size());
        Iterator<Long> it = data.keySet().iterator();
        Long seq = it.next();

        storeInvalidFlag("\\Bulk", seq);
        storeInvalidFlag("\\Unread", seq);
        storeInvalidFlag("\\Forwarded", seq);
    }

    @Test(timeout=100000)
    public void testStoreTagsDirty() throws Exception {
        connection = connectAndSelectInbox();
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        List<ZTag> tags = mbox.getAllTags();
        assertTrue(tags == null || tags.size() == 0);

        String tagName = "T1";
        final String tagName2 = "T2";
        ZTag tag = mbox.getTag(tagName);
        if (tag == null) {
            tag = mbox.createTag(tagName, Color.blue);
        }
        tags = mbox.getAllTags();
        assertTrue(tags != null && tags.size() == 1);
        assertEquals("T1", tags.get(0).getName());

        String folderName = "newfolder1";
        mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", folderName, ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);
        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", tag.getId(), System.currentTimeMillis(), simpleMessage("foo1"), true);

        MailboxInfo info = connection.select("INBOX");
        assertTrue("INBOX does not contain expected flag "+tagName, info.getFlags().isSet(tagName));
        assertFalse("INBOX contain unexpected flag "+tagName2, info.getFlags().isSet(tagName2));

        Map<Long, MessageData> data = connection.fetch("1:*", "FLAGS");
        assertEquals(1, data.size());
        Iterator<Long> it = data.keySet().iterator();
        Long seq = it.next();
        assertTrue("flag not set on first message", data.get(seq).getFlags().isSet(tagName));

        ImapRequest req = connection.newRequest("STORE", seq+"", "+FLAGS", tagName2);
        req.setResponseHandler(new ResponseHandler() {
            @Override
            public void handleResponse(ImapResponse res) throws Exception {
                if (res.isUntagged() && res.getCCode() == CAtom.FLAGS) {
                    Flags flags = (Flags) res.getData();
                    assertTrue(flags.isSet(tagName2));
                }
            }
        });
        req.sendCheckStatus();
    }

    @Test(timeout=100000)
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
            assertTrue(data.getFlags().isSet(tag1));
        } finally {
            msg.dispose();
        }

        //should not have created a visible tag
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        List<ZTag> tags = mbox.getAllTags();
        assertTrue("APPEND created new visible tag", tags == null || tags.size() == 0);

        //now create a visible tag, add it to a message in inbox then try append to message in different folder
        String tag2 = "APPENDTAG2";
        ZTag tag = mbox.getTag(tag2);
        if (tag == null) {
            tag = mbox.createTag(tag2, Color.blue);
        }
        tags = mbox.getAllTags();
        assertTrue(tags != null && tags.size() == 1);
        assertEquals(tag2, tags.get(0).getName());

        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", tag.getId(), System.currentTimeMillis(), simpleMessage("foo1"), true);
        MailboxInfo info = connection.select("INBOX");
        assertTrue("INBOX does not contain expected flag "+tag2, info.getFlags().isSet(tag2));

        String folderName = "newfolder1";
        mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", folderName, ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);
        info = connection.select(folderName);
        assertFalse("new tag unexpectedly set in new folder", info.getFlags().isSet(tag2));

        msg = message(10);
        flags = Flags.fromSpec("afs");
        flags.set(tag2);
        try {
            AppendResult res = connection.append(folderName, flags, date, msg);
            MessageData data = connection.uidFetch(res.getUid(), "FLAGS");
            assertTrue(data.getFlags().isSet(tag2));
        } finally {
            msg.dispose();
        }

        info = connection.select(folderName);
        assertTrue("new tag not set in new folder", info.getFlags().isSet(tag2));
    }

    private void appendInvalidFlag(String flag) throws IOException {
        connection = connectAndSelectInbox();
        Literal msg = message(10);
        Flags flags = Flags.fromSpec("afs");
        flags.set(flag);
        Date date = new Date(System.currentTimeMillis());
        try {
            connection.append("INBOX", flags, date, msg);
            fail("server allowed client to set system flag "+flag);
        } catch (CommandFailedException e) {
            //expected
        } finally {
            msg.dispose();
        }
        connection.noop(); //do a no-op so we don't hit max consecutive error limit
    }

    @Test(timeout=100000)
    public void testAppendInvalidSystemFlag() throws Exception {
        connection = connectAndSelectInbox();
        //basic case - append with new tag
        appendInvalidFlag("\\Bulk");
        appendInvalidFlag("\\Unread");
        appendInvalidFlag("\\Forwarded");
    }

    @Test(timeout=100000)
    public void testAppendTagsDirty() throws Exception {
        connection = connectAndSelectInbox();
        Flags flags = Flags.fromSpec("afs");
        final String tag1 = "NEWDIRTYTAG"; //new tag; does not exist in mbox
        MailboxInfo info = connection.select("INBOX");
        assertFalse("INBOX contains unexpected flag "+tag1, info.getFlags().isSet(tag1));

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
                        assertTrue(flags.isSet(tag1));
                    }
                }
            });
            req.sendCheckStatus();
        } finally {
            msg.dispose();
        }

    }

    private void doCatenateSimple(ImapConnection connection) throws Exception {
        assertTrue(connection.hasCapability("CATENATE"));
        assertTrue(connection.hasCapability("UIDPLUS"));
        String part1 = simpleMessage("test message");
        String part2 = "more text\r\n";
        AppendMessage am = new AppendMessage(
            null, null, literal(part1), literal(part2));
        AppendResult res = connection.append("INBOX", am);
        connection.select("INBOX");
        byte[] body = getBody(fetchMessage(res.getUid()));
        assertArrayEquals("content mismatch", bytes(part1 + part2), body);
    }

    @Test(timeout=100000)
    public void testCatenateSimple() throws Exception {
        connection = connectAndSelectInbox();
        doCatenateSimple(connection);
    }

    @Test(timeout=100000)
    public void testCatenateSimpleNoLiteralPlus() throws Exception {
        connection = connectAndSelectInbox();
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run(ImapConnection connection) throws Exception {
                doCatenateSimple(connection);
            }
        });
    }

    @Test(timeout=100000)
    public void testCatenateUrl() throws Exception {
        connection = connectAndSelectInbox();
        assertTrue(connection.hasCapability("CATENATE"));
        assertTrue(connection.hasCapability("UIDPLUS"));
        String msg1 = simpleMessage("test message");
        AppendResult res1 = connection.append("INBOX", null, null, literal(msg1));
        String s1 = "first part\r\n";
        String s2 = "second part\r\n";
        String msg2 = msg1 + s1 + s2;
        AppendMessage am = new AppendMessage(
            null, null, url("INBOX", res1), literal(s1), literal(s2));
        AppendResult res2 = connection.append("INBOX", am);
        connection.select("INBOX");
        byte[] b2 = getBody(fetchMessage(res2.getUid()));
        assertArrayEquals("content mismatch", bytes(msg2), b2);
    }

    private void doMultiappend(ImapConnection connection) throws Exception {
        assertTrue(connection.hasCapability("MULTIAPPEND"));
        assertTrue(connection.hasCapability("UIDPLUS"));
        AppendMessage msg1 = new AppendMessage(null, null, literal("test 1"));
        AppendMessage msg2 = new AppendMessage(null, null, literal("test 2"));
        AppendResult res = connection.append("INBOX", msg1, msg2);
        assertNotNull(res);
        assertEquals("expecting 2 uids", 2, res.getUids().length);
    }

    @Test(timeout=100000)
    public void testMultiappend() throws Exception {
        connection = connectAndSelectInbox();
        doMultiappend(connection);
    }

    @Test(timeout=100000)
    public void testSubscribe() throws IOException, ServiceException {
        connection = connectAndSelectInbox();
        String folderName = "TestImap-testSubscribe";
        TestUtil.createFolder(TestUtil.getZMailbox(USER), folderName);
        List<ListData> listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        assertEquals("Should have 0 subscriptions at this point", 0, listResult.size());
        try {
            connection.subscribe(folderName);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        assertEquals(1, listResult.size());
        assertTrue("Should be subscribed to " + folderName + ". Instead got " + listResult.get(0).getMailbox(), folderName.equalsIgnoreCase(listResult.get(0).getMailbox()));
    }

    @Test(timeout=100000)
    public void testUidCopy() throws IOException, ServiceException {
        String folderName = testInfo.getMethodName();
        TestUtil.createFolder(TestUtil.getZMailbox(USER), folderName);
        connection = connectAndSelectInbox();
        AppendMessage msg1 = new AppendMessage(null, null, literal(testInfo.getMethodName() + " msg 1"));
        AppendMessage msg2 = new AppendMessage(null, null, literal(testInfo.getMethodName() + " msg 2"));
        AppendResult appendRes = connection.append("INBOX", msg1, msg2);
        long[] uids = appendRes.getUids();
        assertNotNull("AppendResult - getUids() value null", uids);
        assertEquals("AppendResult - getUids() length", 2, uids.length);

        String seq = String.format("%s:%s", uids[0], uids[1]);
        CopyResult copyRes = null;
        try {
            copyRes = connection.uidCopy(seq, folderName);
        } catch (Exception e) {
            ZimbraLog.test.error("Failure from UID COPY", e);
            fail("Failure from UID COPY " + e.getMessage());
            return; // keep Eclipse happy
        }
        assertNotNull("CopyResult is null", copyRes);
        long[] fromUids = copyRes.getFromUids();
        assertNotNull("CopyResult - getFromUids() value null", fromUids);
        assertEquals("CopyResult - getFromUids() length", 2, fromUids.length);
        long[] toUids = copyRes.getToUids();
        assertNotNull("CopyResult - getToUids() value null", toUids);
        assertEquals("CopyResult - getToUids() length", 2, toUids.length);
    }

    @Test(timeout=100000)
    public void testSubscribeNested() throws IOException, ServiceException {
        connection = connectAndSelectInbox();
        String folderName = "TestImap-testSubscribeNested";
        ZFolder folder = TestUtil.createFolder(TestUtil.getZMailbox(USER),Integer.toString(Mailbox.ID_FOLDER_INBOX), folderName);
        List<ListData> listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        assertEquals("Should have 0 subscriptions before subscribing", 0, listResult.size());
        try {
            connection.subscribe(folder.getPath());
        } catch (Exception e) {
            fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        assertEquals("Should have 1 subscription after subscribing", 1, listResult.size());
        assertTrue("Should be subscribed to " + folder.getPath().substring(1) + ". Instead got " + listResult.get(0).getMailbox(), folder.getPath().substring(1).equalsIgnoreCase(listResult.get(0).getMailbox()));
    }

    @Test(timeout=100000)
    public void testUnSubscribe() throws IOException, ServiceException {
        connection = connectAndSelectInbox();
        String folderName = "TestImap-testUnSubscribe";
        TestUtil.createFolder(TestUtil.getZMailbox(USER), folderName);
        List<ListData> listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        assertEquals("Should have 0 subscriptions before subscribing", 0, listResult.size());
        try {
            connection.subscribe(folderName);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        assertEquals("Should have 1 subscription after subscribing", 1, listResult.size());
        assertTrue("Should be subscribed to " + folderName + ". Instead got " + listResult.get(0).getMailbox(), folderName.equalsIgnoreCase(listResult.get(0).getMailbox()));
        try {
            connection.unsubscribe(folderName);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        listResult = connection.lsub("", "*");
        assertNotNull(listResult);
        assertEquals("Should have 0 subscriptions after unsubscribing", 0, listResult.size());
    }

    @Test(timeout=100000)
    public void testMultiappendNoLiteralPlus() throws Exception {
        connection = connectAndSelectInbox();
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run(ImapConnection connection) throws Exception {
                doMultiappend(connection);
            }
        });
    }

    @Test(timeout=100000)
    public void testCreate() throws Exception {
        connection = connectAndSelectInbox();
        String folderName = "TestImap-testCreate";
        assertFalse(connection.exists(folderName));
        connection.create(folderName);
        assertTrue(connection.exists(folderName));

    }

    @Test(timeout=100000)
    public void testCopy() throws IOException {
        connection = connectAndSelectInbox();
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        connection.create("FOO");
        connection.copy("1:3", "FOO");
        connection.select("FOO");
        Map<Long, MessageData> mdMap = connection.fetch("1:3", "(ENVELOPE INTERNALDATE BODY BODY.PEEK[])");
        assertEquals("Size of map returned by fetch", 3, mdMap.size());
    }

    @Test(timeout=100000)
    public void testAppendThrottle() throws Exception {
        connection = connectAndSelectInbox();
        assertTrue(connection.hasCapability("UIDPLUS"));
        Date date = new Date(System.currentTimeMillis());
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < LOOP_LIMIT; i++) {
            Literal msg = message(100000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }

        Literal msg = message(100000);
        try {
            connection.append("INBOX", flags, date, msg);
            fail("expected exception here...");
        } catch (Exception e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        } finally {
            msg.dispose();
        }
    }

    @Test(timeout=100000)
    public void testListThrottle() throws IOException {
        connection = connectAndSelectInbox();
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.list("", "*");
        }

        try {
            connection.list("", "*");
            fail("Expected exception here...");
        } catch (Exception e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testLsubThrottle() throws IOException {
        connection = connectAndSelectInbox();
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.lsub("", "*");
        }

        try {
            connection.lsub("", "*");
            fail("Expected exception here...");
        } catch (Exception e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testXlistThrottle() throws IOException {
        connection = connectAndSelectInbox();
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.newRequest(CAtom.XLIST, new MailboxName(""), new MailboxName("*")).sendCheckStatus();
        }

        try {
            connection.newRequest(CAtom.XLIST, new MailboxName(""), new MailboxName("*")).sendCheckStatus();
            fail("Expected exception here...");
        } catch (Exception e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testCreateThrottle() throws IOException {
        connection = connectAndSelectInbox();
        // can't check exact repeats of create since it gets dropped by
        // imap_max_consecutive_error before imap_throttle_command_limit is reached
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.create("foo" + i);
            if (i % 10 == 0) {
                try {
                    Thread.sleep(250);
                    // sleep a bit so we don't provoke req/sec limits. this is
                    // fuzzy; increase sleep time if this test has sporadic failures
                } catch (InterruptedException e) {
                }
            }
        }

        try {
            connection.create("overthelimit");
            fail("should be over consecutive create limit");
        } catch (CommandFailedException e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testStoreThrottle() throws IOException {
        connection = connectAndSelectInbox();
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.store("1:3", "FLAGS", new String[] { "FOO", "BAR" });
        }

        try {
            connection.store("1:3", "FLAGS", new String[] { "FOO", "BAR" });
            fail("should have been rejected");
        } catch (CommandFailedException e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testExamineThrottle() throws IOException {
        connection = connectAndSelectInbox();
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.examine("INBOX");
        }

        try {
            connection.examine("INBOX");
            fail("should have been rejected");
        } catch (CommandFailedException e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testSelectThrottle() throws IOException {
        connection = connectAndSelectInbox();
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.select("SENT");
        }

        try {
            connection.select("SENT");
            fail("should have been rejected");
        } catch (CommandFailedException e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testFetchThrottle() throws IOException {
        connection = connectAndSelectInbox();
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.fetch(1, new String[] { "FLAGS", "UID" });
        }

        try {
            connection.fetch(1, new String[] { "FLAGS", "UID" });
            fail("should have been rejected");
        } catch (CommandFailedException e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testUIDFetchThrottle() throws IOException {
        connection = connectAndSelectInbox();
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.uidFetch("1:*", new String[] { "FLAGS", "UID" });
        }

        try {
            connection.uidFetch("1:*", new String[] { "FLAGS", "UID" });
            fail("should have been rejected");
        } catch (CommandFailedException e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testCopyThrottle() throws IOException {
        connection = connectAndSelectInbox();
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        connection.create("FOO");
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.copy("1:3", "FOO");
        }

        try {
            connection.copy("1:3", "FOO");
            fail("should have been rejected");
        } catch (CommandFailedException e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testSearchThrottle() throws IOException {
        connection = connectAndSelectInbox();
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.search((Object[]) new String[] { "TEXT", "\"XXXXX\"" });
        }

        try {
            connection.search((Object[]) new String[] { "TEXT", "\"XXXXX\"" });
            fail("should have been rejected");
        } catch (CommandFailedException e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testSortThrottle() throws IOException {
        connection = connectAndSelectInbox();
        Flags flags = Flags.fromSpec("afs");
        for (int i = 0; i < 3; i++) {
            Date date = new Date(System.currentTimeMillis());
            Literal msg = message(1000 + i * 1000);
            try {
                connection.append("INBOX", flags, date, msg);
            } finally {
                msg.dispose();
            }
        }
        for (int i = 0; i < LOOP_LIMIT; i++) {
            connection.newRequest("SORT (DATE REVERSE SUBJECT) UTF-8 ALL").sendCheckStatus();
        }

        try {
            connection.newRequest("SORT (DATE REVERSE SUBJECT) UTF-8 ALL").sendCheckStatus();
            fail("should have been rejected");
        } catch (CommandFailedException e) {
            assertTrue("expecting connection to be closed", connection.isClosed());
        }
    }

    @Test(timeout=100000)
    public void testCreateFolder() throws Exception {
        // test that a folder created by a non-IMAP client can be immediately selected by an IMAP client
        String folderName = "newFolder";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        connection = connect();
        connection.login(PASS);
        TestUtil.createFolder(zmbox, folderName);
        MailboxInfo info = connection.select(folderName);
        assertEquals(folderName, info.getName());
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
            fail("UTF8 encoding not supported");
        }
        return null;
    }

    @Test(timeout=100000)
    public void listSharedFolderViaHome() throws ServiceException, IOException {
        TestUtil.createAccount(SHAREE);
        connection = connectAndSelectInbox();
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        connection.create(sharedFolderName);
        connection.setacl(sharedFolderName, SHAREE, "lrswickxteda");
        String underSharedFolderName = String.format("%s/subFolder", sharedFolderName);
        connection.create(underSharedFolderName);
        connection.logout();
        connection = null;
        String remFolder = String.format("/home/%s/%s", USER, sharedFolderName);
        String underRemFolder = String.format("%s/subFolder", remFolder);
        String homeFilter = String.format("/home/%s", USER);
        otherConnection = connectAndSelectInbox(SHAREE);
        List<ListData> listResult;
        String ref;
        String mailbox;
        doListShouldFail(otherConnection, "/home", "*", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/*", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/fred*", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/*fred", "LIST failed: wildcards not permitted in username");
        doListShouldSucceed(otherConnection, "", "INBOX", 1); // reset zimbraImapMaxConsecutiveError counter
        doListShouldFail(otherConnection, "", "/home/pete*fred", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/*/", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/pete*/", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/pete*fred/", "LIST failed: wildcards not permitted in username");
        doListShouldSucceed(otherConnection, "", "INBOX", 1); // reset zimbraImapMaxConsecutiveError counter
        doListShouldFail(otherConnection, "", "/home/*/INBOX", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/pete*/INBOX", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/pete*fred/INBOX", "LIST failed: wildcards not permitted in username");

        //  LIST "" "/home/user/sharedFolderName"
        listResult = doListShouldSucceed(otherConnection, "", remFolder, 1);

        // 'LIST "/home" "user"' - should get:
        //      * LIST (\NoSelect) "/" "/home/user"
        listResult = doListShouldSucceed(otherConnection, "/home", USER, 1);

        // 'LIST "/home" "user/*"'
        ref = "/home";
        mailbox = USER + "/*";
        listResult = doListShouldSucceed(otherConnection, ref, mailbox, 2);
        assertEquals(String.format(
                "'%s' mailbox not in result of 'list \"%s\" \"%s\"'", remFolder, ref, mailbox),
                remFolder, listResult.get(0).getMailbox());
        assertEquals(String.format(
                "'%s' mailbox not in result of 'list \"%s\" \"%s\"'", underRemFolder, ref, mailbox),
                underRemFolder, listResult.get(1).getMailbox());

        //  LIST "/home/user" "*"
        ref = homeFilter;
        mailbox = "*";
        listResult = doListShouldSucceed(otherConnection, ref, mailbox, 2);
        assertEquals(String.format(
                "'%s' mailbox not in result of 'list \"%s\" \"%s\"'", remFolder, ref, mailbox),
                remFolder, listResult.get(0).getMailbox());
        assertEquals(String.format(
                "'%s' mailbox not in result of 'list \"%s\" \"%s\"'", underRemFolder, ref, mailbox),
                underRemFolder, listResult.get(1).getMailbox());

        // 'LIST "/home" "user/INBOX"'
        ref = "/home";
        mailbox = USER + "/INBOX";
        listResult = doListShouldSucceed(otherConnection, ref, mailbox, 0);

        //  LIST "/home/user" "sharedFolderName"
        ref = homeFilter;
        mailbox = sharedFolderName;
        listResult = doListShouldSucceed(otherConnection, homeFilter, sharedFolderName, 1);
        assertEquals(String.format(
                "'%s' mailbox not in result of 'list \"%s\" \"%s\"'", remFolder, ref, mailbox),
                remFolder, listResult.get(0).getMailbox());

        //  LIST "/home/user" "sharedFolderName/subFolder"
        ref = homeFilter;
        mailbox = underSharedFolderName;
        listResult = doListShouldSucceed(otherConnection, homeFilter, underSharedFolderName, 1);
        assertEquals(String.format(
                "'%s' mailbox not in result of 'list \"%s\" \"%s\"'", underRemFolder, ref, mailbox),
                underRemFolder, listResult.get(0).getMailbox());

        otherConnection.logout();
        otherConnection = null;
    }

    @Test(timeout=100000)
    public void listMountpoint() throws ServiceException, IOException {
        TestUtil.createAccount(SHAREE);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        String remoteFolderPath = "/" + sharedFolderName;
        TestUtil.createFolder(mbox, remoteFolderPath);
        String mountpointName = String.format("%s's %s-shared", USER, testId);
        TestUtil.createMountpoint(mbox, remoteFolderPath, shareeZmbox, mountpointName);
        otherConnection = connectAndLogin(SHAREE);
        List<ListData> listResult;
        //  LIST "" "mountpointName"
        listResult = doListShouldSucceed(otherConnection, "", mountpointName, 1);
        assertEquals(String.format(
                "'%s' mountpoint not in result of 'list \"\" \"%s\"'", mountpointName, mountpointName),
                mountpointName, listResult.get(0).getMailbox());

        listResult = otherConnection.list("", "*");
        assertNotNull("list result 'list \"\" \"*\"' should not be null", listResult);
        boolean seenIt = false;
        for (ListData listEnt : listResult) {
            if (mountpointName.equals(listEnt.getMailbox())) {
                seenIt = true;
                break;
            }
        }
        assertTrue(String.format("'%s' mountpoint not in result of 'list \"\" \"*\"'", mountpointName), seenIt);
        otherConnection.logout();
        otherConnection = null;
    }

    @Test(timeout=100000)
    public void listMountpointWithSubFolder() throws ServiceException, IOException {
        TestUtil.createAccount(SHAREE);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        ZMailbox userZmbox = TestUtil.getZMailbox(USER);
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        String remoteFolderPath = "/" + sharedFolderName;
        TestUtil.createFolder(userZmbox, remoteFolderPath);
        String mountpointName = String.format("%s's %s-shared", USER, testId);
        TestUtil.createMountpoint(userZmbox, remoteFolderPath, shareeZmbox, mountpointName);
        String subFolder = sharedFolderName + "/subFolder";
        String subMountpoint = mountpointName + "/subFolder";
        TestUtil.createFolder(userZmbox, "/" + subFolder);
        otherConnection = connectAndLogin(SHAREE);

        String searchPatt;
        List<ListData> listResult;

        /* wild card at end should pick up top level and sub-folder */
        searchPatt = mountpointName + "*";
        listResult = otherConnection.list("", searchPatt);
        assertNotNull(String.format( "List result for 'list \"\" \"%s\"'", searchPatt), listResult);
        assertEquals(String.format( "Number of entries in list returned for 'list \"\" \"%s\"'", searchPatt),
                2, listResult.size());
        assertEquals(String.format(
                "'%s' mountpoint not in result of 'list \"\" \"%s\"'", mountpointName, mountpointName),
                mountpointName, listResult.get(0).getMailbox());
        assertEquals(String.format(
                "'%s' mountpoint not in result of 'list \"\" \"%s\"'", subMountpoint, mountpointName),
                subMountpoint, listResult.get(1).getMailbox());

        /* exact match shouldn't pick up sub-folder */
        searchPatt = mountpointName;
        listResult = otherConnection.list("", searchPatt);
        assertNotNull(String.format( "List result for 'list \"\" \"%s\"'", searchPatt), listResult);
        assertEquals(String.format( "Number of entries in list returned for 'list \"\" \"%s\"'", searchPatt),
                1, listResult.size());
        assertEquals(String.format(
                "'%s' mountpoint not in result of 'list \"\" \"%s\"'", mountpointName, mountpointName),
                mountpointName, listResult.get(0).getMailbox());

        /* exact match on sub-folder should pick up just sub-folder */
        listResult = otherConnection.list("", subMountpoint);
        assertNotNull(String.format( "List result for 'list \"\" \"%s\"'", subMountpoint), listResult);
        assertEquals(String.format( "Number of entries in list returned for 'list \"\" \"%s\"'", subMountpoint),
                1, listResult.size());
        assertEquals(String.format(
                "'%s' mountpoint not in result of 'list \"\" \"%s\"'", subMountpoint, subMountpoint),
                subMountpoint, listResult.get(0).getMailbox());

        /* sub-folder should be in list of all folders */
        listResult = otherConnection.list("", "*");
        assertNotNull("list result for 'list \"\" \"*\"' should not be null", listResult);
        boolean seenIt = false;
        for (ListData listEnt : listResult) {
            if (subMountpoint.equals(listEnt.getMailbox())) {
                seenIt = true;
                break;
            }
        }
        assertTrue(String.format("'%s' mountpoint not in result of 'list \"\" \"*\"'", subMountpoint), seenIt);
        otherConnection.logout();
        otherConnection = null;
    }

    /** Mountpoints created in the classic ZWC way where a folder is shared and the share is accepted
     *  do not appear in the main list of folders.  They should however, be available under the /home hierarchy.
     */
    @Test(timeout=100000)
    public void copyFromMountpointUsingHomeNaming() throws IOException, ServiceException, MessagingException {
        TestUtil.createAccount(SHAREE);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        String remoteFolderPath = "/" + sharedFolderName;
        ZFolder remoteFolder = TestUtil.createFolder(mbox, remoteFolderPath);
        String mountpointName = String.format(testId + "-ForSharee");
        TestUtil.createMountpoint(mbox, remoteFolderPath, shareeZmbox, mountpointName);
        String subject = String.format("%s-missiveSubject", testInfo.getMethodName());
        TestUtil.addMessage(mbox, subject, remoteFolder.getId());
        connection = connectAndSelectInbox();
        doSelectShouldSucceed(sharedFolderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE)");
        assertEquals("Size of map returned by fetch", 1, mdMap.size());
        connection.logout();
        connection = null;
        String remFolder = String.format("/home/%s/%s", USER, sharedFolderName);
        String copyToFolder = "INBOX/copy-to";
        otherConnection = connectAndSelectInbox(SHAREE);
        doCopy(otherConnection, shareeZmbox, remFolder, copyToFolder, subject);
    }

    @Test(timeout=100000)
    public void copyFromMountpointUsingMountpointNaming() throws IOException, ServiceException, MessagingException {
        TestUtil.createAccount(SHAREE);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        String remoteFolderPath = "/" + sharedFolderName;
        ZFolder remoteFolder = TestUtil.createFolder(mbox, remoteFolderPath);
        String mountpointName = String.format(testId + "-ForSharee");
        TestUtil.createMountpoint(mbox, remoteFolderPath, shareeZmbox, mountpointName);
        String subject = String.format("%s-missiveSubject", testInfo.getMethodName());
        TestUtil.addMessage(mbox, subject, remoteFolder.getId());
        connection = connectAndSelectInbox();
        doSelectShouldSucceed(sharedFolderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE)");
        assertEquals("Size of map returned by fetch", 1, mdMap.size());
        connection.logout();
        connection = null;
        String copyToFolder = "INBOX/copy-to";
        otherConnection = connectAndSelectInbox(SHAREE);
        doCopy(otherConnection, shareeZmbox, mountpointName, copyToFolder, subject);
    }

    @Test(timeout=100000)
    public void copyFromSharedFolderViaHome() throws IOException, ServiceException, MessagingException {
        TestUtil.createAccount(SHAREE);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        connection = connectAndSelectInbox();
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        connection.create(sharedFolderName);
        connection.setacl(sharedFolderName, SHAREE, "lrswickxteda");
        String subject = String.format("%s-missiveSubject", testInfo.getMethodName());
        String bodyStr = String.format("test message body for %s", testInfo.getMethodName());
        String part1 = simpleMessage(subject, bodyStr);
        String part2 = "more text\r\n";
        AppendMessage am = new AppendMessage(null, null, literal(part1), literal(part2));
        AppendResult res = connection.append(sharedFolderName, am);
        assertNotNull(String.format("Append result to folder %s", sharedFolderName), res);
        doSelectShouldSucceed(sharedFolderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE)");
        assertEquals("Size of map returned by fetch", 1, mdMap.size());
        connection.logout();
        connection = null;
        String remFolder = String.format("/home/%s/%s", USER, sharedFolderName);
        String copyToFolder = "INBOX/copy-to";
        otherConnection = connectAndSelectInbox(SHAREE);
        doCopy(otherConnection, shareeZmbox, remFolder, copyToFolder, subject);
    }

    private void doCopy(ImapConnection imapConn, ZMailbox shareeZmbox, String fromFolderName,
            String toFolderName, String srcMsgSubject)
    throws IOException, ServiceException, MessagingException {
        imapConn.list("", "*");
        imapConn.create(toFolderName);
        // This loop is to create some distance between the IDs in the from and to mailboxes
        for (int cnt =1;cnt < 10;cnt++) {
            TestUtil.addMessage(shareeZmbox, String.format("inbox msg %s", cnt));
        }
        doSelectShouldSucceed(imapConn, fromFolderName);
        CopyResult copyResult = imapConn.copy("1", toFolderName);
        assertNotNull("copyResult.getFromUids()", copyResult.getFromUids());
        assertNotNull("copyResult.getToUids()", copyResult.getToUids());
        assertEquals("Number of fromUIDs", 1, copyResult.getFromUids().length);
        assertEquals("Number of toUIDs", 1, copyResult.getToUids().length);
        MailboxInfo selectMboxInfo = imapConn.select(toFolderName);
        Map<Long, MessageData> mdMap = imapConn.fetch("1:*", "(ENVELOPE)");
        assertNotNull(String.format("Select result for folder=%s", toFolderName), selectMboxInfo);
        assertEquals("Select result Folder Name folder", toFolderName, selectMboxInfo.getName());
        assertEquals(String.format("Number of exists for folder=%s after copy", toFolderName),
                1, selectMboxInfo.getExists());
        assertEquals("Size of map returned by fetch", 1, mdMap.size());
        MessageData md = mdMap.values().iterator().next();
        assertNotNull("MessageData should not be null", md);
        Envelope env = md.getEnvelope();
        assertNotNull("Envelope should not be null", env);
        assertEquals("Subject from envelope is wrong", srcMsgSubject, env.getSubject());
        assertNull("Internal date was NOT requested and should be NULL", md.getInternalDate());
        BodyStructure bs = md.getBodyStructure();
        assertNull("Body Structure was not requested and should be NULL", bs);
        Body[] body = md.getBodySections();
        assertNull("body sections were not requested and should be null", body);
    }

    @Test
    public void testRenameParentFolder() throws Exception {
        String parentFolder = "parent";
        String childFolder1 = parentFolder + "/child1";
        String childFolder2 = childFolder1 + "/child2";
        connection = connect();
        connection.login(PASS);
        connection.create(childFolder2);
        List<ListData> listResult = connection.list("", "*");
        assertTrue(listContains(listResult, parentFolder));
        assertTrue(listContains(listResult, childFolder1));
        assertTrue(listContains(listResult, childFolder2));
        String newParentFolder = "renamed";
        String newChildFolder1 = newParentFolder + "/child1";
        String newChildFolder2 = newChildFolder1 + "/child2";
        connection.rename(parentFolder, newParentFolder);
        listResult = connection.list("", "*");
        assertTrue(listContains(listResult, newParentFolder));
        assertTrue(listContains(listResult, newChildFolder1));
        assertTrue(listContains(listResult, newChildFolder2));
        assertFalse(listContains(listResult, parentFolder));
        assertFalse(listContains(listResult, childFolder1));
        assertFalse(listContains(listResult, childFolder2));
    }

    private boolean listContains(List<ListData> listData, String folderName) {
        for (ListData ld: listData) {
            if (ld.getMailbox().equalsIgnoreCase(folderName)) {
                return true;
            }
        }
        return false;
    }

    private MessageData fetchMessage(long uid) throws IOException {
        MessageData md = connection.uidFetch(uid, "(FLAGS BODY.PEEK[])");
        assertNotNull("message not found", md);
        assertEquals(uid, md.getUid());
        return md;
    }

    private byte[] getBody(MessageData md) throws IOException {
        Body[] bs = md.getBodySections();
        assertNotNull("body sections should not be NULL", bs);
        assertEquals("expecting 1 body section", 1, bs.length);
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

    private static String simpleMessage(String subject, String text) {
        return "Return-Path: dac@zimbra.com\r\n" +
            "Date: Fri, 27 Feb 2004 15:24:43 -0800 (PST)\r\n" +
            "From: dac <dac@zimbra.com>\r\n" +
            "To: bozo <bozo@foo.com>\r\n" +
            "Subject: " + subject + "\r\n\r\n" + text + "\r\n";
    }

    private static String simpleMessage(String text) {
        return simpleMessage("Foo foo", text);
    }

    private void withLiteralPlus(boolean lp, RunnableTest test) throws Exception {
        ImapConfig config = connection.getImapConfig();
        boolean oldLp = config.isUseLiteralPlus();
        config.setUseLiteralPlus(lp);
        try {
            test.run(connection);
        } finally {
            config.setUseLiteralPlus(oldLp);
        }
    }

    private static interface RunnableTest {
        void run(ImapConnection connection) throws Exception;
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

    private void checkRegex(String regexPatt, String target, Boolean expected, int maxAccesses, Boolean timeoutOk) {
        try {
            Pattern patt = Pattern.compile(regexPatt);
            AccessBoundedRegex re = new AccessBoundedRegex(patt, maxAccesses);
            assertEquals(String.format("matching '%s' against pattern '%s'", target, patt),
                    expected, new Boolean(re.matches(target)));
        } catch (AccessBoundedRegex.TooManyAccessesToMatchTargetException se) {
            assertTrue("Throwing exception considered OK", timeoutOk);
        }
    }

    protected void flushCacheIfNecessary() throws Exception {
        // overridden by tests running against imapd
    }
}
