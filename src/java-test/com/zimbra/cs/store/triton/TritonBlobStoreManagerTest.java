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
package com.zimbra.cs.store.triton;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import org.bouncycastle.util.Arrays;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.store.AbstractStoreManagerTest;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.IncomingBlob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.qa.unittest.TestUtil;

@Ignore("requires Triton server")
public class TritonBlobStoreManagerTest extends AbstractStoreManagerTest {
    @Override
    protected StoreManager getStoreManager() {
        return new TritonBlobStoreManager("http://10.33.30.77", "SHA0");
    }

    @Test
    @Override
    public void incoming() throws Exception {
        byte[] bytes = new byte[25000];
        Arrays.fill(bytes, (byte) 123);
        StoreManager sm = StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        IncomingBlob incoming = sm.newIncomingBlob("foo", null);

        OutputStream out = incoming.getAppendingOutputStream();
        out.write(bytes);

        Blob blob = incoming.getBlob();

        Assert.assertEquals("blob size = incoming written", bytes.length, blob.getRawSize());
        Assert.assertTrue(blob instanceof TritonBlob);

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
        //same as incoming, but uses ByteUtil.copy() which mimics behavior of FileUploaderResource
        byte[] bytes = new byte[25000];
        Arrays.fill(bytes, (byte) 123);
        StoreManager sm = StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        IncomingBlob incoming = sm.newIncomingBlob("foo", null);

        OutputStream out = incoming.getAppendingOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ByteUtil.copy(bais, true, out, true);

        Blob blob = incoming.getBlob();

        Assert.assertEquals("blob size = incoming written", bytes.length, blob.getRawSize());
        Assert.assertTrue(blob instanceof TritonBlob);

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
}
