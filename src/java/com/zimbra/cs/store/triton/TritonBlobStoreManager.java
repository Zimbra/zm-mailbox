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
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.external.ExternalUploadedBlob;
import com.zimbra.cs.store.external.ExternalResumableIncomingBlob;
import com.zimbra.cs.store.external.ExternalResumableUpload;
import com.zimbra.cs.store.external.SisStore;

/**
 * StoreManager implementation which uses the TDS Blob API for storing and retrieving blobs
 */
public class TritonBlobStoreManager extends SisStore implements ExternalResumableUpload  {

    private String url;
    private String blobApiUrl;
    enum HashType {SHA0, SHA256};
    private HashType hashType;
    private String emptyLocator;

    @VisibleForTesting
    public TritonBlobStoreManager(String url, HashType hashType) {
        super();
        this.url = url;
        this.hashType = hashType;
    }

    public TritonBlobStoreManager() {
        super();
    }

    @Override
    public void startup() throws IOException, ServiceException {
        if (url == null) {
            url = LC.triton_store_url.value();
        }
        blobApiUrl = url + "/blob/";
        if (hashType == null) {
            hashType = HashType.valueOf(LC.triton_hash_type.value());
        }
        MessageDigest digest = newDigest();
        emptyLocator = getLocator(digest.digest());
        ZimbraLog.store.info("TDS Blob store manager using url %s hashType %s",url, hashType);
        super.startup();
    }

    @Override
    protected String getLocator(Blob blob) throws ServiceException, IOException {
        if (blob instanceof TritonBlob) {
            //happily caller used IncomingBlob API
            return ((TritonBlob) blob).getLocator();
        } else {
            //older call sites don't gain benefits of IncomingBlob
            return getLocator(getHash(blob));
        }
    }

    @Override
    public String getLocator(byte[] hash) {
        return Hex.encodeHexString(hash);
    }

    @Override
    public byte[] getHash(Blob blob) throws ServiceException, IOException {
        MessageDigest digest = newDigest();
        DigestInputStream dis = new DigestInputStream(blob.getInputStream(), digest);
        while (dis.read() >= 0) {
        }
        return digest.digest();
    }

    private MessageDigest newDigest() throws ServiceException {
        MessageDigest digest;
        switch (hashType) {
            case SHA256:
                try {
                    digest = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    throw ServiceException.FAILURE("unable to load SHA256 digest due to exception", e);
                }
                break;
            case SHA0:
                try {
                    //SHA0 is not implemented in base Java classes since it was withdrawn in 1993 before Java was released
                    //we use cryptix here for demo, but we aren't bothering with legal approval since we're requiring TDS to switch to SHA-256
                    //so need to drop cryptix32.jar into /opt/zimbra/jetty/webapps/service/WEB-INF/lib during install
                    digest = (MessageDigest) Class.forName("cryptix.provider.md.SHA0").newInstance();
                } catch (Exception e) {
                    throw ServiceException.FAILURE("unable to load SHA0 digest due to exception", e);
                }
                break;
            default : throw ServiceException.FAILURE("Unknown hashType " + hashType, null);
        }
        return digest;
    }

