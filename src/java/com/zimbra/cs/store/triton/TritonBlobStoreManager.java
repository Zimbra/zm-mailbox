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
import com.zimbra.cs.store.IncomingBlob;
import com.zimbra.cs.store.external.ContentAddressableStoreManager;

/**
 * StoreManager implementation which uses the TDS Blob API for storing and retrieving blobs
 */
public class TritonBlobStoreManager extends ContentAddressableStoreManager {

    private String url;
    enum HashType {SHA0, SHA256};
    private HashType hashType;
    static boolean RESUMABLE_IMPLEMENTED = false; //temporary flag so we don't use resumable TDS API which isn't there yet, but we can still compile and make sure our code makes sense

    @VisibleForTesting
    public TritonBlobStoreManager(String url, String hashType) {
        super();
        this.url = url;
        this.hashType = HashType.valueOf(hashType);
    }

    public TritonBlobStoreManager() {
        super();
    }

    @Override
    public void startup() throws IOException, ServiceException {
        if (url == null) {
            url = LC.triton_store_url.value();
        }
        if (hashType == null) {
            hashType = HashType.valueOf(LC.triton_hash_type.value());
        }
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
            MessageDigest digest = newDigest();
            DigestInputStream dis = new DigestInputStream(blob.getInputStream(), digest);
            while (dis.read() >= 0) {
            }
            byte[] hash = digest.digest();
            return Hex.encodeHexString(hash);
        }
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
            default : throw ServiceException.FAILURE("Unknown hashType "+hashType, null);
        }
        return digest;
    }

    @Override
    protected void writeStreamToStore(InputStream in, long actualSize, Mailbox mbox, String locator) throws IOException, ServiceException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        if (actualSize <= 0) {
            throw ServiceException.FAILURE("TDS does not allow upload without size yet, need resumable upload", null);
        }
        PostMethod post = new PostMethod(url + "/blob/");
        ZimbraLog.store.info("posting to %s", post.getURI());
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
                throw ServiceException.FAILURE("unable to store blob "+statusCode + ":" + post.getStatusText(), null);
            }
        } finally {
            post.releaseConnection();
        }
    }

    @Override
    public InputStream readStreamFromStore(String locator, Mailbox mbox)
                    throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(url + "/blob/"+locator);
        get.addRequestHeader(TritonHeaders.HASH_TYPE, hashType.toString());
        ZimbraLog.store.info("getting %s", get.getURI());
        int statusCode = HttpClientUtil.executeMethod(client, get);
        if (statusCode == HttpStatus.SC_OK) {
            return new UserServlet.HttpInputStream(get);
        } else {
            get.releaseConnection();
            throw new IOException("unexpected return code during blob GET: " + get.getStatusText());
        }
    }

    @Override
    public boolean deleteFromStore(String locator, Mailbox mbox)
                    throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        DeleteMethod delete = new DeleteMethod(url + "/blob/"+locator);
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
    public IncomingBlob newIncomingBlob(String id, Object ctxt) throws IOException, ServiceException {
        return new TritonIncomingBlob(id, url, getBlobBuilder(), ctxt, newDigest(), hashType);
    }
}
