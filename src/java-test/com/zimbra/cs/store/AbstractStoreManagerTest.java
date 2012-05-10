/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.ThreaderTest;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.external.ExternalStoreManager;
import com.zimbra.qa.unittest.TestUtil;

public abstract class AbstractStoreManagerTest {

    static StoreManager originalStoreManager;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MailboxTestUtil.initProvisioning();
        Provisioning.getInstance().createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    /**
     * Return an instance of the StoreManager implementation being tested
     */
    protected abstract StoreManager getStoreManager();

    @Before
    public void setUp() throws Exception {
        originalStoreManager = StoreManager.getInstance();
        StoreManager.setInstance(getStoreManager());
        StoreManager.getInstance().startup();
    }

    @After
    public void tearDown() throws Exception {
        StoreManager.getInstance().shutdown();
        StoreManager.setInstance(originalStoreManager);
    }

    @Test
    public void store() throws Exception {
        ParsedMessage pm = ThreaderTest.getRootMessage();
        byte[] mimeBytes = TestUtil.readInputStream(pm.getRawInputStream());

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        StoreManager sm = StoreManager.getInstance();
        Blob blob = sm.storeIncoming(pm.getRawInputStream());

        Assert.assertEquals("blob size = message size", pm.getRawData().length, blob.getRawSize());
        Assert.assertTrue("blob content = mime content", TestUtil.bytesEqual(mimeBytes, blob.getInputStream()));

        StagedBlob staged = sm.stage(blob, mbox);
        Assert.assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        Assert.assertEquals("link size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("link content = mime content", TestUtil.bytesEqual(mimeBytes, mblob.getLocalBlob().getInputStream()));

        mblob = sm.getMailboxBlob(mbox, 0, 0, staged.getLocator());
        Assert.assertEquals("mblob size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("mailboxblob content = mime content", TestUtil.bytesEqual(mimeBytes, mblob.getLocalBlob().getInputStream()));

        sm.delete(mblob);

    }

    /**
     * Tests putting two copies of the same message into the store (bug 67969).
     */
    @Test
    public void sameDigest() throws Exception {
        ParsedMessage pm = ThreaderTest.getRootMessage();
        StoreManager sm = StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Blob blob1 = sm.storeIncoming(pm.getRawInputStream());
        StagedBlob staged1 = sm.stage(blob1, mbox);
        MailboxBlob mblob1 = sm.link(staged1, mbox, 0, 0);

        Blob blob2 = sm.storeIncoming(pm.getRawInputStream());
        StagedBlob staged2 = sm.stage(blob2, mbox);
        MailboxBlob mblob2 = sm.link(staged2, mbox, 0, 0);

        mblob1.getLocalBlob();
        mblob2.getLocalBlob();
        sm.delete(mblob1);
        sm.delete(mblob2);
    }

    @Test
    public void incoming() throws Exception {
        Random rand = new Random();
        byte[] bytes = new byte[1000000];
        rand.nextBytes(bytes);

        StoreManager sm = StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        IncomingBlob incoming = sm.newIncomingBlob("foo", null);

        OutputStream out = incoming.getAppendingOutputStream();
        out.write(bytes);
        Assert.assertEquals(bytes.length, incoming.getCurrentSize());
        Blob blob = incoming.getBlob();

        Assert.assertEquals("blob size = incoming written", bytes.length, blob.getRawSize());

        Assert.assertTrue("blob content = mime content", TestUtil.bytesEqual(bytes, blob.getInputStream()));

        StagedBlob staged = sm.stage(blob, mbox);
        Assert.assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        Assert.assertEquals("link size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("link content = mime content", TestUtil.bytesEqual(bytes, mblob.getLocalBlob().getInputStream()));

        mblob = sm.getMailboxBlob(mbox, 0, 0, staged.getLocator());
        Assert.assertEquals("mblob size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("mailboxblob content = mime content", TestUtil.bytesEqual(bytes, mblob.getLocalBlob().getInputStream()));

        sm.delete(mblob);
    }

    @Test
    public void incomingMultipost() throws Exception {
        byte[] bytes = "AAAAStrinGBBB".getBytes();
        StoreManager sm = StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        IncomingBlob incoming = sm.newIncomingBlob("foo", null);

        OutputStream out = incoming.getAppendingOutputStream();
        byte[] b1 = "AAAA".getBytes();
        byte[] b2 = "StrinG".getBytes();
        byte[] b3 = "BBB".getBytes();
        out.write(b1);
        int written = b1.length;
        Assert.assertEquals(written, incoming.getCurrentSize());
        out.close();
        out = incoming.getAppendingOutputStream();
        out.write(b2);
        out.close();
        written += b2.length;
        Assert.assertEquals(written, incoming.getCurrentSize());
        out = incoming.getAppendingOutputStream();
        out.write(b3);
        out.close();
        written += b3.length;
        Assert.assertEquals(written, incoming.getCurrentSize());
        Blob blob = incoming.getBlob();

        Assert.assertEquals("blob size = incoming written", bytes.length, blob.getRawSize());

        Assert.assertTrue("blob content = mime content", TestUtil.bytesEqual(bytes, blob.getInputStream()));

        StagedBlob staged = sm.stage(blob, mbox);
        Assert.assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        Assert.assertEquals("link size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("link content = mime content", TestUtil.bytesEqual(bytes, mblob.getLocalBlob().getInputStream()));

        mblob = sm.getMailboxBlob(mbox, 0, 0, staged.getLocator());
        Assert.assertEquals("mblob size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("mailboxblob content = mime content", TestUtil.bytesEqual(bytes, mblob.getLocalBlob().getInputStream()));

        sm.delete(mblob);
    }

    @Test
    public void incomingByteUtilCopy() throws Exception {
        //similar to incoming, but uses ByteUtil.copy() which mimics behavior of FileUploaderResource
        Random rand = new Random();
        byte[] bytes = new byte[1000000];

        rand.nextBytes(bytes);
        StoreManager sm = StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        IncomingBlob incoming = sm.newIncomingBlob("foo", null);

        OutputStream out = incoming.getAppendingOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ByteUtil.copy(bais, true, out, true);
        Assert.assertEquals(bytes.length, incoming.getCurrentSize());

        Blob blob = incoming.getBlob();

        Assert.assertEquals("blob size = incoming written", bytes.length, blob.getRawSize());

        Assert.assertTrue("blob content = mime content", TestUtil.bytesEqual(bytes, blob.getInputStream()));

        StagedBlob staged = sm.stage(blob, mbox);
        Assert.assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        Assert.assertEquals("link size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("link content = mime content", TestUtil.bytesEqual(bytes, mblob.getLocalBlob().getInputStream()));

        mblob = sm.getMailboxBlob(mbox, 0, 0, staged.getLocator());
        Assert.assertEquals("mblob size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("mailboxblob content = mime content", TestUtil.bytesEqual(bytes, mblob.getLocalBlob().getInputStream()));

        sm.delete(mblob);
    }

    @Test
    public void emptyBlob() throws Exception {
        StoreManager sm = StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        IncomingBlob incoming = sm.newIncomingBlob("foo", null);
        Blob blob = incoming.getBlob();
        Assert.assertEquals("blob size = incoming written", 0, blob.getRawSize());
        if (sm instanceof ExternalStoreManager) {
            ((ExternalStoreManager) sm).clearCache();
        }
        StagedBlob staged = sm.stage(blob, mbox);
        Assert.assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        if (sm instanceof ExternalStoreManager) {
            ((ExternalStoreManager) sm).clearCache();
        }
        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        Assert.assertEquals("link size = staged size", staged.getSize(), mblob.getSize());


        if (sm instanceof ExternalStoreManager) {
            ((ExternalStoreManager) sm).clearCache();
        }
        mblob = sm.getMailboxBlob(mbox, 0, 0, staged.getLocator());
        Assert.assertEquals("mblob size = staged size", staged.getSize(), mblob.getSize());

        if (sm instanceof ExternalStoreManager) {
            ((ExternalStoreManager) sm).clearCache();
        }
        Assert.assertEquals(0, mblob.getLocalBlob().getRawSize());

        sm.delete(mblob);

    }
}
