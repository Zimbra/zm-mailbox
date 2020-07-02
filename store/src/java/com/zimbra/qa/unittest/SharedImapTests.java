package com.zimbra.qa.unittest;

import com.zimbra.client.ZMailbox.OwnerBy;
import com.zimbra.client.ZMailbox.SharedItemBy;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.MessagingException;

import com.zimbra.client.ZGetInfoResult;
import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.google.common.collect.Lists;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZSearchFolder;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZTag;
import com.zimbra.client.ZTag.Color;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.imap.ImapCredentials;
import com.zimbra.cs.imap.ImapPath;
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
import com.zimbra.soap.type.SearchSortBy;
import com.zimbra.client.ZGrant.GranteeType;

/**
 * Definitions of tests used from {@Link TestLocalImapShared} and {@Link TestRemoteImapShared}
 */
@SuppressWarnings("PMD.ExcessiveClassLength")
public abstract class SharedImapTests extends ImapTestBase {

    /* useful mechanism for reporting multiple errors for a single test */
    @Rule
    public ErrorCollector collector= new ErrorCollector();

    @Test(timeout=100000)
    public void imapPath() throws IOException, ServiceException, MessagingException {
        Account userAcct = TestUtil.getAccount(USER);
        assertNotNull("Account object for user", userAcct); // Shuts up PMD complaint about missing asserts
        Account shareeAcct = TestUtil.createAccount(SHAREE);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        /* create a few folders to ensure that the sharee account won't have the same folder IDs
         * present as are used in the shared folders.
         */
        for (int cnt = 0;cnt < 10;cnt++) {
            TestUtil.createFolder(zmbox, "/toplevel-" + cnt);
        }
        String remFolder = String.format("/INBOX/%s-shared", testInfo.getMethodName());
        String underRemFolder = String.format("%s/subFolder", remFolder);
        String otherUserFolder = String.format("/home/%s%s", USER, remFolder);
        String otherUserSubFolder = String.format("/home/%s%s", USER, underRemFolder);
        ZFolder zfolder = TestUtil.createFolder(zmbox, remFolder);
        ZFolder underZfolder = TestUtil.createFolder(zmbox, underRemFolder);
        String mp = String.format("%s's %s-shared", USER, testInfo.getMethodName());
        String subMp = String.format("%s/subFolder", mp);
        TestUtil.createMountpoint(zmbox, remFolder, shareeZmbox, mp);
        ImapCredentials creds = new ImapCredentials(shareeAcct);

        checkImapPath("inbox", creds, "INBOX", false /* usingReferent */,
                Integer.parseInt(ZFolder.ID_INBOX), shareeAcct.getId());
        checkImapPath(mp, creds, mp, true /* usingReferent */,
                zfolder.getFolderItemIdentifier().id, userAcct.getId());
        checkImapPath(subMp, creds, subMp, true /* usingReferent */,
                underZfolder.getFolderItemIdentifier().id, userAcct.getId());
        checkImapPath(otherUserFolder, creds, otherUserFolder, false /* usingReferent */,
                zfolder.getFolderItemIdentifier().id, userAcct.getId());
        checkImapPath(otherUserSubFolder, creds, otherUserSubFolder, false /* usingReferent */,
                underZfolder.getFolderItemIdentifier().id, userAcct.getId());
    }

    private void checkImapPath(String mboxName, ImapCredentials creds, String expectedPathToString,
            boolean usingReferent, int expectedReferentFolderId, String expectedReferentFolderAcct)
    throws ServiceException {
        ImapPath path = new ImapPath(mboxName, creds);
        path.canonicalize();
        ImapPath referent = path.getReferent();
        assertEquals(String.format("toString() for ImapPath for mailbox '%s'", mboxName),
                expectedPathToString, path.toString());
        if (usingReferent) {
            assertNotSame(
                    String.format("ImapPath=%s and it's getReferent() for mailbox '%s'", path, mboxName),
                    path, referent);
        } else {
            assertSame(
                    String.format("ImapPath=%s and it's getReferent() for mailbox '%s'", path, mboxName),
                    path, referent);
        }
        FolderStore folderForPath = path.getFolder();
        assertEquals(String.format(
                "Folder ID for path.getReferent().getFolder() for mailbox '%s'", mboxName),
                expectedReferentFolderId, folderIdForFolder(folderForPath));
        assertEquals(String.format(
                "Account ID for path.getReferent().getFolder() for mailbox '%s'", mboxName),
                expectedReferentFolderAcct,
                acctIdForFolder(folderForPath));
    }

    private String acctIdForFolder(FolderStore folder) throws ServiceException {
        MailboxStore mbox = folder.getMailboxStore();
        String acctId;
        try {
            acctId = mbox.getAccountId();
        } catch (ServiceException e) {
            acctId = "<unknown>";
        }
        ZimbraLog.test.debug("Account ID = %s for folder %s", acctId, folder);
        return acctId;
    }

    private int folderIdForFolder(FolderStore folder) {
        ZimbraLog.test.debug("Folder ID = %s for folder %s", folder.getFolderIdInOwnerMailbox(), folder);
        return folder.getFolderIdInOwnerMailbox();
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
        byte[] b = getBody(fetchMessage(connection, uids.get(0)));
        assertNotNull("fetched body should not be null", b);
        List<VCard> cards = VCard.parseVCard(new String(b, MimeConstants.P_CHARSET_UTF8));
        assertNotNull("parsed vcards list should not be null", cards);
        assertEquals("expecting to find 1 Vcard", 1, cards.size());
        assertNotNull("parsed vcard should not be null", cards.get(0));
        assertEquals("VCArd's full name is wrong", contactName, cards.get(0).fn);
    }

    protected static class IdleResponseHandler implements ResponseHandler {
        private final AtomicBoolean gotExists = new AtomicBoolean(false);
        private final AtomicBoolean gotRecent = new AtomicBoolean(false);
        private final CountDownLatch doneSignal = new CountDownLatch(1);

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

        public void waitForExpectedSignal(int secs) {
            try {
                doneSignal.await(secs, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Wait interrupted.  RECENT and EXISTS responses not seen yet");
            }
        }
    }

    private void doIdleNotificationCheck(ImapConnection conn1, ImapConnection conn2, String folderName)
            throws IOException {
        // Kick off an IDLE command - which will be processed in another thread until we call stopIdle()
        IdleResponseHandler respHandler = new IdleResponseHandler();
        conn1.idle(respHandler);
        assertTrue("Connection is not idling when it should be", conn1.isIdling());
        doAppend(conn2, folderName, 100, Flags.fromSpec("afs"),
                false /* don't do fetch as affects recent */);
        respHandler.waitForExpectedSignal(10);
        assertTrue("Connection is not idling when it should be", connection.isIdling());
        conn1.stopIdle();
        assertFalse("Connection is idling when it should NOT be", connection.isIdling());
        MailboxInfo mboxInfo = conn1.getMailboxInfo();
        assertEquals("Connection was not notified of correct number of existing items", 1, mboxInfo.getExists());
        assertEquals("Connection was not notified of correct number of recent items", 1, mboxInfo.getRecent());

    }
    @Test(timeout=100000)
    public void idleOnInboxNotification() throws IOException, ServiceException, MessagingException {
        connection = connectAndSelectInbox();
        otherConnection = connectAndLogin(USER);
        String subject = "SharedImapTest-testIdleNotification";
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        assertNotNull("ZMailbox for USER", zmbox);
        TestUtil.addMessage(zmbox, subject, "1", null);
        doIdleNotificationCheck(connection, otherConnection, "INBOX");
    }

    @Test(timeout=100000)
    public void idleOnMountpoint() throws ServiceException, IOException, MessagingException {
        TestUtil.createAccount(SHAREE);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        assertNotNull("ZMailbox for USER", zmbox);
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        String remoteFolderPath = "/" + sharedFolderName;
        TestUtil.createFolder(zmbox, remoteFolderPath);
        String mountpointName = String.format("%s's %s-shared", USER, testId);
        TestUtil.createMountpoint(zmbox, remoteFolderPath, shareeZmbox, mountpointName);
        connection = connectAndLogin(SHAREE);
        otherConnection = connectAndLogin(SHAREE);
        doSelectShouldSucceed(connection, mountpointName);
        doIdleNotificationCheck(connection, otherConnection, mountpointName);
    }

    @Test(timeout=100000)
    public void idleOnFolderViaHome() throws ServiceException, IOException, MessagingException {
        Account shareeAcct = TestUtil.createAccount(SHAREE);
        assertNotNull("Account for SHAREE", shareeAcct);
        connection = connectAndSelectInbox(USER);
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        connection.create(sharedFolderName);
        connection.setacl(sharedFolderName, SHAREE, "lrswickxteda");
        String underSharedFolderName = String.format("%s/subFolder", sharedFolderName);
        connection.create(underSharedFolderName);
        connection.logout();
        connection = null;
        String remFolder = String.format("/home/%s/%s", USER, sharedFolderName);
        connection = connectAndLogin(SHAREE);
        otherConnection = connectAndLogin(SHAREE);
        doSelectShouldSucceed(connection, remFolder);
        doIdleNotificationCheck(connection, otherConnection, remFolder);
    }

