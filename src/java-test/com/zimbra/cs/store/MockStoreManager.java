/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.common.io.ByteStreams;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Mock implementation of {@link StoreManager}.
 *
 * @author jylee
 * @author ysasaki
 */
public final class MockStoreManager extends StoreManager {

    private static final Map<Integer, MockMailboxBlob> BLOBS = new HashMap<Integer, MockMailboxBlob>();

    public MockStoreManager() {
//        DebugConfig.disableMessageStoreFsync = true;
    }

    public static void setBlob(MailItem item, byte[] data) {
        BLOBS.put(item.getId(), new MockMailboxBlob(item.getMailbox(), item.getId(), item.getVersion(), null, data));
    }

    @Override
    public void startup() {
        purge();
    }

    @Override
    public void shutdown() {
        purge();
    }

    @Override
    public boolean supports(StoreFeature feature) {
        switch (feature) {
            case BULK_DELETE:  return false;
            default:           return false;
        }
    }

    public static void purge() {
        BLOBS.clear();
    }

    public static int size() {
        return BLOBS.size();
    }

    @Override
    public BlobBuilder getBlobBuilder() {
        return new MockBlobBuilder();
    }

    @Override
    public Blob storeIncoming(InputStream data, StorageCallback callback, boolean storeAsIs) throws IOException {
        return new MockBlob(ByteStreams.toByteArray(data));
    }

    @Override
    public StagedBlob stage(InputStream data, long actualSize, StorageCallback callback, Mailbox mbox)
    throws IOException {
        return new MockStagedBlob(mbox, ByteStreams.toByteArray(data));
    }

    @Override
    public StagedBlob stage(Blob blob, Mailbox mbox) {
        return new MockStagedBlob(mbox, ((MockBlob) blob).content);
    }

    @Override
    public MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destItemId, int destRevision) {
        MockMailboxBlob blob = new MockMailboxBlob(destMbox, destItemId, destRevision,
                src.getLocator(), ((MockMailboxBlob) src).content);
        BLOBS.put(destItemId, blob);
        return blob;
    }

    @Override
    public MailboxBlob link(StagedBlob src, Mailbox destMbox, int destItemId, int destRevision) {
        MockMailboxBlob blob = new MockMailboxBlob(destMbox, destItemId, destRevision,
                src.getLocator(), ((MockStagedBlob) src).content);
        BLOBS.put(destItemId, blob);
        return blob;
    }

    @Override
    public MailboxBlob link(MailboxBlob src, Mailbox destMbox, int destItemId, int destRevision) {
        MockMailboxBlob blob = new MockMailboxBlob(destMbox, destItemId, destRevision,
                src.getLocator(), ((MockMailboxBlob) src).content);
        BLOBS.put(destItemId, blob);
        return blob;
    }

    @Override
    public MailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destItemId, int destRevision) {
        MockMailboxBlob blob = new MockMailboxBlob(destMbox, destItemId, destRevision,
                src.getLocator(), ((MockStagedBlob) src).content);
        BLOBS.put(destItemId, blob);
        return blob;
    }

    @Override
    public boolean delete(Blob blob) {
        return true;
    }

    @Override
    public boolean delete(StagedBlob staged) {
        return true;
    }

    @Override
    public boolean delete(MailboxBlob mblob) {
        BLOBS.remove(mblob.getItemId());
        return true;
    }

    @Override
    public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, int revision, String locator) {
        return BLOBS.get(Integer.valueOf(itemId));
    }

    @Override
    public InputStream getContent(MailboxBlob mblob) throws IOException {
        return mblob.getLocalBlob().getInputStream();
    }

    @Override
    public InputStream getContent(Blob blob) throws IOException {
        return blob.getInputStream();
    }

    @Override
    public boolean deleteStore(Mailbox mbox, Iterable<MailboxBlob> blobs) {
        assert blobs != null : "we require a blob iterator for testing purposes";
        for (MailboxBlob mblob : blobs) {
            delete(mblob);
        }
        return true;
    }

    private static final class MockBlob extends Blob {
        byte[] content;

        MockBlob() {
            super(new File("build/test/store"));
            content = new byte[0];
        }

        void setContent(byte[] content) {
            this.content = content;
        }

        MockBlob(byte[] data) {
            super(new File("build/test/store"));
            content = data;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public long getRawSize() {
            return content.length;
        }
    }

    private static final class MockStagedBlob extends StagedBlob {
        final byte[] content;

        MockStagedBlob(Mailbox mbox, byte[] data) {
            super(mbox, String.valueOf(data.length), data.length);
            content = data;
        }

        @Override
        public String getLocator() {
            return null;
        }
    }

    @SuppressWarnings("serial")
    private static final class MockMailboxBlob extends MailboxBlob {
        final byte[] content;

        MockMailboxBlob(Mailbox mbox, int itemId, int revision, String locator, byte[] data) {
            super(mbox, itemId, revision, locator);
            content = data;
        }

        @Override
        public Blob getLocalBlob() throws IOException {
            return new MockBlob(content);
        }
    }

    private static final class MockBlobBuilder extends BlobBuilder {
        private ByteArrayOutputStream out;

        protected MockBlobBuilder() {
            super(new MockBlob());
        }

        @Override
        protected OutputStream createOutputStream(File file) throws FileNotFoundException {
            assert out == null : "Output stream already created";
            out = new ByteArrayOutputStream();
            return out;
        }

        @Override
        protected FileChannel getFileChannel() {
            return null;
        }

        @Override
        public Blob finish() throws IOException, ServiceException {
            MockBlob mockblob = (MockBlob) super.finish();

            if (out != null) {
                mockblob.setContent(out.toByteArray());
                out = null;
            }

            return mockblob;
        }
    }
}
