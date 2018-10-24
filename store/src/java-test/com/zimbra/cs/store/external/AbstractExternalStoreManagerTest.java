/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.store.external;

import java.io.File;
import org.junit.Ignore;
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

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public abstract class AbstractExternalStoreManagerTest extends AbstractStoreManagerTest {

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