    private void statusChecker(String user, String folderName, ZMailbox ownerZmbox, String folderId)
            throws ServiceException, IOException, MessagingException {
        connection = connectAndLogin(user);
        new StatusExecutor(connection).setExists(0).setRecent(0)
                .execShouldSucceed("INBOX", "UIDNEXT", "MESSAGES", "RECENT");
        otherConnection = connectAndLogin(user);
        /* note doAppend does a SELECT of the folder/FETCH of the message to verify that it worked
         * which means that an IMAP session is watching the folder, so the recent count remains
         * 0 for other IMAP sessions until the folder is de-selected or that session closes.
         * At that point, the mailbox is updated to make the RECENT value 0.
         */
        doAppend(otherConnection, folderName, 1, null);
        /* at this point, will definitely be watching the folder, which may not have been the case
         * before the first append - so will execute other code paths.
         */
        doAppend(otherConnection, folderName, 1, null);
        otherConnection.logout();
        otherConnection.close();
        otherConnection = null;
        new StatusExecutor(connection).setExists(2).setRecent(0)
                .execShouldSucceed(folderName, "UIDNEXT", "MESSAGES", "RECENT");
        /* Add a message so that the RECENT count will be > 0 */
        TestUtil.addMessage(ownerZmbox, "Created using ZClient", folderId);
        new StatusExecutor(connection).setExists(3).setRecent(1)
                .execShouldSucceed(folderName, "UIDNEXT", "MESSAGES", "RECENT");
    }

    @Test(timeout=1000000)
    public void statusOnInbox() throws ServiceException, IOException, MessagingException {
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        assertNotNull("ZMailbox for USER", zmbox);
        statusChecker(USER, "INBOX", zmbox, ZFolder.ID_INBOX);
    }

    @Test(timeout=1000000)
    public void statusOnMountpoint() throws ServiceException, IOException, MessagingException {
        TestUtil.createAccount(SHAREE);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        assertNotNull("ZMailbox for USER", zmbox);
        String sharedFolderName = String.format("INBOX/%s", testInfo.getMethodName());
        String remoteFolderPath = "/" + sharedFolderName;
        ZFolder zfolder = TestUtil.createFolder(zmbox, remoteFolderPath);
        String mountpointName = String.format("%s's %s", USER, testId);
        TestUtil.createMountpoint(zmbox, remoteFolderPath, shareeZmbox, mountpointName);
        statusChecker(SHAREE, mountpointName, zmbox, zfolder.getId());
    }

