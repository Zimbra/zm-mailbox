/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store.triton;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.client.HttpClient;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.httpclient.InputStreamRequestHttpRetryHandler;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.external.ExternalResumableOutputStream;
import com.zimbra.cs.store.triton.TritonBlobStoreManager.HashType;

/**
 * Output stream that writes to TDS, calculates digest, and buffers to local BlobBuilder
 *
 */
public class TritonIncomingOutputStream extends ExternalResumableOutputStream {

    protected final String baseUrl;
    private final TritonUploadUrl uploadUrl;
    protected final MessageDigest digest;
    protected final HashType hashType;
    protected final MozyServerToken serverToken;
    protected AtomicLong written;
    protected ByteArrayOutputStream baos;

    public TritonIncomingOutputStream(BlobBuilder blobBuilder, MessageDigest digest, HashType hashType, String baseUrl, TritonUploadUrl uploadUrl, MozyServerToken serverToken, AtomicLong written) {
        super(blobBuilder);
        this.baseUrl = baseUrl;
        this.uploadUrl = uploadUrl;
        this.hashType = hashType;
        this.serverToken = serverToken;
        this.digest = digest;
        this.written = written;
        this.baos = new ByteArrayOutputStream(LC.triton_upload_buffer_size.intValue());
    }

    @Override
    protected void writeToExternal(byte[] b, int off, int len) throws IOException {
        baos.write(b, off, len);
        if (baos.size() >= LC.triton_upload_buffer_size.intValue()) {
            sendHttpData();
        }
    }

    @Override
    public void flush() throws IOException {
        if (baos.size() > 0) {
            sendHttpData();
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (baos.size() > 0) {
            sendHttpData();
        }
    }

    private void sendHttpData() throws IOException {
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        clientBuilder.setRetryHandler(new InputStreamRequestHttpRetryHandler());
        HttpClient client = clientBuilder.build();
        HttpPost post;
        boolean started = false;
        if (uploadUrl.isInitialized()) {
            started = true;
            post = new HttpPost(baseUrl + uploadUrl);
        } else {
            post = new HttpPost(baseUrl + "/blob");
        }
        try {
            ZimbraLog.store.info("posting to %s",post.getURI());
            InputStreamEntity entity = new InputStreamEntity(new ByteArrayInputStream(baos.toByteArray()), baos.size(), ContentType.APPLICATION_OCTET_STREAM);
            post.setEntity(entity);
            post.addHeader(TritonHeaders.CONTENT_LENGTH, baos.size()+"");
            post.addHeader(TritonHeaders.HASH_TYPE, hashType.toString());
            post.addHeader("Content-Range", "bytes " + written.longValue() + "-" + (written.longValue()+baos.size()-1)+ "/*");
            if (serverToken.getToken() != null) {
                post.addHeader(TritonHeaders.SERVER_TOKEN, serverToken.getToken());
            }
            HttpResponse httpResp = HttpClientUtil.executeMethod(client, post);
            int statusCode = httpResp.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                handleResponse(httpResp);
            } else if (!started && statusCode == HttpStatus.SC_SEE_OTHER) {
                started = true;
                uploadUrl.setUploadUrl(httpResp.getFirstHeader(TritonHeaders.LOCATION).getValue());
                handleResponse(httpResp);
            } else {
                throw new IOException("Unable to append, bad response code "+statusCode);
            }
        } catch (HttpException e) {
            throw new IOException("unexpected error during sendHttpData() operation: " + e.getMessage());
        } finally {
            post.releaseConnection();
        }
        baos = new ByteArrayOutputStream(LC.triton_upload_buffer_size.intValue());
    }

  private void handleResponse(HttpResponse httpResp) throws IOException {
      serverToken.setToken(httpResp);
      written.set(written.longValue() + baos.size());
      digest.update(baos.toByteArray());
  }
}
