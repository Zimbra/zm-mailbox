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
import java.io.OutputStream;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.HeadMethod;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.BufferingIncomingBlob;
import com.zimbra.cs.store.triton.TritonBlobStoreManager.HashType;

/**
 * IncomingBlob implementation which streams data directly to Triton using TritonIncomingOutputStream
 *
 */
public class TritonIncomingBlob extends BufferingIncomingBlob {
    private final String baseUrl;
    private final MozyServerToken serverToken;
    private final TritonUploadUrl uploadUrl;
    private final HashType hashType;
    private final MessageDigest digest;

    public TritonIncomingBlob(String id, String baseUrl, BlobBuilder blobBuilder, Object ctx, MessageDigest digest, HashType hashType) throws ServiceException, IOException {
        super(id, blobBuilder, ctx);
        this.digest = digest;
        this.hashType = hashType;
        this.baseUrl = baseUrl;
        serverToken = new MozyServerToken("foo");
        uploadUrl = new TritonUploadUrl();
    }

    @Override
    public OutputStream getAppendingOutputStream() {
        lastAccessTime = System.currentTimeMillis();
        return new TritonIncomingOutputStream(blobBuilder, digest, hashType, baseUrl, uploadUrl, serverToken);
    }

    @Override
    public long getCurrentSize() throws IOException, ServiceException {
        long internalSize = super.getCurrentSize();

        if (TritonBlobStoreManager.RESUMABLE_IMPLEMENTED) { //TODO: temporary if block; make it permanent once support is available
            //in normal case will be the same as the size we have locally
            //if it is different it means something went wrong between HF server and TDS
            //e.g. client uploads 10 bytes to HF but error causes only 9 to reach TDS
            //client needs to resume from beginning so we'll throw exception
            HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
            HeadMethod head = new HeadMethod(baseUrl + uploadUrl);
            ZimbraLog.store.info("heading %s", head.getURI());
            try {
                head.addRequestHeader(TritonHeaders.SERVER_TOKEN, serverToken.getToken());
                int statusCode = HttpClientUtil.executeMethod(client, head);
                if (statusCode == HttpStatus.SC_OK) {
                    String contentLength = head.getRequestHeader(TritonHeaders.CONTENT_LENGTH).getValue();
                    try {
                        long remoteSize = Long.valueOf(contentLength);
                        if (remoteSize != internalSize) {
                            //inconsistent sizes between HF and TDS, throw out the download
                            //could also throw out digest and roll blob builder back to size recorded remotely, but that is premature optimization just now
                            throw ServiceException.FAILURE("mismatch between local and remote content sizes. Client must restart upload", null);
                        }
                    } catch (NumberFormatException nfe) {
                        throw ServiceException.FAILURE("Content length can't be parsed to Long", nfe);
                    }
                    serverToken.setToken(head);
                } else {
                    ZimbraLog.store.error("failed with code %d response: %s", statusCode, head.getResponseBodyAsString());
                    throw ServiceException.FAILURE("unable to store blob "+statusCode + ":" + head.getStatusText(), null);
                }
            } finally {
                head.releaseConnection();
            }
        }
        return internalSize;
    }

    @Override
    public Blob getBlob() throws IOException, ServiceException {
        String locator = Hex.encodeHexString((digest.digest()));
        return new TritonBlob(blobBuilder.finish(), locator);
    }
}
