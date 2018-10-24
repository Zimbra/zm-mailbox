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
package com.zimbra.cs.store;

import java.io.IOException;
import org.junit.Ignore;
import java.util.Collection;
import java.util.HashMap;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.file.BlobConsistencyChecker;
import com.zimbra.cs.store.file.BlobConsistencyChecker.BlobInfo;
import com.zimbra.cs.store.file.BlobConsistencyChecker.Results;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public abstract class AbstractBlobConsistencyCheckTest {

    static StoreManager originalStoreManager;
    protected final Log log = ZimbraLog.store;

    protected abstract StoreManager getStoreManager();
    protected abstract BlobConsistencyChecker getChecker();
    protected abstract Collection<Short> getVolumeIds();
    protected abstract void deleteAllBlobs() throws ServiceException, IOException;
    protected abstract void appendText(MailboxBlob blob, String text) throws IOException;
    protected abstract String createUnexpectedBlob(int index) throws ServiceException, IOException;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MailboxTestUtil.initProvisioning();
        Provisioning.getInstance().createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        //don't fail test even if native libraries not installed
        //this makes it easier to run unit tests from command line
        System.setProperty("zimbra.native.required", "false");
    }

    @Before
    public void setUp() throws Exception {
        originalStoreManager = StoreManager.getInstance();
        StoreManager.setInstance(getStoreManager());
        StoreManager.getInstance().startup();
        MailboxTestUtil.clearData();
        deleteAllBlobs();
    }


    @Test
    public void singleBlob() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null, new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(), false), dopt, null);

        BlobConsistencyChecker checker = getChecker();
        Results results = checker.check(getVolumeIds(), mbox.getId(), true, false);
        Assert.assertEquals(0, results.unexpectedBlobs.size());
        Assert.assertEquals(0, results.missingBlobs.size());
        Assert.assertEquals(0, results.usedBlobs.size());
        Assert.assertEquals(0, results.incorrectSize.size());
        Assert.assertEquals(0, results.incorrectModContent.size());
    }

    @Test
    public void missingBlobs() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        int msgs = 10;
        for (int i = 0; i < msgs; i++) {
            mbox.addMessage(null, new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(), false), dopt, null);
        }

        deleteAllBlobs();

        BlobConsistencyChecker checker = getChecker();
        Results results = checker.check(getVolumeIds(), mbox.getId(), true, false);

        Assert.assertEquals(msgs, results.missingBlobs.size());

        Assert.assertEquals(0, results.unexpectedBlobs.size());
        Assert.assertEquals(0, results.usedBlobs.size());
        Assert.assertEquals(0, results.incorrectSize.size());
        Assert.assertEquals(0, results.incorrectModContent.size());
    }

    @Test
    public void unexpectedBlobs() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String path = createUnexpectedBlob(0);

        BlobConsistencyChecker checker = getChecker();
        Results results = checker.check(getVolumeIds(), mbox.getId(), true, false);

        Assert.assertEquals(0, results.missingBlobs.size());

        Assert.assertEquals(1, results.unexpectedBlobs.size());
        BlobInfo info = results.unexpectedBlobs.values().iterator().next();
        Assert.assertEquals(path, info.path);

        Assert.assertEquals(0, results.usedBlobs.size());
        Assert.assertEquals(0, results.incorrectSize.size());
        Assert.assertEquals(0, results.incorrectModContent.size());

        deleteAllBlobs();

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null, new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(), false), dopt, null);


        int msgs = 10;
        for (int i = 0; i < msgs; i++) {
            createUnexpectedBlob(i);
        }

        results = checker.check(getVolumeIds(), mbox.getId(), true, false);

        Assert.assertEquals(0, results.missingBlobs.size());

        Assert.assertEquals(msgs, results.unexpectedBlobs.size());

        Assert.assertEquals(0, results.usedBlobs.size());
        Assert.assertEquals(0, results.incorrectSize.size());
        Assert.assertEquals(0, results.incorrectModContent.size());
    }

    @Test
    public void wrongSize() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(), false), dopt, null);

        MailboxBlob blob = msg.getBlob();
        String text = "some garbage";
        appendText(blob, text);

        BlobConsistencyChecker checker = getChecker();
        Results results = checker.check(getVolumeIds(), mbox.getId(), true, false);

        Assert.assertEquals(0, results.missingBlobs.size());
        Assert.assertEquals(0, results.unexpectedBlobs.size());
        Assert.assertEquals(0, results.usedBlobs.size());

        Assert.assertEquals(1, results.incorrectSize.size());
        BlobInfo info = results.incorrectSize.values().iterator().next();
        Assert.assertEquals(blob.size + text.length(), (long) info.fileDataSize);

        Assert.assertEquals(0, results.incorrectModContent.size());
    }

    @Test
    public void allBlobs() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        int msgs = 10;
        for (int i = 0; i < msgs; i++) {
            mbox.addMessage(null, new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(), false), dopt, null);
        }

        BlobConsistencyChecker checker = getChecker();
        Results results = checker.check(getVolumeIds(), mbox.getId(), true, true);

        Assert.assertEquals(0, results.missingBlobs.size());
        Assert.assertEquals(0, results.unexpectedBlobs.size());

        Assert.assertEquals(msgs, results.usedBlobs.size());

        Assert.assertEquals(0, results.incorrectSize.size());
        Assert.assertEquals(0, results.incorrectModContent.size());
    }

    @After
    public void tearDown() throws Exception {
        StoreManager.getInstance().shutdown();
        StoreManager.setInstance(originalStoreManager);
    }

}