    @Override
    protected void writeStreamToStore(InputStream in, long actualSize, Mailbox mbox, String locator) throws IOException, ServiceException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        if (actualSize < 0) {
            throw ServiceException.FAILURE("Must use resumable upload (i.e. StoreManager.newIncomingBlob()) if size is unknown", null);
        }
        else if (actualSize == 0) {
            ZimbraLog.store.info("storing empty blob");
            return; //don't bother writing empty file to remote
        }
        PostMethod post = new PostMethod(blobApiUrl);
        ZimbraLog.store.info("posting to %s with locator %s", post.getURI(), locator);
        try {
            HttpClientUtil.addInputStreamToHttpMethod(post, in, actualSize, "application/octet-stream");
            post.addRequestHeader(TritonHeaders.CONTENT_LENGTH, actualSize+"");
            post.addRequestHeader(TritonHeaders.OBJECTID, locator);
            post.addRequestHeader(TritonHeaders.HASH_TYPE, hashType.toString());
            int statusCode = HttpClientUtil.executeMethod(client, post);
            if (statusCode == HttpStatus.SC_CREATED) {
                return;
            } else {
                ZimbraLog.store.error("failed with code %d response: %s", statusCode, post.getResponseBodyAsString());
                throw ServiceException.FAILURE("unable to store blob " + statusCode + ":" + post.getStatusText(), null);
            }
        } finally {
            post.releaseConnection();
        }
    }

    @Override
    public InputStream readStreamFromStore(String locator, Mailbox mbox)
                    throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(blobApiUrl + locator);
        get.addRequestHeader(TritonHeaders.HASH_TYPE, hashType.toString());
        ZimbraLog.store.info("getting %s", get.getURI());
        int statusCode = HttpClientUtil.executeMethod(client, get);
        if (statusCode == HttpStatus.SC_OK) {
            return new UserServlet.HttpInputStream(get);
        } else {
            get.releaseConnection();
            if (statusCode == HttpStatus.SC_NOT_FOUND && emptyLocator.equals(locator)) {
                //empty file edge case. could compare hash before this, but that hurts perf. for normal case
                ZimbraLog.store.info("returning input stream for empty blob");
                return new ByteArrayInputStream(new byte[0]);
            } else {
                throw new IOException("unexpected return code during blob GET: " + get.getStatusText());
            }
        }
    }

    @Override
    public boolean deleteFromStore(String locator, Mailbox mbox)
                    throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        DeleteMethod delete = new DeleteMethod(blobApiUrl + locator);
        delete.addRequestHeader(TritonHeaders.HASH_TYPE, hashType.toString());
        try {
            ZimbraLog.store.info("deleting %s", delete.getURI());
            int statusCode = HttpClientUtil.executeMethod(client, delete);
            if (statusCode == HttpStatus.SC_OK) {
                return true;
            } else {
                throw new IOException("unexpected return code during blob DELETE: " + delete.getStatusText());
            }
        } finally {
            delete.releaseConnection();
        }
    }

    @Override
    public ExternalResumableIncomingBlob newIncomingBlob(String id, Object ctxt) throws IOException, ServiceException {
        return new TritonIncomingBlob(id, url, getBlobBuilder(), ctxt, newDigest(), hashType);
    }

    @Override
    public String finishUpload(ExternalUploadedBlob blob) throws IOException, ServiceException {
        TritonBlob tb = (TritonBlob) blob;
        PostMethod post = new PostMethod(url + tb.getUploadId());
        ZimbraLog.store.info("posting to %s with locator %s", post.getURI(), tb.getLocator());
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        try {
            post.addRequestHeader(TritonHeaders.OBJECTID, tb.getLocator());
            post.addRequestHeader(TritonHeaders.HASH_TYPE, hashType.toString());
            post.addRequestHeader(TritonHeaders.SERVER_TOKEN, tb.getServerToken().getToken());
            int statusCode = HttpClientUtil.executeMethod(client, post);
            if (statusCode == HttpStatus.SC_CREATED) {
                return tb.getLocator();
            } else {
                ZimbraLog.store.error("failed with code %d response: %s", statusCode, post.getResponseBodyAsString());
                throw ServiceException.FAILURE("unable to store blob " + statusCode + ":" + post.getStatusText(), null);
            }
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * Run SIS operation against remote server. If a blob already exists for the locator the remote ref count is incremented.
     * @param hash: The content hash of the blob
     * @return true if blob already exists, false if not
     * @throws IOException
     * @throws ServiceException
     */
    private boolean sisCreate(byte[] hash) throws IOException {
        String locator = getLocator(hash);
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        PostMethod post = new PostMethod(blobApiUrl + locator);
        ZimbraLog.store.info("SIS create URL: %s", post.getURI());
        try {
            post.addRequestHeader(TritonHeaders.HASH_TYPE, hashType.toString());
            int statusCode = HttpClientUtil.executeMethod(client, post);
            if (statusCode == HttpStatus.SC_CREATED) {
                return true; //exists, ref count incremented
            } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                if (emptyLocator.equals(locator)) {
                    //empty file
                    return true;
                } else {
                    return false; //does not exist
                }
            } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                //does not exist, probably wrong hash algorithm
                ZimbraLog.store.warn("failed with code %d response: %s", statusCode, post.getResponseBodyAsString());
                return false;
            } else {
                //unexpected condition
                ZimbraLog.store.error("failed with code %d response: %s", statusCode, post.getResponseBodyAsString());
                throw new IOException("unable to SIS create " + statusCode + ":" + post.getStatusText(), null);
            }
        } finally {
            post.releaseConnection();
        }
    }

    @Override
    public Blob getSisBlob(byte[] hash) throws IOException {
        if (sisCreate(hash)) {
            //null Mailbox arg is OK here, by definition SIS store cannot partition by mbox
            return getLocalBlob(null, getLocator(hash));
        } else {
            return null;
        }
    }

    @Override
    public boolean supports(StoreFeature feature) {
        switch (feature) {
            case SINGLE_INSTANCE_SERVER_CREATE : return hashType == HashType.SHA256;
            default: return super.supports(feature);
        }
    }
}
