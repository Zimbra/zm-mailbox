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
import java.security.MessageDigest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.BlobBuilderOutputStream;
import com.zimbra.cs.store.triton.TritonBlobStoreManager.HashType;

/**
 * Output stream that writes to TDS, calculates digest, and buffers to local BlobBuilder
 *
 */
public class TritonIncomingOutputStream extends BlobBuilderOutputStream {

    protected final String baseUrl;
    private final TritonUploadUrl uploadUrl;
    protected final MessageDigest digest;
    protected final HashType hashType;
    protected final MozyServerToken serverToken;

    public TritonIncomingOutputStream(BlobBuilder blobBuilder, MessageDigest digest, HashType hashType, String baseUrl, TritonUploadUrl uploadUrl, MozyServerToken serverToken) {
        super(blobBuilder);
        this.baseUrl = baseUrl;
        this.uploadUrl = uploadUrl;
        this.hashType = hashType;
        this.serverToken = serverToken;
        this.digest = digest;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (TritonBlobStoreManager.RESUMABLE_IMPLEMENTED) { //TODO: temporary if block; make it permanent once support is available
            ByteArrayRequestEntity reqEntity = new ByteArrayRequestEntity(b);
            HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
            PostMethod post;
            boolean started = false;
            if (uploadUrl.isInitialized()) {
                started = true;
                post = new PostMethod(baseUrl + uploadUrl);
            } else {
                post = new PostMethod(baseUrl + "/blob");
            }
            try {
                ZimbraLog.store.info("posting to %s",post.getURI());
                post.setRequestEntity(reqEntity);
                post.addRequestHeader(TritonHeaders.CONTENT_LENGTH, len+"");
                post.addRequestHeader(TritonHeaders.HASH_TYPE, hashType.toString());
                post.addRequestHeader(TritonHeaders.SERVER_TOKEN, serverToken.getToken());
                int statusCode = HttpClientUtil.executeMethod(client, post);
                if (statusCode == HttpStatus.SC_OK) {
                    handleResponse(post, b, off, len);
                } else if (!started && statusCode == HttpStatus.SC_SEE_OTHER) {
                    started = true;
                    uploadUrl.setUploadUrl(post.getResponseHeader(TritonHeaders.LOCATION).getValue());
                    handleResponse(post, b, off, len);
                } else {
                    throw new IOException("Unable to append, bad response code "+statusCode);
                }
            } finally {
                post.releaseConnection();
            }
        } else {
            super.write(b, off, len);
            digest.update(b, off, len);
        }
    }

    private void handleResponse(PostMethod post, byte[] b, int off, int len) throws IOException {
        serverToken.setToken(post);
        super.write(b, off, len);
        digest.update(b, off, len);
    }
}
