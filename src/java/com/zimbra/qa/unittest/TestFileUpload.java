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
package com.zimbra.qa.unittest;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

    public void setUp() throws Exception {
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

    public void testRaw() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, false);
        assertTrue(responseContent, responseContent.startsWith("200,"));
    }

    public void testRawEmpty() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, false, "rawEmpty", null);
        assertTrue(responseContent, responseContent.startsWith("204,"));
    }

    /**
     * Confirms that <tt>requestId</tt> parameter values are restricted as desired.
     * See bug 99914 and bug 40377.
     * @throws Exception
     */
    public void testRequestIdScript() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, false, "<script></script>", "anything");
        assertFalse("Response does not contain 'script': " + responseContent, responseContent.contains("script"));
        assertTrue(responseContent, responseContent.startsWith("400,"));
    }

    public void testRequestIdAlert() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, false, "alert(1)", null);
        assertFalse("Response does not contain 'alert': " + responseContent, responseContent.contains("alert"));
        assertTrue(responseContent, responseContent.startsWith("400,"));
    }

    private String postAndVerify(ZMailbox mbox, URI uri, boolean clearCookies)
    throws IOException {
        return postAndVerify(mbox, uri, clearCookies, "myReqId", "some data");
    }

    private String postAndVerify(ZMailbox mbox, URI uri, boolean clearCookies, String requestId, String attContent)
    throws IOException {
        HttpClient client = mbox.getHttpClient(uri);
        if (clearCookies) {
            client.getState().clearCookies();
        }

        List<Part> parts = new ArrayList<Part>();
        parts.add(new StringPart("requestId", requestId));
        if (attContent != null) {
            parts.add(mbox.createAttachmentPart("test.txt", attContent.getBytes()));
        }

        PostMethod post = new PostMethod(uri.toString());
        post.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), post.getParams()));
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
            value = header[0].getName();
        }
        return value;
    }

    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestFileUpload.class);
    }
}