    @Test(timeout=1000000)
    public void statusOnFolderViaHome() throws ServiceException, IOException, MessagingException {
        Account shareeAcct = TestUtil.createAccount(SHAREE);
        assertNotNull("Account for SHAREE", shareeAcct);
        ZMailbox zmbox = TestUtil.getZMailbox(USER);
        assertNotNull("ZMailbox for USER", zmbox);
        String sharedFolderName = String.format("INBOX/%s", testInfo.getMethodName());
        String remoteFolderPath = "/" + sharedFolderName;
        ZFolder zfolder = TestUtil.createFolder(zmbox, remoteFolderPath);
        connection = connectAndSelectInbox(USER);
        connection.setacl(sharedFolderName, SHAREE, "lrswickxteda");
        connection.logout();
        connection = null;
        String remFolder = String.format("/home/%s/%s", USER, sharedFolderName);
        statusChecker(SHAREE, remFolder, zmbox, zfolder.getId());
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
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test(timeout=100000)
    public void testList93114DOSRegex() throws ServiceException, InterruptedException {
        StringBuilder regexPatt = new StringBuilder();
        for (int cnt = 1;cnt < 64; cnt++) {
            regexPatt.append(".*");
        }
        regexPatt.append(" HELLO");
        checkRegex(regexPatt.toString(), "EMAILED CONTACTS", false, 5000000, true /* expecting regex to take too long */);
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
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

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test(timeout=100000)
    public void testList93114StarRegex() throws ServiceException, InterruptedException {
        checkRegex(".*", "EMAILED CONTACTS", true, 1000, false);
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test(timeout=100000)
    public void testList93114EndingACTSRegex() throws ServiceException, InterruptedException {
        checkRegex(".*ACTS", "EMAILED CONTACTS", true, 1000, false);
        checkRegex(".*ACTS", "INBOX", false, 1000, false);
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
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

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test(timeout=100000)
    public void testAppend() throws Exception {
        connection = connectAndSelectInbox();
        doAppend(connection, "INBOX", 100000, null);
    }

    @Test(timeout=100000)
    public void testAppendAndCount() throws Exception {
        connection = connectAndSelectInbox();
        Date date = new Date(System.currentTimeMillis());
        Literal msg = message(100000);
        try {
            MailboxInfo mi;
            mi = new StatusExecutor(connection)
                    .execShouldSucceed("Sent", "MESSAGES", "UIDNEXT", "UIDVALIDITY", "UNSEEN", "HIGHESTMODSEQ");
            long oldCount = mi.getExists();
            AppendResult res = connection.append("SENT", null, date, msg);
            assertNotNull("result of append command should not be null", res);
            mi = new StatusExecutor(connection)
                    .execShouldSucceed("Sent", "MESSAGES", "UIDNEXT", "UIDVALIDITY", "UNSEEN", "HIGHESTMODSEQ");
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
            MessageData md = fetchMessage(connection, res.getUid());
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

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test(timeout=100000)
    public void testAppendNoLiteralPlus() throws Exception {
        connection = connectAndSelectInbox();
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run(ImapConnection conn) throws Exception {
                doAppend(conn, "INBOX", 100000, null);
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

    @Test
    public void testZCS1776() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        TestUtil.addMessage(mbox, "test for ZCS-1776");
        connection = connect(USER);
        connection.login(PASS);
        MailboxInfo info = connection.select("INBOX");
        assertEquals("should have 1 RECENT item in IMAP response", 1L, info.getRecent());
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

    @Test(timeout=100000)
    public void testTagRemovalNotificationsToMultipleConnections() throws Exception {
        String tagToAdd = "Add";
        String tagToRemove = "Remove";

        ZMailbox mbox = TestUtil.getZMailbox(USER);
        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", "", System.currentTimeMillis(), simpleMessage("TagRemoval"), true);

        connection = connectAndSelectInbox();
        // Add both flags to our message
        connection.store("1", "+FLAGS", tagToRemove);
        otherConnection = connectAndSelectInbox();
        connection.store("1", "+FLAGS", tagToAdd);
        // Remove 'tagToRemove' using connection 2
        otherConnection.store("1", "-FLAGS", tagToRemove);
        Map<Long, MessageData> dataOne = connection.fetch("1", "FLAGS");
        Map<Long, MessageData> dataTwo = otherConnection.fetch("1", "FLAGS");

        Long seq = Long.parseLong("1");

        Flags connectionOneFlags = dataOne.get(seq).getFlags();
        Flags connectionTwoFlags = dataTwo.get(seq).getFlags();
        connectionOneFlags.unsetRecent();
        connectionTwoFlags.unsetRecent();

        assertEquals("Flags should be equal on the two connections", connectionOneFlags, connectionTwoFlags);
    }

    @Test(timeout=100000)
    public void testListSubfolders() throws Exception {
        String testFolder = "imaptest1";
        String testSubFolder = "test";
        connection = connectAndLogin(USER);
        otherConnection = connectAndLogin(USER);
        List<ListData> listData = connection.list("", "");
        assertEquals("expecting list response length to be 1", 1, listData.size());
        char delimiter = listData.get(0).getDelimiter();
        connection.create(testFolder + delimiter);
        String testSubFolderPath = String.format("%s%s%s", testFolder, delimiter, testSubFolder);
        connection.create(testSubFolderPath);
        listData = otherConnection.list("", String.format("%s%s%%",testFolder, delimiter));
        assertEquals("expecting to find one subfolder", 1, listData.size());
        String foundFolder = listData.get(0).getMailbox();
        assertEquals(String.format("Expecting to find %s folder in list response", testSubFolderPath), testSubFolderPath, foundFolder);
    }

    @Test(timeout=100000)
    public void testFolderDeletedByOtherConnectionSelected() throws Exception {
        String newFolder = "imaptest1";
        connection = connectAndLogin(USER);
        connection.create(newFolder);
        otherConnection = connectAndLogin(USER);
        otherConnection.select(newFolder);
        assertTrue("Second connection should be in SELECTED state", otherConnection.isSelected());
        assertFalse("First connection should NOT be in SELECTED state", connection.isSelected());
        final CountDownLatch doneSignal = new CountDownLatch(1);
        final AtomicBoolean gotBye = new AtomicBoolean(false);
        //Wait for BYE from IMAP server. Zimbra IMAP client does not detect when connection is dropped by server
        otherConnection.idle(new ResponseHandler() {
            @Override
            public void handleResponse(ImapResponse res) {
                if(res.isBYE()) {
                    gotBye.set(true);
                    doneSignal.countDown();
                }
            }
        });
        assertTrue("Connection is not idling when it should be", otherConnection.isIdling());

        connection.delete(newFolder);
        try {
            doneSignal.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Wait interrupted. ");
        }
        assertTrue("Second connection should have received BYE", gotBye.get());
    }

    @Test(timeout=100000)
    public void testFolderDeletedByOtherConnectionNotSelected() throws Exception {
        String newFolder = "imaptest1";
        connection = connectAndLogin(USER);
        connection.create(newFolder);
        MailboxInfo folderInfo = connection.select(newFolder);
        long oldUIDValidityC1 = folderInfo.getUidValidity();
        otherConnection = connectAndLogin(USER);
        folderInfo  = otherConnection.select(newFolder);
        long oldUIDValidityC2 = folderInfo.getUidValidity();
        assertEquals(String.format("UIDVALIDITY for the old folder should be the same for both connections. C1: %d C2: %d", oldUIDValidityC1, oldUIDValidityC2), oldUIDValidityC2, oldUIDValidityC1);
        otherConnection.close_mailbox();
        //delete and recreate the folder
        connection.delete(newFolder);
        connection.create(newFolder);
        folderInfo = connection.select(newFolder);
        long newUIDValidityC1 = folderInfo.getUidValidity();
        //2d connection should now select the newly recreated folder
        folderInfo  = otherConnection.select(newFolder);
        long newUIDValidityC2 = folderInfo.getUidValidity();
        assertEquals(String.format("UIDVALIDITY for the new folder should be the same for both connections. C1: %d C2: %d", newUIDValidityC1, newUIDValidityC2), newUIDValidityC1, newUIDValidityC2);
        assertTrue("Second connection should be in SELECTED state", otherConnection.isSelected());
        assertTrue("First connection should  be in SELECTED state", connection.isSelected());
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
    public void testSessionFlags() throws Exception {
        connection = connect();
        connection.login(PASS);
        String foldername = String.format("INBOX/%s-append", testId);
        connection.create(foldername);
        connection.select(foldername);
        Literal msg = message(100);
        String junkFlag = "JUNK";
        Flags flags = new Flags();
        flags.set(junkFlag);
        Date date = new Date(System.currentTimeMillis());
        try {
            connection.append(foldername, flags, date, msg);
            MessageData data = connection.fetch(1, "ALL");
            assertTrue("Expecting 'JUNK' session flag", data.getFlags().isSet(junkFlag));
        } finally {
            msg.dispose();
        }

        Literal msg1 = message(101);
        String junkRecordedFlag = "JUNKRECORDED";
        flags = new Flags();
        flags.set(junkRecordedFlag);
        date = new Date(System.currentTimeMillis());
        try {
            connection.append(foldername, flags, date, msg1);
            MessageData data = connection.fetch(2, "ALL");
            assertTrue("Expecting 'JUNKRECORDED' session flag", data.getFlags().isSet(junkRecordedFlag));
            assertFalse("Should not be getting 'JUNK' session flag", data.getFlags().isSet(junkFlag));
        } finally {
            msg.dispose();
        }

        Literal msg2 = message(102);
        String notJunkFlag = "NOTJUNK";
        flags = new Flags();
        flags.set(notJunkFlag);
        date = new Date(System.currentTimeMillis());
        try {
            connection.append(foldername, flags, date, msg2);
            MessageData data = connection.fetch(3, "ALL");
            assertTrue("Expecting 'NOTJUNK' session flag", data.getFlags().isSet(notJunkFlag));
            assertFalse("Should not be getting 'JUNK' session flag", data.getFlags().isSet(junkFlag));
            assertFalse("Should not be getting 'JUNKRECORDED' session flag", data.getFlags().isSet(junkRecordedFlag));
        } finally {
            msg.dispose();
        }
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

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
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
        byte[] body = getBody(fetchMessage(connection, res.getUid()));
        assertArrayEquals("content mismatch", bytes(part1 + part2), body);
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test(timeout=100000)
    public void catenateSimple() throws Exception {
        connection = connectAndSelectInbox();
        doCatenateSimple(connection);
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
    @Test(timeout=100000)
    public void catenateSimpleNoLiteralPlus() throws Exception {
        connection = connectAndSelectInbox();
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run(ImapConnection connection) throws Exception {
                doCatenateSimple(connection);
            }
        });
    }

    private class ImapClientThread
    implements Runnable {
        private final String folderName;
        private Random rand = new Random();

        private ImapClientThread(String fldr) {
            folderName = fldr;
        }

        @Override
        public void run() {
            for (int i = 0; i < 1000; i++) {
                try {
                    ImapConnection imapConn = connectAndLogin(USER);
                    imapConn.select(folderName);
                    imapConn.store("1:*", "+FLAGS", new String[] { "\\Seen" });
                    try {
                        Thread.sleep(rand.nextInt(1000));
                    } catch (InterruptedException e) {
                    }
                    imapConn.select("INBOX");
                    imapConn.select(folderName);
                    imapConn.store("1:*", "-FLAGS", new String[] { "\\Seen" });
                    imapConn.select("INBOX");
                    imapConn.logout();
                    imapConn.close();
                } catch (Exception e) {
                    ZimbraLog.test.error("Problem connecting and selecting %s in thread", folderName, e);
                }
            }
        }
    }

    /** Test created to attempt to repro ZBUG-446 */
    @Test(timeout=1000000)
    @Ignore("Takes a while to run.")
    public void imapFolderReload() throws Exception {
        connection = connectAndLogin(USER);
        String folderName = "INBOX/exercise";
        connection.create(folderName);
        assertTrue(connection.exists(folderName));
        String msg1 = simpleMessage("message to exercise ImapURL");
        connection.append(folderName, null, null, literal(msg1));
        Thread[] threads = new Thread[14];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new ImapClientThread(folderName));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private void doAppendThenOtherAppendReferencingFirst(
            ImapConnection conn, String folder1, String folder2) throws IOException {
        assertTrue(conn.hasCapability("CATENATE"));
        assertTrue(conn.hasCapability("UIDPLUS"));
        String msg1 = simpleMessage("message to exercise ImapURL");
        AppendResult res1 = conn.append(folder1, null, null, literal(msg1));
        String s1 = "first part\r\n";
        String s2 = "second part\r\n";
        String msg2 = msg1 + s1 + s2;
        AppendMessage am = new AppendMessage(null, null, url(folder1, res1), literal(s1), literal(s2));
        /* This append command expands into the following (most of the work being done inside AppendMessage)
         *     APPEND INBOX CATENATE (URL "/INBOX;UIDVALIDITY=1/;UID=257" TEXT {12+}
         *     first part
         *      TEXT {13+}
         *     second part
         *     )
         *
         * The URL in the above ends up being processed by the ImapURL class server side.
         */
        AppendResult res2 = conn.append(folder2, am);
        conn.select(folder2);
        byte[] b2 = getBody(fetchMessage(conn, res2.getUid()));
        String newMsg = new String(b2, "UTF-8");
        assertEquals("Content of message not as expected", msg2, newMsg);
    }

    @Test(timeout=100000)
    public void catenateUrl() throws Exception {
        connection = connectAndSelectInbox();
        assertTrue(connection.hasCapability("CATENATE"));  /* stop PMD warnings about no asserts */
        // Note that with INBOX selected, the server uses the folder cache to provide data
        // from the message identified by the source URL in the catenate.  The next 2 tests
        // exercise the other code path
        doAppendThenOtherAppendReferencingFirst(connection, "INBOX", "INBOX");
    }

    @Test(timeout=100000)
    public void catenateUrlReferencingMP() throws Exception {
        String sharedFolder = "share";
        String mountpoint = "Mountpoint";
        TestUtil.createAccount(SHAREE);
        ZMailbox userZmbox = TestUtil.getZMailbox(USER);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        TestUtil.createMountpoint(userZmbox, "/" + sharedFolder, shareeZmbox, mountpoint);
        connection = connectAndLogin(SHAREE);
        assertTrue(connection.hasCapability("CATENATE"));  /* stop PMD warnings about no asserts */
        // deliberately NOT got either of the involved folders selected to ensure
        // that content is not retrieved from the folder cache - in the remote case, UserServlet
        // is used instead.
        doAppendThenOtherAppendReferencingFirst(connection, mountpoint, "INBOX");
    }

    @Test(timeout=100000)
    public void catenateUrlReferencingOtherUserFolder() throws Exception {
        String sharedFolder = "shared";
        String remFolder = String.format("/home/%s/%s", USER, sharedFolder);
        TestUtil.createAccount(SHAREE);
        TestUtil.createFolder(TestUtil.getZMailbox(USER), sharedFolder);
        connection = connectAndLogin(USER);
        connection.setacl(sharedFolder, SHAREE, "lrswickxteda");
        connection.logout();
        connection = connectAndLogin(SHAREE);
        // deliberately NOT got either of the involved folders selected to ensure
        // that content is not retrieved from the folder cache - in the remote case, UserServlet
        // is used instead.
        doSelectShouldSucceed(connection, "Drafts");
        assertTrue(connection.hasCapability("CATENATE"));  /* stop PMD warnings about no asserts */
        doAppendThenOtherAppendReferencingFirst(connection, remFolder, "INBOX");
    }

    private void doMultiappend(ImapConnection conn) throws Exception {
        assertTrue(conn.hasCapability("MULTIAPPEND"));
        assertTrue(conn.hasCapability("UIDPLUS"));
        AppendMessage msg1 = new AppendMessage(null, null, literal("test 1"));
        AppendMessage msg2 = new AppendMessage(null, null, literal("test 2"));
        AppendResult res = conn.append("INBOX", msg1, msg2);
        assertNotNull("Result of multi-append", res);
        assertEquals("expecting 2 uids", 2, res.getUids().length);
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
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
        String folderName = testId;
        ZFolder folder = TestUtil.createFolder(
                TestUtil.getZMailbox(USER),Integer.toString(Mailbox.ID_FOLDER_INBOX), folderName);
        assertNotNull("Folder object from createFolder", folder);
        doLSubShouldSucceed(connection, "", "*", Lists.newArrayListWithExpectedSize(0), "before subscribe");
        String imapMboxName = folder.getPath().substring(1);
        doSubscribeShouldSucceed(connection, imapMboxName);
        doLSubShouldSucceed(connection, "", "*", Lists.newArrayList(imapMboxName), "after subscribe");
    }

    @Test(timeout=100000)
    public void testUnSubscribe() throws IOException, ServiceException {
        connection = connectAndSelectInbox();
        String folderName = testId;
        ZFolder folder = TestUtil.createFolder(TestUtil.getZMailbox(USER), folderName);
        assertNotNull("Folder object from createFolder", folder);
        doLSubShouldSucceed(connection, "", "*", Lists.newArrayListWithExpectedSize(0), "before subscribe");
        doSubscribeShouldSucceed(connection, folderName);
        doLSubShouldSucceed(connection, "", "*", Lists.newArrayList(folderName), "after subscribe");
        doUnsubscribeShouldSucceed(connection, folderName);
        doLSubShouldSucceed(connection, "", "*", Lists.newArrayListWithExpectedSize(0), "after unsubscribe");
    }

    @Test(timeout=100000)
    public void homeNameSpaceSubscribe() throws ServiceException, IOException, MessagingException {
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        String subFolder = sharedFolderName + "/subFolder";
        ZMailbox userZmbox = TestUtil.getZMailbox(USER);
        ZFolder folder = TestUtil.createFolder(userZmbox, "/" + sharedFolderName);
        assertNotNull("Folder object from createFolder", folder);
        TestUtil.createFolder(userZmbox, "/" + subFolder);
        TestUtil.createAccount(SHAREE);
        connection = connectAndLogin(USER);
        connection.setacl(sharedFolderName, SHAREE, "lrswickxteda");
        connection.logout();
        connection = null;
        String remFolder = String.format("/home/%s/%s", USER, sharedFolderName);
        String underRemFolder = String.format("%s/subFolder", remFolder);
        String homePatt = String.format("/home/%s/*", USER);
        otherConnection = connectAndLogin(SHAREE);
        doLSubShouldSucceed(otherConnection, "", "*",
                Lists.newArrayListWithExpectedSize(0), "before 1st subscribe");
        doSubscribeShouldSucceed(otherConnection, "INBOX");
        doSubscribeShouldSucceed(otherConnection, remFolder);
        doLSubShouldSucceed(otherConnection, "", "*", Lists.newArrayList("INBOX"),
                String.format("after subscribing to INBOX and '%s'", remFolder));
        doLSubShouldSucceed(otherConnection, "", homePatt, Lists.newArrayList(remFolder),
                String.format("after subscribing to INBOX and '%s'", remFolder));
        doSubscribeShouldSucceed(otherConnection, underRemFolder);
        doLSubShouldSucceed(otherConnection, "", homePatt, Lists.newArrayList(remFolder, underRemFolder),
                String.format("after subscribing to INBOX and '%s' and '%s'", remFolder, underRemFolder));
        doUnsubscribeShouldSucceed(otherConnection, remFolder);
        doLSubShouldSucceed(otherConnection, "", homePatt, Lists.newArrayList(underRemFolder),
                String.format("after unsubscribing from '%s'", remFolder));
        doUnsubscribeShouldSucceed(otherConnection, underRemFolder);
        doLSubShouldSucceed(otherConnection, "", homePatt, Lists.newArrayListWithExpectedSize(0),
                String.format("after unsubscribing from '%s'", remFolder));
        doLSubShouldSucceed(otherConnection, "", "*", Lists.newArrayList("INBOX"),
                String.format("after unsubscribing from '%s'", remFolder));
        otherConnection.logout();
        otherConnection = null;
    }

    @Test(timeout=100000)
    public void mountpointSubscribe() throws ServiceException, IOException {
        TestUtil.createAccount(SHAREE);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        assertNotNull("Sharee ZMailbox", shareeZmbox);
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        String remFolder = String.format("/INBOX/%s-shared", testId);
        String underRemFolder = String.format("%s/subFolder", remFolder);
        TestUtil.createFolder(mbox, remFolder);
        TestUtil.createFolder(mbox, underRemFolder);
        String mp = String.format("%s's %s-shared", USER, testId);
        String subMp = String.format("%s/subFolder", mp);
        TestUtil.createMountpoint(mbox, remFolder, shareeZmbox, mp);
        otherConnection = connectAndLogin(SHAREE);
        doLSubShouldSucceed(otherConnection, "", "*",
                Lists.newArrayListWithExpectedSize(0), "before 1st subscribe");
        doSubscribeShouldSucceed(otherConnection, "INBOX");
        doSubscribeShouldSucceed(otherConnection, mp);
        doLSubShouldSucceed(otherConnection, "", "*", Lists.newArrayList("INBOX", mp),
                String.format("after subscribing to INBOX and '%s'", mp));
        doSubscribeShouldSucceed(otherConnection, subMp);
        doLSubShouldSucceed(otherConnection, "", "*", Lists.newArrayList("INBOX", mp, subMp),
                String.format("after subscribing to '%s'", subMp));
        doUnsubscribeShouldSucceed(otherConnection, mp);
        doLSubShouldSucceed(otherConnection, "", "*", Lists.newArrayList("INBOX", subMp),
                String.format("after unsubscribing from '%s'", mp));
        doUnsubscribeShouldSucceed(otherConnection, subMp);
        doLSubShouldSucceed(otherConnection, "", "*", Lists.newArrayList("INBOX"),
                String.format("after unsubscribing from '%s'", subMp));
        otherConnection.logout();
        otherConnection = null;
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
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
        String mboxname = mbox;
        if (mbox.startsWith("/")) {
            /* other user namespace starts with "/home" c.f. e.g. "INBOX".  Strip any leading
             * '/' to avoid '//'. */
            mboxname = mboxname.substring(1);
        }
        return String.format("/%s;UIDVALIDITY=%d/;UID=%d", mboxname, res.getUidValidity(), res.getUid());
    }

    @Test(timeout=100000)
    public void listSharedFolderViaHome() throws ServiceException, IOException {
        Account shareeAcct = TestUtil.createAccount(SHAREE);
        assertNotNull("Account for SHAREE", shareeAcct);
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
        doListShouldFail(otherConnection, "/home", "*", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/*", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/fred*", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/*fred", "LIST failed: wildcards not permitted in username");
        // reset zimbraImapMaxConsecutiveError counter
        doListShouldSucceed(otherConnection, "", "INBOX", Lists.newArrayList("INBOX"), "JUST INBOX #1");
        doListShouldFail(otherConnection, "", "/home/pete*fred", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/*/", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/pete*/", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/pete*fred/", "LIST failed: wildcards not permitted in username");
        // reset zimbraImapMaxConsecutiveError counter
        doListShouldSucceed(otherConnection, "", "INBOX", Lists.newArrayList("INBOX"), "JUST INBOX #2");
        doListShouldFail(otherConnection, "", "/home/*/INBOX", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/pete*/INBOX", "LIST failed: wildcards not permitted in username");
        doListShouldFail(otherConnection, "", "/home/pete*fred/INBOX", "LIST failed: wildcards not permitted in username");

        //  LIST "" "/home/user/sharedFolderName"
        doListShouldSucceed(otherConnection, "", remFolder, Lists.newArrayList(remFolder), "JUST remFolder");

        // 'LIST "/home" "user"' - should get:
        //      * LIST (\NoSelect) "/" "/home/user"
        doListShouldSucceed(otherConnection, "/home", USER,
                Lists.newArrayList(String.format("/home/%s", USER)), "JUST /home/user");

        // 'LIST "/home" "user/*"'
        doListShouldSucceed(otherConnection, "/home", USER + "/*",
                Lists.newArrayList(remFolder, underRemFolder), "all folders for user - 1");

        //  LIST "/home/user" "*"
        doListShouldSucceed(otherConnection, homeFilter, "*",
                Lists.newArrayList(remFolder, underRemFolder), "all folders for user  - 2");

        // 'LIST "/home" "user/INBOX"'
        doListShouldSucceed(otherConnection, "/home", USER + "/INBOX",
                Lists.newArrayList(), "unshared INBOX for user");

        //  LIST "/home/user" "sharedFolderName"
        doListShouldSucceed(otherConnection, homeFilter, sharedFolderName,
                Lists.newArrayList(remFolder), "shared folder for user");

        //  LIST "/home/user" "sharedFolderName/subFolder"
        doListShouldSucceed(otherConnection, homeFilter, underSharedFolderName,
                Lists.newArrayList(underRemFolder), "shared folder for user");

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
        //  LIST "" "mountpointName"
        doListShouldSucceed(otherConnection, "", mountpointName,
                Lists.newArrayList(mountpointName), "mountpoint");

        List<ListData> listResult;
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

    /** Simulate how LIST handles the situation where a user has shared their whole mailbox with a sharee
     * and the sharee has used ZWC to add a shared folder for "All applications".
        "CreateMountpointRequest": [{
            "link": {
                "l": 1, "name": "owner's", "view": "unknown", "zid": "028fdbd8-d5de-483a-a545-11e421b8bd12", "rid": 1
            },
            "_jsns": "urn:zimbraMail"
        }], "_jsns": "urn:zimbra"
     */
    @Test(timeout=100000)
    public void listMountpointForAllApplicationsShare() throws ServiceException, IOException {
        TestUtil.createAccount(SHAREE);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        String sharedFolderName = String.format("%s-shared", testId);
        String remoteFolderPath = "/" + sharedFolderName;
        TestUtil.createFolder(mbox, remoteFolderPath);
        String mountpointName = String.format("%s's", USER, testId);
        ZGetInfoResult remoteInfo = mbox.getAccountInfo(true);
        String folderUserRoot = Integer.toString(Mailbox.ID_FOLDER_USER_ROOT);
        otherConnection = connectAndLogin(SHAREE);
        List<ListData> beforeListResult = otherConnection.list("", "*");
        assertNotNull("list result 'list \"\" \"*\"' should not be null (before share)", beforeListResult);
        mbox.modifyFolderGrant(folderUserRoot, GranteeType.all, null, "rwidx", null);
        shareeZmbox.createMountpoint(folderUserRoot, mountpointName, null, null,
                null, OwnerBy.BY_ID, remoteInfo.getId(), SharedItemBy.BY_ID, folderUserRoot, false);
        List<ListData> listResult;
        listResult = otherConnection.list("", "*");
        assertNotNull("list result 'list \"\" \"*\"' should not be null", listResult);
        boolean seenIt = false;
        for (ListData listEnt : listResult) {
            ZimbraLog.test.info("LIST result has entry '%s'", listEnt.getMailbox());
            if (mountpointName.equals(listEnt.getMailbox())) {
                seenIt = true;
                break;
            }
        }
        assertTrue(String.format("'%s' mountpoint not in result of 'list \"\" \"*\"'", mountpointName), seenIt);
        int extras = listResult.size() - beforeListResult.size();
        /* Extra entries should look something like these - i.e. a top level container and the sharer user's folders
         * under there.
         * LIST (\NoSelect \HasChildren) "/" "testimapviaembeddedlocal-listmountpointforallapplicationsshare-61-user's"
         * LIST (\HasNoChildren) "/" "testimapviaembeddedlocal-listmountpointforallapplicationsshare-61-user's/Chats"
         * LIST (\HasNoChildren) "/" "testimapviaembeddedlocal-listmountpointforallapplicationsshare-61-user's/Contacts"
         * LIST (\HasNoChildren \Drafts) "/" "testimapviaembeddedlocal-listmountpointforallapplicationsshare-61-user's/Drafts"
         * LIST (\HasNoChildren) "/" "testimapviaembeddedlocal-listmountpointforallapplicationsshare-61-user's/Emailed Contacts"
         * LIST (\HasNoChildren) "/" "testimapviaembeddedlocal-listmountpointforallapplicationsshare-61-user's/Inbox"
         * LIST (\NoInferiors \Junk) "/" "testimapviaembeddedlocal-listmountpointforallapplicationsshare-61-user's/Junk"
         * LIST (\HasNoChildren \Sent) "/" "testimapviaembeddedlocal-listmountpointforallapplicationsshare-61-user's/Sent"
         * LIST (\HasNoChildren) "/" "testimapviaembeddedlocal-listmountpointforallapplicationsshare-61-user's/TestImapViaEmbeddedLocal-listMountpointForAllApplicationsShare-61-shared"
         * LIST (\HasNoChildren \Trash) "/" "testimapviaembeddedlocal-listmountpointforallapplicationsshare-61-user's/Trash"
         */
        assertTrue(String.format(
                "'list \"\" \"*\"' response before share had %s entries, after %s entries (diff %s) expected >= 10 new",
                beforeListResult.size(), listResult.size(), extras), extras >= 10);
        otherConnection.logout();
        otherConnection = null;
    }

    private final class SubFolderEnv {
        private final List<String> subjects;
        private final List<String> subFolderSubjects;

        private SubFolderEnv(String sharedFolderName, String subFolder)
                throws ServiceException, IOException, MessagingException {
            ZMailbox userZmbox = TestUtil.getZMailbox(USER);
            String remoteFolderPath = "/" + sharedFolderName;
            ZFolder zFolder = TestUtil.createFolder(userZmbox, remoteFolderPath);
            String subject;
            subject = String.format("%s-MsgInFolder", testInfo.getMethodName());
            subjects = Lists.newArrayList(subject + " 1", subject + " 2");
            ZFolder subZFolder = TestUtil.createFolder(userZmbox, "/" + subFolder);
            subject = String.format("%s-MsgInSubFolder", testInfo.getMethodName());
            subFolderSubjects = Lists.newArrayList(subject + " 1", subject + " 2", subject + " 3");
            TestUtil.addMessage(userZmbox, subjects.get(0), zFolder.getId());
            TestUtil.addMessage(userZmbox, subjects.get(1), zFolder.getId());
            TestUtil.addMessage(userZmbox, subFolderSubjects.get(0), subZFolder.getId());
            TestUtil.addMessage(userZmbox, subFolderSubjects.get(1), subZFolder.getId());
            TestUtil.addMessage(userZmbox, subFolderSubjects.get(2), subZFolder.getId());
        }
    }

    @Test(timeout=100000)
    public void mountpointWithSubFolder() throws ServiceException, IOException, MessagingException {
        List<ListData> listResult;

        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        String subFolder = sharedFolderName + "/subFolder";
        TestUtil.createAccount(SHAREE);
        otherConnection = connectAndLogin(SHAREE);
        listResult = otherConnection.list("", "*");
        List<String> baselineMboxNames = mailboxNames(listResult);

        SubFolderEnv subFolderEnv = new SubFolderEnv(sharedFolderName, subFolder);
        ZMailbox userZmbox = TestUtil.getZMailbox(USER);
        assertNotNull("ZMailbox for USER", userZmbox);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);

        String mountpointName = String.format("%s's %s-shared", USER, testId);
        String subMountpoint = mountpointName + "/subFolder";
        String remoteFolderPath = "/" + sharedFolderName;
        TestUtil.createMountpoint(userZmbox, remoteFolderPath, shareeZmbox, mountpointName);

        /* wild card at end should pick up top level and sub-folder */
        doListShouldSucceed(otherConnection, "", mountpointName + "*",
                Lists.newArrayList(mountpointName, subMountpoint), "wildcard under MP");

        /* exact match shouldn't pick up sub-folder */
        doListShouldSucceed(otherConnection, "", mountpointName,
                Lists.newArrayList(mountpointName), "JUST MP");

        /* exact match on sub-folder should pick up just sub-folder */
        doListShouldSucceed(otherConnection, "", subMountpoint,
                Lists.newArrayList(subMountpoint), "JUST subfolder of MP");

        List<String> expectedMboxNames = Lists.newArrayList(baselineMboxNames);
        expectedMboxNames.add(mountpointName);
        expectedMboxNames.add(subMountpoint);
        /* sub-folder should be in list of all folders */
        doListShouldSucceed(otherConnection, "", "*", expectedMboxNames, "List ALL including mountpoints");
        doSelectShouldSucceed(otherConnection, mountpointName);
        doFetchShouldSucceed(otherConnection, "1:*", "(FLAGS ENVELOPE)", subFolderEnv.subjects);
        doSelectShouldSucceed(otherConnection, subMountpoint);
        doFetchShouldSucceed(otherConnection, "1:*", "(FLAGS ENVELOPE)", subFolderEnv.subFolderSubjects);
        // recent should have been set to 0 when closing the folder to select another one
        new StatusExecutor(otherConnection).setExists(2).setRecent(0)
                .execShouldSucceed(mountpointName,
                        "MESSAGES", "RECENT", "UIDNEXT", "UIDVALIDITY", "UNSEEN", "HIGHESTMODSEQ");
        // recent should not have changed whilst this folder is still selected
        new StatusExecutor(otherConnection).setExists(3).setRecent(3)
                .execShouldSucceed(subMountpoint,
                        "MESSAGES", "RECENT", "UIDNEXT", "UIDVALIDITY", "UNSEEN", "HIGHESTMODSEQ");
        // Result from this should show that the previous select reset the recent count on the folder
        doSelectShouldSucceed(otherConnection, mountpointName);
        otherConnection.logout();
        otherConnection = null;
    }

    @Test(timeout=100000)
    public void homeNameSpaceWithSubFolder() throws ServiceException, IOException, MessagingException {
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        String subFolder = sharedFolderName + "/subFolder";
        Account shareeAcct = TestUtil.createAccount(SHAREE);
        assertNotNull("Account for SHAREE", shareeAcct);
        SubFolderEnv subFolderEnv = new SubFolderEnv(sharedFolderName, subFolder);
        connection = connectAndLogin(USER);
        connection.setacl(sharedFolderName, SHAREE, "lrswickxteda");
        connection.logout();
        connection = null;
        String remFolder = String.format("/home/%s/%s", USER, sharedFolderName);
        String underRemFolder = String.format("%s/subFolder", remFolder);
        otherConnection = connectAndLogin(SHAREE);
        doListShouldSucceed(otherConnection, "", remFolder, Lists.newArrayList(remFolder), "shared folder");
        doListShouldSucceed(otherConnection, "", underRemFolder, Lists.newArrayList(underRemFolder),
                "subfolder of shared folder");
        doSelectShouldSucceed(otherConnection, remFolder);
        doFetchShouldSucceed(otherConnection, "1:*", "(FLAGS ENVELOPE)", subFolderEnv.subjects);
        doSelectShouldSucceed(otherConnection, underRemFolder);
        doFetchShouldSucceed(otherConnection, "1:*", "(FLAGS ENVELOPE)", subFolderEnv.subFolderSubjects);
        // recent should have been set to 0 when closing the folder to select another one
        new StatusExecutor(otherConnection).setExists(2).setRecent(0)
                .execShouldSucceed(remFolder,
                        "MESSAGES", "UIDNEXT", "UIDVALIDITY", "UNSEEN", "RECENT", "HIGHESTMODSEQ");
        // recent should not have changed whilst this folder is still selected
        new StatusExecutor(otherConnection).setExists(3).setRecent(3)
                .execShouldSucceed(underRemFolder,
                        "MESSAGES", "UIDNEXT", "UIDVALIDITY", "UNSEEN", "RECENT", "HIGHESTMODSEQ");
        // Result from this should show that the previous select reset the recent count on the folder
        doSelectShouldSucceed(otherConnection, remFolder);
        otherConnection.logout();
        otherConnection = null;
    }

    /** Mountpoints created in the classic ZWC way where a folder is shared and the share is accepted
     *  do not appear in the main list of folders.  They should however, be available under the /home hierarchy.
     */
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
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
        doFetchShouldSucceed(connection, "1:*", "(ENVELOPE)", Lists.newArrayList(subject));
        connection.logout();
        connection = null;
        String remFolder = String.format("/home/%s/%s", USER, sharedFolderName);
        String copyToFolder = "INBOX/copy-to";
        otherConnection = connectAndSelectInbox(SHAREE);
        doCopy(otherConnection, shareeZmbox, remFolder, copyToFolder, subject);
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
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
        doFetchShouldSucceed(connection, "1:*", "(ENVELOPE)", Lists.newArrayList(subject));
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
        doFetchShouldSucceed(connection, "1:*", "(ENVELOPE)", Lists.newArrayList(subject));
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
        assertNotNull(String.format("Select result for folder=%s", toFolderName), selectMboxInfo);
        assertEquals("Select result Folder Name folder", toFolderName, selectMboxInfo.getName());
        assertEquals(String.format("Number of exists for folder=%s after copy", toFolderName),
                1, selectMboxInfo.getExists());
        Map<Long, MessageData> mdMap = this.doFetchShouldSucceed(imapConn, "1:*", "(ENVELOPE)",
                Lists.newArrayList(srcMsgSubject));
        MessageData md = mdMap.values().iterator().next();
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
        assertTrue(listDataContains(listResult, parentFolder));
        assertTrue(listDataContains(listResult, childFolder1));
        assertTrue(listDataContains(listResult, childFolder2));
        String newParentFolder = "renamed";
        String newChildFolder1 = newParentFolder + "/child1";
        String newChildFolder2 = newChildFolder1 + "/child2";
        connection.rename(parentFolder, newParentFolder);
        listResult = connection.list("", "*");
        assertTrue(listDataContains(listResult, newParentFolder));
        assertTrue(listDataContains(listResult, newChildFolder1));
        assertTrue(listDataContains(listResult, newChildFolder2));
        assertFalse(listDataContains(listResult, parentFolder));
        assertFalse(listDataContains(listResult, childFolder1));
        assertFalse(listDataContains(listResult, childFolder2));
    }

    @Test(timeout=100000)
    public void savedSearch() throws ServiceException, IOException, MessagingException {
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        String subjectPrefix = String.format("%s test message ", testId);
        TestUtil.addMessage(mbox, subjectPrefix + "1", ZFolder.ID_INBOX);
        TestUtil.addMessage(mbox, subjectPrefix + "2", ZFolder.ID_DRAFTS);
        TestUtil.addMessage(mbox, subjectPrefix + "3 - does not match search", ZFolder.ID_SENT);
        String folderName = "searchFolderInDraftsOrInOnbox";
        ZSearchFolder srchFolder = mbox.createSearchFolder(ZFolder.ID_USER_ROOT, folderName,
            "in:drafts or in:inbox", ZSearchParams.TYPE_CONVERSATION, SearchSortBy.nameAsc, ZFolder.Color.ORANGE);
        assertNotNull("SearchFolder in Response to CreateSearchFolderRequest should not be null", srchFolder);
        connection = this.connectAndLogin(USER);
        List<ListData> listResult;
        //  LIST "" "mountpointName"
        doListShouldSucceed(connection, "", folderName, Lists.newArrayList(folderName),
                "Just search folder");
        listResult = connection.list("", "*");
        assertNotNull("list result 'list \"\" \"*\"' should not be null", listResult);
        boolean seenIt = false;
        for (ListData listEnt : listResult) {
            if (folderName.equals(listEnt.getMailbox())) {
                seenIt = true;
                break;
            }
        }
        assertTrue(String.format("'%s' mailbox not in result of 'list \"\" \"*\"'", folderName), seenIt);
        connection.select(folderName);
        Map<Long, MessageData> mdMap = connection.fetch("1:*", "(ENVELOPE)");
        assertEquals("Size of map returned by fetch", 2, mdMap.size());
        Iterator<MessageData> iter = mdMap.values().iterator();
        while (iter.hasNext()) {
            MessageData md = iter.next();
            assertNotNull("MessageData", md);
            Envelope env = md.getEnvelope();
            assertNotNull("Envelope", env);
            assertTrue(String.format("Message subject was '%s' expected to contain '%s'",
                    env.getSubject(), subjectPrefix), env.getSubject().contains(subjectPrefix));
        }
        connection.logout();
        connection = null;
    }

    private void createFolderAndShareWithSharee(String user, String sharedFolderName) throws IOException {
        otherConnection = connectAndSelectInbox(user);
        otherConnection.create(sharedFolderName);
        otherConnection.setacl(sharedFolderName, SHAREE, "lrswickxteda");
        otherConnection.logout();
        otherConnection.close();
        otherConnection = null;
    }

    @Test(timeout=100000)
    public void clashingHomeSubFolders() throws ServiceException, IOException, MessagingException {
        TestUtil.createAccount(USER2);
        Account shareeAcct = TestUtil.createAccount(SHAREE);
        assertNotNull("Account object for sharee", shareeAcct);
        String sharedFolderName = String.format("INBOX/%s-shared", testId);
        String underSharedFolderName = String.format("%s/subFolder", sharedFolderName);
        createFolderAndShareWithSharee(USER, sharedFolderName);
        createFolderAndShareWithSharee(USER2, sharedFolderName);
        String remFolder1 = String.format("/home/%s/%s", USER, underSharedFolderName);
        String remFolder2 = String.format("/home/%s/%s", USER2, underSharedFolderName);
        otherConnection = connectAndLogin(SHAREE);
        otherConnection.create(remFolder1);
        otherConnection.create(remFolder2);
        doSelectShouldSucceed(otherConnection, remFolder1);
        doSelectShouldSucceed(otherConnection, remFolder2);
        doListShouldSucceed(otherConnection, "", remFolder1, Lists.newArrayList(remFolder1), "shared 1");
        doListShouldSucceed(otherConnection, "", remFolder2, Lists.newArrayList(remFolder2), "shared 2");
        otherConnection = connectAndLogin(USER);
        doSelectShouldSucceed(otherConnection, underSharedFolderName);
        otherConnection.logout();
        otherConnection.close();
        otherConnection = connectAndLogin(USER2);
        doSelectShouldSucceed(otherConnection, underSharedFolderName);
    }

    @Test(timeout=100000)
    public void searchBodyHomeShare() throws ServiceException, IOException {
        List<Long> matches;
        TestUtil.createAccount(SHAREE);
        connection = super.connectAndLogin(USER);
        String sharedFolderName = "INBOX/share";
        connection.create(sharedFolderName);
        String underSharedFolderName = String.format("%s/subFolder", sharedFolderName);
        connection.create(underSharedFolderName);
        String topBody = "Orange\nApple\nPear\nPlum Nectarine";
        String subBody = "Green\nBlack\nBlue\nPurple Silver";
        doAppend(connection, sharedFolderName, "in share directly under inbox", topBody, (Flags) null,
                true /* do fetch to check content */);
        doAppend(connection, sharedFolderName, "in share directly under inbox", "nothing much", (Flags) null,
                true /* do fetch to check content */);
        doAppend(connection, underSharedFolderName, "in subFolder", subBody, (Flags) null,
                true /* do fetch to check content */);
        doAppend(connection, underSharedFolderName, "in subFolder", "even less interesting", (Flags) null,
                true /* do fetch to check content */);
        doSelectShouldSucceed(connection, sharedFolderName);
        matches = connection.search((Object[]) new String[] { "BODY Pear" } );
        assertEquals("Number of matches in top level for owner", 1, matches.size());
        connection.setacl(sharedFolderName, SHAREE, "lrswickxteda");
        connection.logout();
        connection = null;
        String remFolder = String.format("/home/%s/%s", USER, sharedFolderName);
        String underRemFolder = String.format("%s/subFolder", remFolder);
        otherConnection = connectAndLogin(SHAREE);
        doSelectShouldSucceed(otherConnection, remFolder);
        matches = otherConnection.search((Object[]) new String[] { "BODY Pear" } );
        assertEquals("Number of matches in top level", 1, matches.size());
        assertEquals("ID of matching message in top level", Long.valueOf(1), matches.get(0));
        doSelectShouldSucceed(otherConnection, underRemFolder);
        matches = otherConnection.search((Object[]) new String[] { "BODY Purple" } );
        assertEquals("Number of matches in subFolder", 1, matches.size());
        assertEquals("ID of matching message in subFolder", Long.valueOf(1), matches.get(0));
        otherConnection.logout();
        otherConnection = null;
    }

    @Test(timeout=100000)
    public void searchBodyMountpoint() throws ServiceException, IOException {
        String sharedFolder = "INBOX/share";
        String subFolder = sharedFolder + "/subFolder";
        String mountpoint = String.format("shared-", testInfo.getMethodName());
        String subMountpoint = mountpoint + "/subFolder";
        TestUtil.createAccount(SHAREE);
        ZMailbox userZmbox = TestUtil.getZMailbox(USER);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);

        TestUtil.createMountpoint(userZmbox, "/" + sharedFolder, shareeZmbox, mountpoint);
        TestUtil.createFolder(userZmbox, "/" + subFolder);
        List<Long> matches;
        connection = super.connectAndLogin(USER);
        String topBody = "Orange\nApple\nPear\nPlum Nectarine";
        String subBody = "Green\nBlack\nBlue\nPurple Silver";
        doAppend(connection, sharedFolder, "in share directly under inbox", topBody, (Flags) null,
                true /* do fetch to check content */);
        doAppend(connection, sharedFolder, "in share directly under inbox", "nothing much", (Flags) null,
                true /* do fetch to check content */);
        doAppend(connection, subFolder, "in subFolder", subBody, (Flags) null,
                true /* do fetch to check content */);
        doAppend(connection, subFolder, "in subFolder", "even less interesting", (Flags) null,
                true /* do fetch to check content */);
        doSelectShouldSucceed(connection, subFolder);
        matches = connection.search((Object[]) new String[] { "BODY Black" } );
        assertEquals("Number of matches in top level for owner", 1, matches.size());
        connection.logout();
        connection = null;
        otherConnection = connectAndLogin(SHAREE);
        doSelectShouldSucceed(otherConnection, mountpoint);
        matches = otherConnection.search((Object[]) new String[] { "BODY Pear" } );
        assertEquals("Number of matches in top level", 1, matches.size());
        assertEquals("ID of matching message in top level", Long.valueOf(1), matches.get(0));
        doSelectShouldSucceed(otherConnection, subMountpoint);
        matches = otherConnection.search((Object[]) new String[] { "BODY Black" } );
        assertEquals("Number of matches in subFolder", 1, matches.size());
        assertEquals("ID of matching message in subFolder", Long.valueOf(1), matches.get(0));
        otherConnection.logout();
        otherConnection = null;
    }

    @Test(timeout=100000)
    public void copyToMountpoint() throws Exception {
        TestUtil.createAccount(SHAREE);
        ZMailbox userZmbox = TestUtil.getZMailbox(USER);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        String sharedFolder = "INBOX/share";
        String mountpoint = String.format("shared-", testInfo.getMethodName());
        String subject = "SharedImapTests-testMessage";
        TestUtil.createMountpoint(userZmbox, "/" + sharedFolder, shareeZmbox, mountpoint);
        TestUtil.addMessage(shareeZmbox, subject, Integer.toString(Mailbox.ID_FOLDER_INBOX), null);
        connection = connectAndSelectInbox(SHAREE);
        CopyResult copyResult = connection.copy("1", mountpoint);
        assertNotNull("copyResult.getFromUids()", copyResult.getFromUids());
        assertNotNull("copyResult.getToUids()", copyResult.getToUids());
        assertEquals("Number of fromUIDs", 1, copyResult.getFromUids().length);
        assertEquals("Number of toUIDs", 1, copyResult.getToUids().length);
        MailboxInfo selectMboxInfo = connection.select(mountpoint);
        assertNotNull(String.format("Select result for folder=%s", mountpoint), selectMboxInfo);
        assertEquals("Select result Folder Name folder", mountpoint, selectMboxInfo.getName());
        assertEquals(String.format("Number of exists for folder=%s after copy", mountpoint),
                1, selectMboxInfo.getExists());
        Map<Long, MessageData> mdMap = this.doFetchShouldSucceed(connection, "1:*", "(ENVELOPE)",
                Lists.newArrayList(subject));
        MessageData md = mdMap.values().iterator().next();
        assertNull("Internal date was NOT requested and should be NULL", md.getInternalDate());
        BodyStructure bs = md.getBodyStructure();
        assertNull("Body Structure was not requested and should be NULL", bs);
        Body[] body = md.getBodySections();
        assertNull("body sections were not requested and should be null", body);
    }

    @Test(timeout=100000)
    public void recentWithSelectAndExamine() throws Exception {
        connection = connectAndLogin(USER);
        String folderName = "INBOX/recent";
        connection.create(folderName);
        for (int cnt = 0;cnt < 4;cnt++) {
            doAppend(connection, folderName, 5 /* size */, Flags.fromSpec("afs"),
                    false /* don't do fetch as affects recent */);
        }
        connection.logout();
        connection = connectAndLogin(USER);
        for (int cnt = 0;cnt < 3;cnt++) {
            doAppend(connection, folderName, 8 /* size */, Flags.fromSpec("afs"),
                    false /* don't do fetch as affects recent */);
        }
        MailboxInfo selectInfo = doSelectShouldSucceed(connection, folderName);
        assertEquals("SELECT after several appends - RECENT count", 7, selectInfo.getRecent());
        List<Long> searchResult;
        searchResult = connection.search((Object[]) new String[] { "RECENT" } );
        assertEquals("number of 'SEARCH RECENT' hits after first SELECT", 7, searchResult.size());
        searchResult = connection.search((Object[]) new String[] { "NOT RECENT" } );
        assertEquals("number of 'SEARCH NOT RECENT' hits after first SELECT", 0, searchResult.size());
        searchResult = connection.search((Object[]) new String[] { "NOT NOT RECENT" } );
        assertEquals("number of 'SEARCH NOT NOT RECENT' hits after first SELECT", 7, searchResult.size());
        MailboxInfo examineInfo = doExamineShouldSucceed(connection, folderName);
        assertEquals("EXAMINE when have folder selected - RECENT count", 0, examineInfo.getRecent());

        otherConnection = connectAndLogin(USER);
        selectInfo = doSelectShouldSucceed(otherConnection, folderName);
        assertEquals("SELECT when selected by other session - RECENT count", 0, selectInfo.getRecent());
        otherConnection.logout();
        otherConnection = null;
        otherConnection = connectAndLogin(USER);

        /* switch folders in original session, so that it is no longer monitoring the target folder */
        selectInfo = doSelectShouldSucceed(connection, "INBOX");
        doAppend(connection, folderName, 5 /* size */, Flags.fromSpec("afs"),
                false /* don't do fetch as affects recent */);
        otherConnection = connectAndLogin(USER);
        selectInfo = doSelectShouldSucceed(otherConnection, folderName);
        assertEquals("SELECT when no other session has selected + append since last select - RECENT count",
                1, selectInfo.getRecent());
        selectInfo = doSelectShouldSucceed(otherConnection, "INBOX");
        selectInfo = doSelectShouldSucceed(otherConnection, folderName);
        assertEquals("SELECT when switched folder and back - RECENT count",
                0, selectInfo.getRecent());
        selectInfo = doSelectShouldSucceed(otherConnection, "INBOX");
        doAppend(connection, folderName, 5 /* size */, Flags.fromSpec("afs"),
                false /* don't do fetch as affects recent */);
        examineInfo = doExamineShouldSucceed(otherConnection, folderName);
        assertEquals("EXAMINE folder not selected, recent append elsewhere - RECENT count",
                1, examineInfo.getRecent());
        selectInfo = doSelectShouldSucceed(otherConnection, folderName);
        assertEquals("SELECT after EXAMINE, should remain same - RECENT count", 1, selectInfo.getRecent());
        otherConnection.logout();
        otherConnection = null;
    }

    @Test
    public void appendTooLarge() throws Exception {
        TestUtil.setConfigAttr(Provisioning.A_zimbraMtaMaxMessageSize, "100");
        connection = super.connectAndLogin(USER);
        try {
            doAppend(connection, "INBOX", 120, Flags.fromSpec("afs"),
                    false /* don't do fetch as affects recent */);
            fail("APPEND succeeded - should have failed because content is too big");
        } catch (CommandFailedException cfe) {
            String msg = "maximum message size exceeded";
            assertTrue(String.format(
                    "APPEND threw CommandFailedException with message '%s' which does not contain '%s'",
                    cfe.getMessage(), msg), cfe.getMessage().contains(msg));
        }
    }

    protected void flushCacheIfNecessary() throws Exception {
        // overridden by tests running against imapd
    }

    private void createMsgsInFolder(Mailbox mbox, int folderId, int numMessages) throws Exception {
        assertNotNull("Mailbox for USER", mbox);
        String subject = "testUidRangeSearch-%d";
        for (int i=0;i<numMessages;i++) {
            TestUtil.addMessage(mbox, folderId, String.format(subject, i));
        }
    }

    private class SearchInfo {
        private String folder;
        private String search;
        private int numHits;
        private int expected;
        private String firstToLast;

        private SearchInfo(String selectedFolder, String srchSpec, int expectedHits) {
            folder = selectedFolder;
            search = srchSpec;
            expected = expectedHits;
        }

        private void assertPassed(String template) {
            assertEquals("Folder=" + folder + ":" +
                String.format(template, search + " UNDELETED"), expected, numHits);
        }

        private void assertPassed() {
            assertPassed("WrongNumber of results for 'UID SEARCH %s'");
        }

        private List<Long> uidSearch(ImapConnection conn) throws IOException {
            List<Long> results = conn.uidSearch((Object[]) new String[] {search + " UNDELETED"});
            numHits = results.size();
            if (numHits > 0) {
                firstToLast = String.format("%s:%s", results.get(0), results.get(results.size() - 1));
            } else {
                firstToLast = null;
            }
            return results;
        }

        private void uidSearchExpectingFailure(ImapConnection conn) throws IOException {
            try {
                conn.uidSearch((Object[]) new String[] {search + " UNDELETED"});
                fail("Folder=" + folder + ":Expected 'UID SEARCH %s' to fail but it succeeded.");
            } catch (CommandFailedException cfe) {
            }
        }

        @Override
        public String toString() {
            return String.format("'%s':%d", search, numHits);
        }
    }

    private void doRangeSearch(String user, String folderName, int numMessages) throws Exception {
        connection = connect(user);
        connection.login(PASS);

        doListShouldSucceed(connection, "", folderName, Lists.newArrayList(folderName),
                "List just single folder");
        if (!folderName.startsWith("/home")) {
            List<ListData> listResult = connection.list("", "*");
            assertNotNull("list result 'list \"\" \"*\"' should not be null", listResult);
            boolean seenIt = false;
            for (ListData listEnt : listResult) {
                if (folderName.equals(listEnt.getMailbox())) {
                    seenIt = true;
                    break;
                }
            }
            assertTrue(String.format("'%s' mailbox not in result of 'list \"\" \"*\"'", folderName), seenIt);
        }
        connection.select(folderName);

        /* on remote IMAP this used to result in a search request with 9 item IDs (see ZCS-3557)
         * Now should make use of ranges.  This is true of later searches too. */
        SearchInfo ten18 = new SearchInfo(folderName, "10:18", 9);
        ten18.uidSearch(connection);

        SearchInfo twenty = new SearchInfo(folderName, "20", 1);
        twenty.uidSearch(connection);

        SearchInfo ten18uidRange = new SearchInfo(folderName, "UID " + ten18.firstToLast, 9);
        ten18uidRange.uidSearch(connection);

        SearchInfo twenty23 = new SearchInfo(folderName, "20:23", 4);
        twenty23.uidSearch(connection);

        SearchInfo ten18twenty23uidRange = new SearchInfo(folderName,
                String.format("UID %s,%s", ten18.firstToLast, twenty23.firstToLast), 9 + 4);
        ten18twenty23uidRange.uidSearch(connection);

        SearchInfo ten800 = new SearchInfo(folderName, "10:800", 791);
        ten800.uidSearch(connection);

        SearchInfo oneThousand1999 = new SearchInfo(folderName, "1000:1999", 1000);
        oneThousand1999.uidSearch(connection);

        SearchInfo one2000 = new SearchInfo(folderName, "1:2000", 2000);
        one2000.uidSearch(connection);

        SearchInfo oneThousand2100 = new SearchInfo(folderName, "1000:2100", 1101);
        oneThousand2100.uidSearch(connection);

        SearchInfo oneStar = new SearchInfo(folderName, "1:*", numMessages);
        oneStar.uidSearch(connection);

        /*
         * The reason for performing all assertions together and reporting all numbers in each
         * assertion message is to get a more complete picture when tests fail.
         */
        String assertTemplate = "Wrong number of results for 'UID SEARCH %s'. Results are: " +
             String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s",
                ten18, twenty, twenty23, ten18uidRange, ten18twenty23uidRange, ten800,
                one2000, oneThousand1999, oneThousand2100, oneStar
                );
        ten18.assertPassed(assertTemplate);
        ten18uidRange.assertPassed(assertTemplate);
        twenty.assertPassed(assertTemplate);
        twenty23.assertPassed(assertTemplate);
        ten18twenty23uidRange.assertPassed(assertTemplate);
        /* Once ZCS-3810 has been fixed, can delete associated small range tests and always do this */
        if (numMessages >= 800) {
            ten800.assertPassed(assertTemplate);
            oneThousand1999.assertPassed(assertTemplate);
            one2000.assertPassed(assertTemplate);
            oneThousand2100.assertPassed(assertTemplate);
            oneStar.assertPassed(assertTemplate);
        }
        //  Test out some odd searches
        SearchInfo same = new SearchInfo(folderName, "20:20", 1);
        same.uidSearch(connection);
        same.assertPassed();
        SearchInfo negMin = new SearchInfo(folderName, "-20:23", 0);
        negMin.uidSearchExpectingFailure(connection);
        SearchInfo negMax = new SearchInfo(folderName, "20:-23", 0);
        negMax.uidSearchExpectingFailure(connection);
        SearchInfo negs = new SearchInfo(folderName, "-26:-23", 0);
        negs.uidSearchExpectingFailure(connection);
        /* Code seems to assume range given wrong way round and adjusts accordingly.
         * Doesn't seem particularly harmful, so left like that.
         */
        SearchInfo rangeWrongWayRound = new SearchInfo(folderName, "23:20", 4);
        rangeWrongWayRound.uidSearch(connection);
        rangeWrongWayRound.assertPassed();
        connection.logout();
        connection.close();
        connection = null;
    }

    private void rangeSearchPopulateCollector(String user, String folderName, int numMessages)
            throws Exception {
        try {
            doRangeSearch(user, folderName, numMessages);
        } catch (Throwable t) {
            collector.addError(t);
        }
    }

    @Test(timeout=240000)
    public void uidSearchRangeUndeleted() throws Exception {
        TestUtil.createAccount(SHAREE);
        ZMailbox userZmbox = TestUtil.getZMailbox(USER);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        Mailbox userMbox = TestUtil.getMailbox(USER);
        Mailbox shareeMbox = TestUtil.getMailbox(SHAREE);

        /* create a mountpoint */
        String sharedFolder = "INBOX";
        String mountpoint = String.format("sharedINBOX-%s", testInfo.getMethodName());
        TestUtil.createMountpoint(userZmbox, "/" + sharedFolder, shareeZmbox, mountpoint);
        assertNotNull("Mailbox for SHAREE", shareeMbox); // Keep PMD happy that doing asserts

        String virtualFolderInInboxIsUnread = "InInboxUnread";
        userMbox.createSearchFolder(null, Mailbox.ID_FOLDER_USER_ROOT, virtualFolderInInboxIsUnread,
                "IN:INBOX IS:UNREAD", "message", "none", 0, (byte)9);

        String otherUsersInboxUnderHome = String.format("/home/%s/INBOX", USER);

        int numMessages = 2200;
        createMsgsInFolder(userMbox, Mailbox.ID_FOLDER_INBOX, numMessages);
        rangeSearchPopulateCollector(USER, "INBOX", numMessages);
        rangeSearchPopulateCollector(USER, virtualFolderInInboxIsUnread, numMessages);
        rangeSearchPopulateCollector(SHAREE, mountpoint, numMessages);
        rangeSearchPopulateCollector(SHAREE, otherUsersInboxUnderHome, numMessages);
    }

    private void doSimpleRangeSearch(String user, String folderName) throws Exception {
        connection = connect(user);
        connection.login(PASS);
        connection.select(folderName);
        SearchInfo same = new SearchInfo(folderName, "11:46", 36);
        same.uidSearch(connection);
        same.assertPassed();
    }

    /** The idea of this test was to have a search folder based on items in 2 mailboxes.
     *  As we use the item ID as the UID most of the time, then there is a danger of UID collisions.
     */
    @Test(timeout=160000)
    @Ignore("ZCS-4110 Need to support underlying folders from different mailboxes which have same [U]ID")
    public void cursorOnComplexVirtualFolder() throws Exception {
        TestUtil.createAccount(SHAREE);
        ZMailbox userZmbox = TestUtil.getZMailbox(USER);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(SHAREE);
        Mailbox userMbox = TestUtil.getMailbox(USER);
        Mailbox shareeMbox = TestUtil.getMailbox(SHAREE);
        String sharedFolder = "INBOX";
        String mountpoint = String.format("sharedINBOX-%s", testInfo.getMethodName());
        TestUtil.createMountpoint(userZmbox, "/" + sharedFolder, shareeZmbox, mountpoint);
        assertNotNull("Mailbox for SHAREE", shareeMbox); // Keep PMD happy that doing asserts
        int numMessages = 24;
        /* If add in this to avoid UID name clashes, then local IMAP works but remote doesn't.
         * However, don't think it is worth more effort until we can cope with UID clashes
         */
        // createMsgsInFolder(userMbox, Mailbox.ID_FOLDER_DRAFTS, 30);  // avoid clashing UIDs
        createMsgsInFolder(userMbox, Mailbox.ID_FOLDER_INBOX, numMessages);
        createMsgsInFolder(shareeMbox, Mailbox.ID_FOLDER_INBOX, numMessages);
        String folderName = "InInboxesUnread";
        shareeMbox.createSearchFolder(null, Mailbox.ID_FOLDER_USER_ROOT, folderName,
                String.format("(IN:INBOX OR IN:%s) IS:UNREAD", mountpoint), "message", "none", 0, (byte)9);
        doSimpleRangeSearch(SHAREE, folderName);
    }
}
