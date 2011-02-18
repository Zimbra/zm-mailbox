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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public class MockStoreManager extends StoreManager {

    private static class MockBlob extends Blob {
        private String content;
        protected MockBlob(String content) {
            super(new File("/var/tmp/mockblob"));
            this.content = content;
        }
        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content.getBytes());
        }        
    }
    private static class MockMailboxBlob extends MailboxBlob {
        private String content;
        protected MockMailboxBlob(Mailbox mbox, int itemId, int revision,
                String locator, String content) {
            super(mbox, itemId, revision, locator);
            this.content = content;
        }
        @Override
        public Blob getLocalBlob() throws IOException {
            return new MockBlob(content);
        }
    }
    
    private static HashMap<Integer,MockMailboxBlob> blobs = new HashMap<Integer,MockMailboxBlob>();
    
    public static void setBlob(MailItem item, String content) {
        blobs.put(item.getId(), new MockMailboxBlob(item.getMailbox(), item.getId(), item.getVersion(), null, content));
    }
    
    @Override
    public void startup() throws IOException, ServiceException {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public BlobBuilder getBlobBuilder() throws IOException, ServiceException {
        return null;
    }

    @Override
    public Blob storeIncoming(InputStream data, StorageCallback callback,
            boolean storeAsIs) throws IOException, ServiceException {
        return null;
    }

    @Override
    public StagedBlob stage(InputStream data, long actualSize,
            StorageCallback callback, Mailbox mbox) throws IOException,
            ServiceException {
        return null;
    }

    @Override
    public StagedBlob stage(Blob blob, Mailbox mbox) throws IOException,
            ServiceException {
        return null;
    }

    @Override
    public MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destMsgId,
            int destRevision) throws IOException, ServiceException {
        return null;
    }

    @Override
    public MailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId,
            int destRevision) throws IOException, ServiceException {
        return null;
    }

    @Override
    public MailboxBlob link(MailboxBlob src, Mailbox destMbox, int destMsgId,
            int destRevision) throws IOException, ServiceException {
        return null;
    }

    @Override
    public MailboxBlob renameTo(StagedBlob src, Mailbox destMbox,
            int destMsgId, int destRevision) throws IOException,
            ServiceException {
        return null;
    }

    @Override
    public boolean delete(Blob blob) throws IOException {
        return false;
    }

    @Override
    public boolean delete(StagedBlob staged) throws IOException {
        return false;
    }

    @Override
    public boolean delete(MailboxBlob mblob) throws IOException {
        return false;
    }

    @Override
    public MailboxBlob getMailboxBlob(Mailbox mbox, int msgId, int revision,
            String locator) throws ServiceException {
        return blobs.get(Integer.valueOf(msgId));
    }

    @Override
    public InputStream getContent(MailboxBlob mboxBlob) throws IOException {
        return mboxBlob.getLocalBlob().getInputStream();
    }

    @Override
    public InputStream getContent(Blob blob) throws IOException {
        return blob.getInputStream();
    }

    @Override
    public boolean deleteStore(Mailbox mbox) throws IOException,
            ServiceException {
        return false;
    }
}
