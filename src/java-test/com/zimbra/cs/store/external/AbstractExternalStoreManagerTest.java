/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
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
package com.zimbra.cs.store.external;

import java.io.File;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ThreaderTest;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.AbstractStoreManagerTest;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.qa.unittest.TestUtil;

public abstract class AbstractExternalStoreManagerTest extends AbstractStoreManagerTest {

    @Test
    public void testUncachedSubstream() throws Exception {
        ParsedMessage pm = ThreaderTest.getRootMessage();
        byte[] mimeBytes = TestUtil.readInputStream(pm.getRawInputStream());
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        StoreManager sm = StoreManager.getInstance();
        Blob blob = sm.storeIncoming(pm.getRawInputStream());
        StagedBlob staged = sm.stage(blob, mbox);
        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        mblob = sm.getMailboxBlob(mbox, 0, 0, staged.getLocator());

        Blob localBlob = mblob.getLocalBlob();
        InputStream stream = sm.getContent(localBlob);

        Assert.assertTrue("input stream external", stream instanceof BlobInputStream);

        if (sm instanceof ExternalStoreManager) {
            ((ExternalStoreManager) sm).clearCache();
        }
        blob.getFile().delete();
        Assert.assertFalse(blob.getFile().exists());

        //create new stream spanning the whole blob
        InputStream newStream = ((BlobInputStream) stream).newStream(0, -1);
        Assert.assertNotNull(newStream);
        Assert.assertTrue("stream content = mime content", TestUtil.bytesEqual(mimeBytes, newStream));
    }

    @Test
    public void testUncachedFile() throws Exception {
        ParsedMessage pm = ThreaderTest.getRootMessage();
        byte[] mimeBytes = TestUtil.readInputStream(pm.getRawInputStream());
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        StoreManager sm = StoreManager.getInstance();
        Blob blob = sm.storeIncoming(pm.getRawInputStream());
        StagedBlob staged = sm.stage(blob, mbox);
        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        mblob = sm.getMailboxBlob(mbox, 0, 0, staged.getLocator());

        Blob localBlob = mblob.getLocalBlob();
        InputStream stream = sm.getContent(localBlob);

        Assert.assertTrue("input stream external", stream instanceof BlobInputStream);

        if (sm instanceof ExternalStoreManager) {
            ((ExternalStoreManager) sm).clearCache();
        }
        blob.getFile().delete();
        Assert.assertFalse(blob.getFile().exists());

        //now get it again. this would bomb if it only looked in cache
        stream = sm.getContent(mblob.getLocalBlob());
        Assert.assertTrue("input stream external", stream instanceof ExternalBlobInputStream);
        ExternalBlobInputStream extStream = (ExternalBlobInputStream) stream;
        File file = extStream.getRootFile();
        Assert.assertTrue(file.exists());

        Assert.assertTrue("stream content = mime content", TestUtil.bytesEqual(mimeBytes, stream));
    }

}
