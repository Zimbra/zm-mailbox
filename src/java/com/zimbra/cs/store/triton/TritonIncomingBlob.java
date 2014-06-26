/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store.triton;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;

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
import com.zimbra.cs.store.external.ExternalResumableIncomingBlob;
import com.zimbra.cs.store.external.ExternalResumableOutputStream;
import com.zimbra.cs.store.triton.TritonBlobStoreManager.HashType;

/**
 * IncomingBlob implementation which streams data directly to Triton using TritonIncomingOutputStream
 *
 */
public class TritonIncomingBlob extends ExternalResumableIncomingBlob {
    private final String baseUrl;
    private final MozyServerToken serverToken;
    private final TritonUploadUrl uploadUrl;
    private final HashType hashType;
    private final MessageDigest digest;
    private TritonIncomingOutputStream outStream;
    private AtomicLong written;

    public TritonIncomingBlob(String id, String baseUrl, BlobBuilder blobBuilder, Object ctx, MessageDigest digest, HashType hashType) throws ServiceException, IOException {
        super(id, blobBuilder, ctx);
        this.digest = digest;
        this.hashType = hashType;
        this.baseUrl = baseUrl;
        serverToken = new MozyServerToken();
        uploadUrl = new TritonUploadUrl();
        written = new AtomicLong(0);
    }

    @Override
    protected ExternalResumableOutputStream getAppendingOutputStream(
                    BlobBuilder blobBuilder) {
        lastAccessTime = System.currentTimeMillis();
        outStream = new TritonIncomingOutputStream(blobBuilder, digest, hashType, baseUrl, uploadUrl, serverToken, written);
        return outStream;
    }

    @Override
    public Blob getBlob() throws IOException, ServiceException {
        if (outStream != null) {
            outStream.close();
        }
        String locator = Hex.encodeHexString((digest.digest()));
        return new TritonBlob(blobBuilder.finish(), locator, uploadUrl.toString(), serverToken);
    }

    @Override
    protected long getRemoteSize() throws IOException {
        outStream.flush();
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HeadMethod head = new HeadMethod(baseUrl + uploadUrl);
        ZimbraLog.store.info("heading %s", head.getURI());
        try {
            head.addRequestHeader(TritonHeaders.SERVER_TOKEN, serverToken.getToken());
            int statusCode = HttpClientUtil.executeMethod(client, head);
            if (statusCode == HttpStatus.SC_OK) {
                String contentLength = head.getResponseHeader(TritonHeaders.CONTENT_LENGTH).getValue();
                long remoteSize = -1;
                try {
                    remoteSize = Long.valueOf(contentLength);
                } catch (NumberFormatException nfe) {
                    throw new IOException("Content length can't be parsed to Long", nfe);
                }
                return remoteSize;
            } else {
                ZimbraLog.store.error("failed with code %d response: %s", statusCode, head.getResponseBodyAsString());
                throw new IOException("unable to head blob "+statusCode + ":" + head.getStatusText(), null);
            }
        } finally {
            head.releaseConnection();
        }
    }
}
