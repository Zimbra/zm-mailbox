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

import java.io.OutputStream;
import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.ThreaderTest;
import com.zimbra.cs.mime.ParsedMessage;
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
    }

    @Test
    public void incoming() throws Exception {
        StoreManager sm = StoreManager.getInstance();

        IncomingBlob incoming = sm.newIncomingBlob("foo", null);

        OutputStream out = incoming.getAppendingOutputStream();
        out.write(123);

        Blob blob = incoming.getBlob();

        Assert.assertEquals("blob size = incoming written", 1, blob.getRawSize());

    }
}
