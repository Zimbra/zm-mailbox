/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.UserServlet;

public abstract class HttpStoreManager extends StoreManager {
    private final IncomingDirectory mIncoming = new IncomingDirectory(LC.zimbra_store_directory.value() + File.separator + "incoming");
    private final LocalBlobCache mLocalCache = new LocalBlobCache(LC.zimbra_tmp_directory.value() + File.separator + "blobs");

    protected abstract String getPostUrl(Mailbox mbox);
    protected abstract String getGetUrl(Mailbox mbox, String locator);
    protected abstract String getDeleteUrl(Mailbox mbox, String locator);

    @Override public void startup() throws IOException, ServiceException {
        ZimbraLog.store.info("starting up store " + this.getClass().getName());

        // set up sweeping for the incoming blob directory
        FileUtil.mkdirs(new File(mIncoming.getPath()));
        IncomingDirectory.setSweptDirectories(mIncoming);
        IncomingDirectory.startSweeper();

        // initialize file uncompressed file cache and file descriptor cache
        String uncompressedPath = LC.zimbra_tmp_directory.value() + File.separator + "uncompressed";
        FileUtil.ensureDirExists(uncompressedPath);
        UncompressedFileCache<String> ufcache = new UncompressedFileCache<String>(uncompressedPath).startup();
        BlobInputStream.setFileDescriptorCache(new FileDescriptorCache(ufcache).loadSettings());

        // create a local cache for downloading remote blobs
        mLocalCache.startup();
    }

    @Override public void shutdown() {
        IncomingDirectory.stopSweeper();
    }

    LocalBlobCache getBlobCache() {
        return mLocalCache;
    }

    @Override public BlobBuilder getBlobBuilder() throws IOException, ServiceException {
        return new BlobBuilder(new Blob(mIncoming.getNewIncomingFile()));
    }

    @Override public InputStream getContent(MailboxBlob mblob) throws IOException {
        if (mblob == null)
            return null;

        // check the local cache before doing a GET
        Blob blob = mLocalCache.get(mblob);
        if (blob != null)
            return getContent(blob);

        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(getGetUrl(mblob.getMailbox(), mblob.getLocator()));
        int statusCode = client.executeMethod(get);
        if (statusCode != HttpStatus.SC_OK)
            throw new IOException("unexpected return code during blob GET: " + get.getStatusText());
        return new UserServlet.HttpInputStream(get);
    }

    @Override public InputStream getContent(Blob blob) throws IOException {
        return new BlobInputStream(blob);
    }

    @Override public MailboxBlob getMailboxBlob(Mailbox mbox, int msgId, int revision, String locator)
    throws ServiceException {
        return new HttpMailboxBlob(mbox, msgId, revision, locator);
    }

    private static final int BUFLEN = Math.max(LC.zimbra_store_copy_buffer_size_kb.intValue(), 1) * 1024;

    @Override public Blob storeIncoming(InputStream data, int sizeHint, StorageCallback callback, boolean storeAsIs)
    throws IOException, ServiceException {
        BlobBuilder builder = getBlobBuilder().setSizeHint(sizeHint).disableCompression(storeAsIs).setStorageCallback(callback).init();

        byte[] buffer = new byte[BUFLEN];
        int numRead;
        while ((numRead = data.read(buffer)) >= 0)
            builder.update(buffer, 0, numRead);

        return builder.finish();
    }

    @Override public StagedBlob stage(InputStream in, int actualSize, StorageCallback callback, Mailbox mbox)
    throws IOException, ServiceException {
        // just stream straight to the remote http server if we can
        if (actualSize >= 0 && callback == null)
            return stage(in, actualSize, mbox, null);

        // for some reason, we need to route through the local filesystem
        Blob blob = storeIncoming(in, actualSize, callback);
        try {
            return stage(blob, mbox);
        } finally {
            quietDelete(blob);
        }
    }

    @Override public StagedBlob stage(Blob blob, Mailbox mbox) throws IOException, ServiceException {
        InputStream is = new BlobInputStream(blob);
        try {
            return stage(is, blob.getRawSize(), mbox, blob);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    protected abstract StagedBlob getStagedBlob(PostMethod post, Blob local) throws ServiceException, IOException;

    protected StagedBlob stage(InputStream in, long sizeHint, Mailbox mbox, Blob blob)
    throws IOException, ServiceException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        PostMethod post = new PostMethod(getPostUrl(mbox));
        try {
            post.setRequestEntity(new InputStreamRequestEntity(in, sizeHint, "application/octet-stream"));
            int statusCode = client.executeMethod(post);
            if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_NO_CONTENT)
                throw ServiceException.FAILURE("error POSTing blob: " + post.getStatusText(), null);
            return getStagedBlob(post, blob);
        } finally {
            post.releaseConnection();
        }
    }

    @Override public MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        return link(src, destMbox, destMsgId, destRevision);
    }

    @Override public MailboxBlob link(MailboxBlob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        // default implementation is a GET fed directly into a POST
        InputStream is = getContent(src);
        try {
            StagedBlob staged = stage(is, src.getSize(), destMbox, null);
            return link(staged, destMbox, destMsgId, destRevision);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    @Override public MailboxBlob link(StagedBlob staged, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        // link is a noop
        String locator = ((HttpStagedBlob) staged).getLocator();
        return new HttpMailboxBlob(destMbox, destMsgId, destRevision, locator);
    }

    @Override public MailboxBlob renameTo(StagedBlob staged, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException {
        // rename is a noop
        String locator = ((HttpStagedBlob) staged).getLocator();
        return new HttpMailboxBlob(destMbox, destMsgId, destRevision, locator);
    }

    @Override public boolean delete(MailboxBlob mblob) throws IOException {
        if (mblob == null)
            return true;

        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        DeleteMethod delete = new DeleteMethod(getDeleteUrl(mblob.getMailbox(), mblob.getLocator()));
        try {
            int statusCode = client.executeMethod(delete);
            switch (statusCode) {
                case HttpStatus.SC_OK:         return true;
                case HttpStatus.SC_NOT_FOUND:  return false;
                default:
                    throw new IOException("unexpected return code during blob DELETE: " + delete.getStatusText());
            }
        } finally {
            delete.releaseConnection();
        }
    }

    @Override public boolean delete(Blob blob) throws IOException {
        return blob.getFile().delete();
    }

    @Override public boolean deleteStore(Mailbox mbox) throws IOException, ServiceException {
        // TODO Auto-generated method stub
        return false;
    }
}
