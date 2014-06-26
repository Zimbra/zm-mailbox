/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
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
package com.zimbra.cs.store.http;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.store.external.ExternalStoreManager;

public abstract class HttpStoreManager extends ExternalStoreManager {

    protected abstract String getPostUrl(Mailbox mbox);
    protected abstract String getGetUrl(Mailbox mbox, String locator);
    protected abstract String getDeleteUrl(Mailbox mbox, String locator);
    protected abstract String getLocator(PostMethod post, String postDigest, long postSize, Mailbox mbox) throws ServiceException, IOException;

    @Override
    public String writeStreamToStore(InputStream in, long actualSize, Mailbox mbox) throws IOException,
                    ServiceException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("SHA-256 digest not found", e);
        }
        ByteUtil.PositionInputStream pin = new ByteUtil.PositionInputStream(new DigestInputStream(in, digest));

        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        PostMethod post = new PostMethod(getPostUrl(mbox));
        try {
            HttpClientUtil.addInputStreamToHttpMethod(post, pin, actualSize, "application/octet-stream");
            int statusCode = HttpClientUtil.executeMethod(client, post);
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_NO_CONTENT) {
                return getLocator(post, ByteUtil.encodeFSSafeBase64(digest.digest()), pin.getPosition(), mbox);
            } else {
                throw ServiceException.FAILURE("error POSTing blob: " + post.getStatusText(), null);
            }
        } finally {
            post.releaseConnection();
        }
    }

    @Override
    public InputStream readStreamFromStore(String locator, Mailbox mbox)
                    throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        GetMethod get = new GetMethod(getGetUrl(mbox, locator));
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
        DeleteMethod delete = new DeleteMethod(getDeleteUrl(mbox, locator));
        try {
            int statusCode = HttpClientUtil.executeMethod(client, delete);
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_NO_CONTENT) {
                return true;
            } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return false;
            } else {
                throw new IOException("unexpected return code during blob DELETE: " + delete.getStatusText());
            }
        } finally {
            delete.releaseConnection();
        }
    }
}
