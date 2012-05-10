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
package com.zimbra.cs.store.external;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.IncomingBlob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.qa.unittest.TestUtil;

public class StoreManagerNegativeTest {

    static StoreManager originalStoreManager;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MailboxTestUtil.initProvisioning();
        Provisioning.getInstance().createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

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

    protected StoreManager getStoreManager() {
        return new BrokenStreamingStoreManager();
    }

    @Test
    public void nullLocator() throws Exception {
        Random rand = new Random();
        byte[] bytes = new byte[10000];
        rand.nextBytes(bytes);

        StoreManager sm = StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        IncomingBlob incoming = sm.newIncomingBlob("foo", null);

        OutputStream out = incoming.getAppendingOutputStream();
        out.write(bytes);

        Blob blob = incoming.getBlob();

        Assert.assertEquals("blob size = incoming written", bytes.length, blob.getRawSize());

        Assert.assertTrue("blob content = mime content", TestUtil.bytesEqual(bytes, blob.getInputStream()));

        StagedBlob staged = sm.stage(blob, mbox);
        Assert.assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        Assert.assertEquals("link size = staged size", staged.getSize(), mblob.getSize());
        try {
            mblob.getLocalBlob().getInputStream();
            Assert.fail("Expected IOException since locator is not handled correctly");
        } catch (IOException io) {
            //expected
        } finally {
            sm.delete(mblob);
        }
    }

    @Test
    public void incorrectRemoteSize() throws Exception {
        Random rand = new Random();
        byte[] bytes = new byte[10000];
        rand.nextBytes(bytes);

        StoreManager sm = StoreManager.getInstance();
        IncomingBlob incoming = sm.newIncomingBlob("foo", null);

        OutputStream out = incoming.getAppendingOutputStream();
        out.write(bytes);

        try {
            incoming.getCurrentSize();
            Assert.fail("Expected exception since remote size is incorrect");
        } catch (IOException ioe) {
            //expected
        }
    }



    private class BrokenStreamingStoreManager extends SimpleStreamingStoreManager implements ExternalResumableUpload {

        @Override
        public String finishUpload(ExternalUploadedBlob blob) throws IOException, ServiceException {
            return null;
        }

        @Override
        public String writeStreamToStore(InputStream in, long actualSize, Mailbox mbox) throws IOException {
            super.writeStreamToStore(in, actualSize, mbox);
            return null;
        }

        @Override
        public InputStream readStreamFromStore(String locator, Mailbox mbox) throws IOException {
            return null;
        }

        @Override
        public boolean deleteFromStore(String locator, Mailbox mbox) throws IOException {
            return false;
        }

        @Override
        public ExternalResumableIncomingBlob newIncomingBlob(String id, Object ctxt) throws IOException,
                        ServiceException {
            return new SimpleStreamingIncomingBlob(id, getBlobBuilder(), ctxt);
        }

        private class SimpleStreamingIncomingBlob extends ExternalResumableIncomingBlob {

            private final File file;

            public SimpleStreamingIncomingBlob(String id, BlobBuilder blobBuilder, Object ctx) throws ServiceException,
                            IOException {
                super(id, blobBuilder, ctx);
                String baseName = uploadDirectory + "/upload-" + id;
                String name = baseName;

                synchronized (this) {
                    int count = 1;
                    File upFile = new File(name + ".upl");
                    while (upFile.exists()) {
                        name = baseName + "_" + count++;
                        upFile = new File(name + ".upl");
                    }
                    if (upFile.createNewFile()) {
                        ZimbraLog.store.debug("writing to new file %s", upFile.getName());
                        file = upFile;
                    } else {
                        throw new IOException("unable to create new file");
                    }
                }
            }

            @Override
            protected ExternalResumableOutputStream getAppendingOutputStream(BlobBuilder blobBuilder)
                            throws IOException {
                return new SimpleStreamingOutputStream(blobBuilder, file);
            }

            @Override
            protected long getRemoteSize() throws IOException {
                return file.length() - 1; //size returned wrong to test getCurrentSize() mismatches
            }

            @Override
            public Blob getBlob() throws IOException, ServiceException {
                return new ExternalUploadedBlob(blobBuilder.finish(), file.getCanonicalPath());
            }
        }

        private class SimpleStreamingOutputStream extends ExternalResumableOutputStream {

            private final FileOutputStream fos;

            public SimpleStreamingOutputStream(BlobBuilder blobBuilder, File file) throws IOException {
                super(blobBuilder);
                this.fos = new FileOutputStream(file);
            }

            @Override
            protected void writeToExternal(byte[] b, int off, int len) throws IOException {
                fos.write(b, off, len);
            }
        }
    }
}
