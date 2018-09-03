/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.RandomStringUtils;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;
import com.zimbra.qa.unittest.AccountTestUtil;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Utility for verifying StoreManager functionality. Intended to be available for 3rd party integrators
 * Does not (and must never) depend on Junit since we do not ship that with the product
 */
public class VerifyStoreManager extends AdminDocumentHandler {

    private class Stats {
        int numBlobs;
        long incomingTime;
        long stageTime;
        long linkTime;
        long fetchTime;
        long deleteTime;

        private Stats(int numBlobs, long incomingTime, long stageTime,
                        long linkTime, long fetchTime, long deleteTime) {
            this.numBlobs = numBlobs;
            this.incomingTime = incomingTime;
            this.stageTime = stageTime;
            this.linkTime = linkTime;
            this.fetchTime = fetchTime;
            this.deleteTime = deleteTime;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("num blobs", numBlobs).add("storeIncoming",incomingTime)
                            .add("stage",stageTime).add("link", linkTime).add("fetch", fetchTime)
                            .add("delete", deleteTime).toString();
        }
    }

    private String USER_NAME = "zimbraStoreVerifyUser";

    private byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = -1;
        while ((i = is.read()) >= 0) {
            baos.write(i);
        }
        return baos.toByteArray();
    }

    private boolean bytesEqual(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            return false;
        } else {
            for (int i = 0; i < b1.length; i++) {
                if (b1[i] != b2[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    private void assertEquals(String message, Object o1, Object o2) throws Exception {
        if (o1 == null && o2 == null) {
            return;
        } else if (o1 == null && o2 != null) {
            throw new Exception("verification failed checking "+message);
        } else if (!o1.equals(o2)) {
            if (o1 instanceof Number && o2 instanceof Number) {
                Number num1 = (Number) o1;
                Number num2 = (Number) o2;
                if (num1.longValue() == num2.longValue()) {
                    return;
                }
            }
            throw new Exception("verification failed checking "+message);
        }
    }

    private void assertTrue(String message, boolean condition) throws Exception {
        if (!condition) {
            throw new Exception("verification failed checking "+message);
        }
    }

    private ParsedMessage getMessage(int size) throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", " Jimmy <jimmy@example.com>");
        mm.setHeader("To", " Janis <janis@example.com>");
        mm.setHeader("Subject", "Hello");
        mm.setHeader("Message-ID", "<sakfuslkdhflskjch@oiwm.example.com>");
        mm.setText("nothing to see here\r\n" + RandomStringUtils.random(size));
        return new ParsedMessage(mm, false);
    }

    private void testStore() throws Exception {
        ParsedMessage pm = getMessage(1024);
        byte[] mimeBytes = readInputStream(pm.getRawInputStream());

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(AccountTestUtil.getAccount(USER_NAME));

        StoreManager sm = StoreManager.getInstance();
        Blob blob = sm.storeIncoming(pm.getRawInputStream());

        assertEquals("blob size = message size", pm.getRawData().length, blob.getRawSize());
        assertTrue("blob content = mime content", bytesEqual(mimeBytes, readInputStream(blob.getInputStream())));

        StagedBlob staged = sm.stage(blob, mbox);
        assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        assertEquals("link size = staged size", staged.getSize(), mblob.getSize());
        assertTrue("link content = mime content", bytesEqual(mimeBytes, readInputStream(mblob.getLocalBlob().getInputStream())));

        mblob = sm.getMailboxBlob(mbox, 0, 0, staged.getLocator());
        assertEquals("mblob size = staged size", staged.getSize(), mblob.getSize());
        assertTrue("mailboxblob content = mime content", bytesEqual(mimeBytes, readInputStream(mblob.getLocalBlob().getInputStream())));

        sm.delete(mblob);
    }

    private void checkBlob(ParsedMessage pm, Blob blob, StagedBlob staged, MailboxBlob linked, MailboxBlob fetched, Mailbox mbox)
    throws IOException, Exception {
        byte[] mimeBytes = readInputStream(pm.getRawInputStream());

        assertEquals("blob size = message size", pm.getRawData().length, blob.getRawSize());
        assertTrue("blob content = mime content", bytesEqual(mimeBytes, readInputStream(blob.getInputStream())));

        assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        assertEquals("link size = staged size", staged.getSize(), linked.getSize());
        assertTrue("link content = mime content", bytesEqual(mimeBytes, readInputStream(linked.getLocalBlob().getInputStream())));

        assertEquals("mblob size = staged size", staged.getSize(), fetched.getSize());
        assertTrue("mailboxblob content = mime content", bytesEqual(mimeBytes, readInputStream(fetched.getLocalBlob().getInputStream())));
    }

    private Stats basicPerfTest(int numBlobs, int blobSize, boolean checkBlobs) throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(AccountTestUtil.getAccount(USER_NAME));
        StoreManager sm = StoreManager.getInstance();
        List<ParsedMessage> msgs = new ArrayList<ParsedMessage>();
        int count = numBlobs;
        for (int i = 0; i < count; i++) {
            msgs.add(getMessage(blobSize));
        }
        List<Blob> incoming = new ArrayList<Blob>();
        ZimbraLog.store.info("starting store incoming loop");
        long start = System.currentTimeMillis();
        for (ParsedMessage msg : msgs) {
            incoming.add(sm.storeIncoming(msg.getRawInputStream()));
        }
        long incomingTime = System.currentTimeMillis() - start;

        List<StagedBlob> staged = new ArrayList<StagedBlob>();
        ZimbraLog.store.info("starting stage loop");
        start = System.currentTimeMillis();
        for (Blob blob : incoming) {
            staged.add(sm.stage(blob, mbox));
        }
        long stageTime = System.currentTimeMillis() - start;

        List<MailboxBlob> linked = new ArrayList<MailboxBlob>();
        ZimbraLog.store.info("starting link loop");
        start = System.currentTimeMillis();
        int i = 0; //fake itemId, never use this test with real userid
        for (StagedBlob blob : staged) {
            linked.add(sm.link(blob, mbox, i++, 1));
        }
        long linkTime = System.currentTimeMillis() - start;

        List<MailboxBlob> fetched = new ArrayList<MailboxBlob>();
        ZimbraLog.store.info("starting fetch loop");
        start = System.currentTimeMillis();
        i = 0;
        for (MailboxBlob mblob : linked) {
            fetched.add(sm.getMailboxBlob(mbox, i++, 1, mblob.getLocator()));
        }
        long fetchTime = System.currentTimeMillis() - start;
        if (checkBlobs) {
            for (i = 0; i < count; i++) {
                checkBlob(msgs.get(i), incoming.get(i), staged.get(i), linked.get(i), fetched.get(i), mbox);
            }
        }

        ZimbraLog.store.info("starting delete loop");
        start = System.currentTimeMillis();
        for (MailboxBlob mblob: fetched) {
            sm.delete(mblob);
        }
        long deleteTime = System.currentTimeMillis() - start;

        Stats stats = new Stats(numBlobs, incomingTime, stageTime, linkTime, fetchTime, deleteTime);
        return stats;
    }

    private boolean createAccountIfNeeded() throws ServiceException {
        if (AccountTestUtil.accountExists(USER_NAME)) {
            return false;
        }
        ZimbraLog.store.info("creating account for test");
        Provisioning.getInstance().createAccount(AccountTestUtil.getAddress(USER_NAME), "test123", null);
        return true;
    }

    private void deleteAccount() throws ServiceException {
        Account acct = AccountTestUtil.getAccount(USER_NAME);
        MailboxManager.getInstance().getMailboxByAccount(acct).deleteMailbox();
        Provisioning.getInstance().deleteAccount(acct.getId());
    }

    @Override
    public synchronized Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element response = zsc.createElement(AdminConstants.VERIFY_STORE_MANAGER_RESPONSE);

        boolean created = createAccountIfNeeded();
        try {
            try {
                ZimbraLog.store.info("verifying basic store functionality");
                testStore();
            } catch (Exception e) {
                throw ServiceException.FAILURE("store verification failed", e);
            }
            int blobSize = request.getAttributeInt(AdminConstants.A_FILE_SIZE, 1024);
            try {
                ZimbraLog.store.info("running perf sanity tests");
                Stats stats = basicPerfTest(request.getAttributeInt(AdminConstants.A_NUM, 1000), blobSize,
                    request.getAttributeBool(AdminConstants.A_CHECK_BLOBS, true));
                response.addAttribute("storeManagerClass", StoreManager.getInstance().getClass().getName())
                    .addAttribute("incomingTime", stats.incomingTime).addAttribute("stageTime", stats.stageTime)
                    .addAttribute("linkTime", stats.linkTime).addAttribute("fetchTime", stats.fetchTime)
                    .addAttribute("deleteTime", stats.deleteTime);
            } catch (Exception e) {
                throw ServiceException.FAILURE("perf sanity test failed", e);
            } catch (OutOfMemoryError oome) {
                //don't crash server when user tries a huge blob size
                throw ServiceException.FAILURE("OOME during perf test, with blob size "+blobSize+
                    ". May need to use a smaller blob size or increase heap space", oome);
            }
        } finally {
            ZimbraLog.store.info("deleting account for test");
            if (created) {
                deleteAccount();
            }
        }
        return response;
    }
}