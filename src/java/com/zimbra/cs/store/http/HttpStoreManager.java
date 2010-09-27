/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.store.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.FileDescriptorCache;
import com.zimbra.cs.store.IncomingDirectory;
import com.zimbra.cs.store.LocalBlobCache;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StorageCallback;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.UncompressedFileCache;

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

    /** Private subclass to get around Blob constructor visibility issues. */
    private static class HttpBlob extends Blob {
        HttpBlob(File incoming)  { super(incoming); }
    }

    /** Private subclass to get around BlobBuilder constructor visibility issues. */
    private static class HttpBlobBuilder extends BlobBuilder {
        HttpBlobBuilder(Blob targetBlob)  { super(targetBlob); }
    }

    @Override public BlobBuilder getBlobBuilder() {
        return new HttpBlobBuilder(new HttpBlob(mIncoming.getNewIncomingFile()));
    }

    @Override public InputStream getContent(Blob blob) throws IOException {
        return new BlobInputStream(blob);
    }

    @Override public InputStream getContent(MailboxBlob mblob) throws IOException {
        if (mblob == null)
            return null;
        return getContent(mblob.getMailbox(), mblob.getLocator());
    }

    private InputStream getContent(Mailbox mbox, String locator) throws IOException {
        if (mbox == null || locator == null)
            return null;

        // check the local cache before doing a GET
        Blob blob = mLocalCache.get(locator);
        if (blob != null)
            return getContent(blob);

        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(getGetUrl(mbox, locator));
        int statusCode = HttpClientUtil.executeMethod(client, get);
        if (statusCode != HttpStatus.SC_OK)
            throw new IOException("unexpected return code during blob GET: " + get.getStatusText());
        return new UserServlet.HttpInputStream(get);
    }

    Blob getLocalBlob(Mailbox mbox, String locator, long sizeHint) throws IOException {
        Blob blob = mLocalCache.get(locator);
        if (blob != null)
            return blob;

        InputStream is = getContent(mbox, locator);
        try {
            blob = storeIncoming(is, null);
            return mLocalCache.cache(locator, blob);
        } catch (ServiceException e) {
            throw new IOException("fetching local blob: " + e);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    @Override public MailboxBlob getMailboxBlob(Mailbox mbox, int msgId, int revision, String locator) {
        return new HttpMailboxBlob(mbox, msgId, revision, locator);
    }

    @Override public Blob storeIncoming(InputStream data, StorageCallback callback, boolean storeAsIs)
    throws IOException, ServiceException {
        BlobBuilder builder = getBlobBuilder().setStorageCallback(callback);
        // if the blob is already compressed, *don't* calculate a digest/size from what we write
        builder.disableCompression(storeAsIs).disableDigest(storeAsIs);

        return builder.init().append(data).finish();
    }

    @Override public StagedBlob stage(InputStream in, long actualSize, StorageCallback callback, Mailbox mbox)
    throws IOException, ServiceException {
        // just stream straight to the remote http server if we can
        if (actualSize >= 0 && callback == null)
            return stage(in, actualSize, mbox);

        // for some reason, we need to route through the local filesystem
        Blob blob = storeIncoming(in, callback);
        try {
            return stage(blob, mbox);
        } finally {
            quietDelete(blob);
        }
    }

    @Override public StagedBlob stage(Blob blob, Mailbox mbox) throws IOException, ServiceException {
        InputStream is = new BlobInputStream(blob);
        try {
            return stage(is, blob.getRawSize(), mbox);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    protected abstract StagedBlob getStagedBlob(PostMethod post, String postDigest, long postSize, Mailbox mbox)
    throws ServiceException, IOException;

    protected StagedBlob stage(InputStream in, long actualSize, Mailbox mbox)
    throws IOException, ServiceException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("SHA1 digest not found", e);
        }
        ByteUtil.PositionInputStream pin = new ByteUtil.PositionInputStream(new DigestInputStream(in, digest));

        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        PostMethod post = new PostMethod(getPostUrl(mbox));
        try {
            HttpClientUtil.addInputStreamToHttpMethod(post, pin, actualSize, "application/octet-stream");
            int statusCode = HttpClientUtil.executeMethod(client, post);
            if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED && statusCode != HttpStatus.SC_NO_CONTENT)
                throw ServiceException.FAILURE("error POSTing blob: " + post.getStatusText(), null);
            return getStagedBlob(post, ByteUtil.encodeFSSafeBase64(digest.digest()), pin.getPosition(), mbox);
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
            StagedBlob staged = stage(is, src.getSize(), destMbox);
            return link(staged, destMbox, destMsgId, destRevision);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    @Override public MailboxBlob link(StagedBlob staged, Mailbox destMbox, int destMsgId, int destRevision) {
        // link is a noop
        return renameTo(staged, destMbox, destMsgId, destRevision);
    }

    @Override public MailboxBlob renameTo(StagedBlob staged, Mailbox destMbox, int destMsgId, int destRevision) {
        // rename is a noop
        HttpStagedBlob hsb = (HttpStagedBlob) staged;
        hsb.markInserted();

        MailboxBlob mblob = new HttpMailboxBlob(destMbox, destMsgId, destRevision, hsb.getLocator());
        return mblob.setSize(hsb.getSize()).setDigest(hsb.getDigest());
    }

    @Override public boolean delete(MailboxBlob mblob) throws IOException {
        if (mblob == null)
            return true;
        return delete(mblob.getMailbox(), mblob.getLocator());
    }

    @Override public boolean delete(StagedBlob staged) throws IOException {
        HttpStagedBlob hsb = (HttpStagedBlob) staged;
        // we only delete a staged blob if it hasn't already been added to the mailbox 
        if (hsb == null || hsb.isInserted())
            return true;
        return delete(hsb.getMailbox(), hsb.getLocator());
    }

    private boolean delete(Mailbox mbox, String locator) throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        DeleteMethod delete = new DeleteMethod(getDeleteUrl(mbox, locator));
        try {
            int statusCode = HttpClientUtil.executeMethod(client, delete);
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

    @Override public boolean delete(Blob blob) {
        return blob.getFile().delete();
    }

    @Override public boolean deleteStore(Mailbox mbox) {
        // TODO Auto-generated method stub
        return false;
    }

}
