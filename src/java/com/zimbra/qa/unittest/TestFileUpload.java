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
package com.zimbra.qa.unittest;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.HttpClientUtil;

public class TestFileUpload
extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestFileUpload.class.getSimpleName();

    public void setUp()
    throws Exception {
        cleanUp();
    }

    public void testUnauthorizedExtended() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String uriString = mbox.getUploadURI().toString().replace("fmt=raw", "fmt=extended");
        URI uri = new URI(uriString);
        String responseContent = postAndVerify(mbox, uri, true);
        assertTrue(responseContent, responseContent.contains("401,"));
    }

    public void testUnauthorizedRaw() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, true);
        assertTrue(responseContent, responseContent.startsWith("401,"));
    }

    /**
     * Confirms that &lt;script&gt; tags are JavaScript-encoded when passed
     * to the <tt>requestId</tt> parameter.  See bug 40377.
     * @throws Exception
     */
    public void testRequestId()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        HttpClient client = mbox.getHttpClient(uri);

        Part attachmentPart = mbox.createAttachmentPart("test.txt", new byte[10]);
        Part requestIdPart = new StringPart("requestId", "<script></script>");
        Part[] parts = new Part[] { attachmentPart, requestIdPart };

        PostMethod post = new PostMethod(uri.toString());
        post.setRequestEntity( new MultipartRequestEntity(parts, post.getParams()) );
        int statusCode = HttpClientUtil.executeMethod(client, post);
        assertEquals(HttpServletResponse.SC_OK, statusCode);

        String response = post.getResponseBodyAsString();
        assertTrue("Response does not contain 'script': " + response, response.contains("script"));
        assertFalse("Response contains '<script>': " + response, response.contains("<script>"));

        post.releaseConnection();
    }

    public void testRaw() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, false);
        assertTrue(responseContent, responseContent.startsWith("200,"));
    }

    private String postAndVerify(ZMailbox mbox, URI uri, boolean clearCookies)
    throws IOException {
        HttpClient client = mbox.getHttpClient(uri);
        if (clearCookies) {
            client.getState().clearCookies();
        }

        Part attachmentPart = mbox.createAttachmentPart("test.txt", new byte[10]);
        Part requestIdPart = new StringPart("requestId", "<script></script>");
        Part[] parts = new Part[] { attachmentPart, requestIdPart };

        PostMethod post = new PostMethod(uri.toString());
        post.setRequestEntity( new MultipartRequestEntity(parts, post.getParams()) );
        int status = HttpClientUtil.executeMethod(client, post);
        assertEquals(200, status);

        String contentType = getHeaderValue(post, "Content-Type");
        assertTrue(contentType, contentType.startsWith("text/html"));
        String content = post.getResponseBodyAsString();
        post.releaseConnection();
        return content;
    }

    private String getHeaderValue(HttpMethod method, String name) {
        HeaderElement[] header = method.getResponseHeader(name).getElements();
        String value = null;
        if(header.length > 0) {
         value = header[0].getName( );
        }
        return value;
    }

    public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestFileUpload.class);
    }
}
