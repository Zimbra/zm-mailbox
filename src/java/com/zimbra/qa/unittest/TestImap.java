/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZTag;
import com.zimbra.client.ZTag.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.AccessBoundedRegex;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.imap.AppendMessage;
import com.zimbra.cs.mailclient.imap.AppendResult;
import com.zimbra.cs.mailclient.imap.Body;
import com.zimbra.cs.mailclient.imap.CAtom;
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
 * IMAP server tests.
 */
public class TestImap extends TestCase {
    private static final String HOST = "localhost";
    private static final int PORT = 7143;
    private static final String USER = "imap-test-user";
    private static final String PASS = "test123";

    private ImapConnection connection;

    @Override
    public void setUp() throws Exception {
        TestUtil.createAccount(USER);
        connection = connect();
    }

    @Override
    public void tearDown() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER); //funky, but somehow it gets us around SSL

        TestUtil.deleteAccount(USER);
        if (connection != null) {
            connection.close();
        }
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
        try {
            List<ListData> listResult = connection.list("", "**************** HELLO");
            assertNotNull(listResult);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.info("Expected CommandFailedException", cfe);
        }
    }

    @Test
    public void testList93114DosWithPercents() throws Exception {
        try {
            List<ListData> listResult = connection.list("", "%%%%%%%%%%%%%%%% HELLO");
            assertNotNull(listResult);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.info("Expected CommandFailedException", cfe);
        }
    }

    @Test
    public void testList93114DosStarPercentRepeats() throws Exception {
        StringBuilder mboxPatt = new StringBuilder();
        for (int cnt = 1;cnt < 50; cnt++) {
            mboxPatt.append("*%");
        }
        mboxPatt.append(" HELLO");
        try {
            List<ListData> listResult = connection.list("", mboxPatt.toString());
            assertNotNull(listResult);
        } catch (CommandFailedException cfe) {
            ZimbraLog.test.info("Expected CommandFailedException", cfe);
        }
    }

    @Test
    public void testListInbox() throws Exception {
        List<ListData> listResult = connection.list("", "INBOX");
        assertNotNull(listResult);
        assertEquals("List result should have this number of entries", 1, listResult.size());
    }
    @Test
    public void testListContacts() throws Exception {
        List<ListData> listResult = connection.list("", "*Contacts*");
        assertNotNull(listResult);
        // 'Contacts' and 'Emailed Contacts'
        assertTrue("List result should have at least 2 entries", listResult.size() >= 2);
        for (ListData le : listResult) {
            assertTrue(String.format("mailbox '%s' contains 'Contacts'", le.getMailbox()),
                    le.getMailbox().contains("Contacts"));
        }
    }

    @Test
    public void testAppend() throws Exception {
        assertTrue(connection.hasCapability("UIDPLUS"));
        Flags flags = Flags.fromSpec("afs");
        Date date = new Date(System.currentTimeMillis());
        Literal msg = message(100000);
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
    public void testOverflowAppend() throws Exception {
        assertTrue(connection.hasCapability("UIDPLUS"));
        int oldReadTimeout = connection.getConfig().getReadTimeout();
        try {
            connection.setReadTimeout(10);
            Flags flags = Flags.fromSpec("afs");
            Date date = new Date(System.currentTimeMillis());
            ImapRequest req = connection.newRequest(CAtom.APPEND, new MailboxName("INBOX"));
            req.addParam("{"+((long)(Integer.MAX_VALUE)+1)+"+}");
            ImapResponse resp = req.send();
            assertTrue(resp.isNO() || resp.isBAD());

            req = connection.newRequest(CAtom.APPEND, new MailboxName("INBOX"));
            req.addParam("{"+((long)(Integer.MAX_VALUE)+1)+"}");
            resp = req.send();
            assertTrue(resp.isNO() || resp.isBAD());
        } finally {
            connection.setReadTimeout(oldReadTimeout);
        }
    }

    @Test
    public void testOverflowNotAppend() throws Exception {
        int oldReadTimeout = connection.getConfig().getReadTimeout();
        try {
            connection.setReadTimeout(10);
            Flags flags = Flags.fromSpec("afs");
            Date date = new Date(System.currentTimeMillis());
            ImapRequest req = connection.newRequest(CAtom.FETCH, "1:*");
            req.addParam("{"+((long)(Integer.MAX_VALUE)+1)+"+}");
            ImapResponse resp = req.send();
            assertTrue(resp.isNO() || resp.isBAD());
        } finally {
            connection.setReadTimeout(oldReadTimeout);
        }
    }

    @Test
    public void testAppendNoLiteralPlus() throws Exception {
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run() throws Exception {
                testAppend();
            }
        });
    }

    @Test
    public void testStoreTags() throws Exception {
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
        assertTrue("flags unexpectedly set after STORE on message in "+folderName, data.get(seq).getFlags().isEmpty());

        info = connection.select("INBOX");
        assertTrue("old tag not set in new folder", info.getFlags().isSet(tagName));
        assertFalse("new tag unexpectedly set in new folder", info.getFlags().isSet(tagName2));
    }

    private void storeInvalidFlag(String flag, Long seq) throws IOException {
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

    @Test
    public void testStoreInvalidSystemFlag() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER);
        mbox.addMessage(Mailbox.ID_FOLDER_INBOX+"", "u", "", System.currentTimeMillis(), simpleMessage("foo"), true);
        MailboxInfo info = connection.select("INBOX");
        Map<Long, MessageData> data = connection.fetch("1:*", "FLAGS");
        assertEquals(1, data.size());
        Iterator<Long> it = data.keySet().iterator();
        Long seq = it.next();

        storeInvalidFlag("\\Bulk", seq);
        storeInvalidFlag("\\Unread", seq);
        storeInvalidFlag("\\Forwarded", seq);
    }

    @Test
    public void testStoreTagsDirty() throws Exception {
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
        ZFolder folder = mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", folderName, ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);
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

    @Test
    public void testAppendTags() throws Exception {
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
        ZFolder folder = mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", folderName, ZFolder.View.message, ZFolder.Color.DEFAULTCOLOR, null, null);

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
        Literal msg = message(10);
        Flags flags = Flags.fromSpec("afs");
        flags.set(flag);
        Date date = new Date(System.currentTimeMillis());
        try {
            AppendResult res = connection.append("INBOX", flags, date, msg);
            fail("server allowed client to set system flag "+flag);
        } catch (CommandFailedException e) {
            //expected
        } finally {
            msg.dispose();
        }
        connection.noop(); //do a no-op so we don't hit max consecutive error limit
    }

    @Test
    public void testAppendInvalidSystemFlag() throws Exception {
        //basic case - append with new tag
        appendInvalidFlag("\\Bulk");
        appendInvalidFlag("\\Unread");
        appendInvalidFlag("\\Forwarded");
    }

    @Test
    public void testAppendTagsDirty() throws Exception {
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

    @Test
    public void testCatenateSimple() throws Exception {
        assertTrue(connection.hasCapability("CATENATE"));
        assertTrue(connection.hasCapability("UIDPLUS"));
        String part1 = simpleMessage("test message");
        String part2 = "more text\r\n";
        AppendMessage am = new AppendMessage(
            null, null, literal(part1), literal(part2));
        AppendResult res = connection.append("INBOX", am);
        connection.select("INBOX");
        byte[] body = fetchBody(res.getUid());
        assertArrayEquals("content mismatch", bytes(part1 + part2), body);
    }


    @Test
    public void testCatenateSimpleNoLiteralPlus() throws Exception {
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run() throws Exception {
                testCatenateSimple();
            }
        });
    }

    @Test
    public void testCatenateUrl() throws Exception {
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
        byte[] b2 = fetchBody(res2.getUid());
        assertArrayEquals("content mismatch", bytes(msg2), b2);
    }

    @Test
    public void testMultiappend() throws Exception {
        assertTrue(connection.hasCapability("MULTIAPPEND"));
        assertTrue(connection.hasCapability("UIDPLUS"));
        AppendMessage msg1 = new AppendMessage(null, null, literal("test 1"));
        AppendMessage msg2 = new AppendMessage(null, null, literal("test 2"));
        AppendResult res = connection.append("INBOX", msg1, msg2);
        assertNotNull(res);
        assertEquals("expecting 2 uids", 2, res.getUids().length);
    }

    @Test
    public void testMultiappendNoLiteralPlus() throws Exception {
        withLiteralPlus(false, new RunnableTest() {
            @Override
            public void run() throws Exception {
                testMultiappend();
            }
        });
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

    private byte[] fetchBody(long uid) throws IOException {
        MessageData md = connection.uidFetch(uid, "(BODY.PEEK[])");
        assertNotNull("message not found", md);
        assertEquals(uid, md.getUid());
        Body[] bs = md.getBodySections();
        assertNotNull(bs);
        assertEquals(1, bs.length);
        return bs[0].getImapData().getBytes();
    }

    private static Literal message(int size) throws IOException {
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

    private ImapConnection connect() throws IOException {
        ImapConfig config = new ImapConfig(HOST);
        config.setPort(PORT);
        config.setAuthenticationId(USER);
        config.getLogger().setLevel(Log.Level.trace);
        ImapConnection connection = new ImapConnection(config);
        connection.connect();
        connection.login(PASS);
        connection.select("INBOX");
        return connection;
    }

    public static void amain(String[] args) throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestImapImport.class);
    }
}
