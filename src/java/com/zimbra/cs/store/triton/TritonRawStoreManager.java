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

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.external.ContentAddressableStoreManager;


/**
 * StoreManager implementation which uses the TDS RAW API for storing and retrieving blobs
 * This implementation is intended *only* for demonstration and will eventually be superseded by TritonBlobStoreManager
 */
public class TritonRawStoreManager extends ContentAddressableStoreManager {

    private String url;

    @VisibleForTesting
    public TritonRawStoreManager(String url) {
        super();
        this.url = url;
    }

    public TritonRawStoreManager() {
        super();
    }

    @Override
    public void startup() throws IOException, ServiceException {
        if (url == null) {
            url = LC.triton_store_url.value();
        }
        ZimbraLog.store.info("TDS RAW store manager using url %s",url);
        super.startup();
    }

    private String toHex(byte[] a) {
        return Hex.encodeHexString(a).toLowerCase();
    }

    @Override
    protected String getLocator(Blob blob) throws ServiceException, IOException {
        //TODO: switch to SHA-256 once Triton implements it
//        return DigestUtils.sha256Hex(blob.getInputStream())+".dat";

        MessageDigest digest;
        try {
            //SHA0 is not implemented in base Java classes since it was withdrawn in 1993 before Java was released
            //we use cryptix here for demo, but we aren't bothering with legal approval since we're requiring TDS to switch to SHA-256
            //so need to drop cryptix32.jar into /opt/zimbra/jetty/webapps/service/WEB-INF/lib during install
            digest = (MessageDigest) Class.forName("cryptix.provider.md.SHA0").newInstance();
        } catch (Exception e) {
            throw ServiceException.FAILURE("unable to load SHA0 digest due to exception", e);
        }
        DigestInputStream dis = new DigestInputStream(blob.getInputStream(), digest);
        while (dis.read() >= 0) {
        }
        byte[] hash = digest.digest();
        return toHex(hash) + ".dat";
    }


    @Override
    protected void writeStreamToStore(InputStream in, long actualSize, Mailbox mbox, String locator) throws IOException, ServiceException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        PutMethod put = new PutMethod(url + "/raw/"+locator);
        ZimbraLog.store.info("putting to %s", put.getURI());
        try {
            HttpClientUtil.addInputStreamToHttpMethod(put, in, actualSize, "application/octet-stream");
            int statusCode = HttpClientUtil.executeMethod(client, put);
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_NO_CONTENT) {
                return;
            } else {
                ZimbraLog.store.error("failed with code %d response: %s", statusCode, put.getResponseBodyAsString());
                throw ServiceException.FAILURE("unable to store blob "+statusCode + ":" + put.getStatusText(), null);
            }
        } finally {
            put.releaseConnection();
        }
    }

    @Override
    public InputStream readStreamFromStore(String locator, Mailbox mbox)
                    throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(url + "/raw/"+locator);
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
        //not implemented, just stub for demo purposes
        return true;
//
//        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
//        DeleteMethod delete = new DeleteMethod(url + "/raw/"+locator);
//        try {
//            ZimbraLog.store.info("deleting %s", delete.getURI());
//            int statusCode = HttpClientUtil.executeMethod(client, delete);
//            if (statusCode == HttpStatus.SC_OK) {
//                return true;
//            } else {
//                throw new IOException("unexpected return code during blob DELETE: " + delete.getStatusText());
//            }
//        } finally {
//            delete.releaseConnection();
//        }
    }
}
