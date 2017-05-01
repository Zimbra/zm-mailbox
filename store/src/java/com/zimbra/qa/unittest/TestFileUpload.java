/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.JaxbUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class TestFileUpload {
    @Rule
    public TestName testInfo = new TestName();
    private static String USER_NAME = null;
    private static final String NAME_PREFIX = TestFileUpload.class.getSimpleName();
    private static final String FILE_NAME = "my_zimlet.zip";
    private static String RESP_STR = "window.parent._uploadManager.loaded";
    private static String ADMIN_UPLOAD_URL = "/service/upload";

    @Before
    public void setUp() throws Exception {
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user";		
        cleanUp();
        TestUtil.createAccount(USER_NAME);
    }
    @Test
    public void testUnauthorizedExtended() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String uriString = mbox.getUploadURI().toString().replace("fmt=raw", "fmt=extended");
        URI uri = new URI(uriString);
        String responseContent = postAndVerify(mbox, uri, true);
        Assert.assertTrue(responseContent, responseContent.contains("401,"));
    }
    @Test
    public void testUnauthorizedRaw() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, true);
        Assert.assertTrue(responseContent, responseContent.startsWith("401,"));
    }
    @Test
    public void testRaw() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, false);
        Assert.assertTrue(responseContent, responseContent.startsWith("200,"));
    }
    @Test
    public void testRawEmpty() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, false, "rawEmpty", null);
        Assert.assertTrue(responseContent, responseContent.startsWith("204,"));
    }

    @Test
    public void testAdminUploadWithCsrfInHeader() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest req = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        req.setCsrfSupported(true);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(req, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        String csrfToken = authResp.getCsrfToken();
        int port = 7071;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
        }
        String Url = "https://localhost:" + port + ADMIN_UPLOAD_URL;
        PostMethod post = new PostMethod(Url);
        FilePart part = new FilePart(FILE_NAME, new ByteArrayPartSource(FILE_NAME, "some file content".getBytes()));
        String contentType = "application/x-msdownload";
        part.setContentType(contentType);
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpState state = new HttpState();
        state.addCookie(new org.apache.commons.httpclient.Cookie("localhost", ZimbraCookie.authTokenCookieName(true),
                authToken, "/", null, false));
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        client.setState(state);
        post.setRequestEntity(new MultipartRequestEntity(new Part[] { part }, post.getParams()));
        post.addRequestHeader(Constants.CSRF_TOKEN, csrfToken);
        int statusCode = HttpClientUtil.executeMethod(client, post);
        Assert.assertEquals("This request should succeed. Getting status code " + statusCode, HttpStatus.SC_OK, statusCode);
        String resp = post.getResponseBodyAsString();
        Assert.assertNotNull("Response should not be empty", resp);
        Assert.assertTrue("Incorrect HTML response", resp.contains(RESP_STR));
    }

    @Test
    public void testMissingCsrfAdminUpload() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest req = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        req.setCsrfSupported(true);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(req, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        int port = 7071;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
        }
        String Url = "https://localhost:" + port + ADMIN_UPLOAD_URL;
        PostMethod post = new PostMethod(Url);
        FilePart part = new FilePart(FILE_NAME, new ByteArrayPartSource(FILE_NAME, "some file content".getBytes()));
        String contentType = "application/x-msdownload";
        part.setContentType(contentType);
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpState state = new HttpState();
        state.addCookie(new org.apache.commons.httpclient.Cookie("localhost", ZimbraCookie.authTokenCookieName(true),
                authToken, "/", null, false));
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        client.setState(state);
        post.setRequestEntity(new MultipartRequestEntity(new Part[] { part }, post.getParams()));
        int statusCode = HttpClientUtil.executeMethod(client, post);
        Assert.assertEquals("This request should succeed. Getting status code " + statusCode, HttpStatus.SC_OK, statusCode);
        String resp = post.getResponseBodyAsString();
        Assert.assertNotNull("Response should not be empty", resp);
        Assert.assertTrue("Incorrect HTML response", resp.contains(RESP_STR));
    }

    @Test
    public void testAdminUploadWithCsrfInFormField() throws Exception {
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getAdminSoapUrl());
        com.zimbra.soap.admin.message.AuthRequest req = new com.zimbra.soap.admin.message.AuthRequest(
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
        req.setCsrfSupported(true);
        Element response = transport.invoke(JaxbUtil.jaxbToElement(req, SoapProtocol.SoapJS.getFactory()));
        com.zimbra.soap.admin.message.AuthResponse authResp = JaxbUtil.elementToJaxb(response);
        String authToken = authResp.getAuthToken();
        String csrfToken = authResp.getCsrfToken();
        int port = 7071;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
        }
        String Url = "https://localhost:" + port + ADMIN_UPLOAD_URL;
        PostMethod post = new PostMethod(Url);
        FilePart part = new FilePart(FILE_NAME, new ByteArrayPartSource(FILE_NAME, "some file content".getBytes()));
        Part csrfPart = new StringPart("csrfToken", csrfToken);
        String contentType = "application/x-msdownload";
        part.setContentType(contentType);
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpState state = new HttpState();
        state.addCookie(new org.apache.commons.httpclient.Cookie("localhost", ZimbraCookie.authTokenCookieName(true),
                authToken, "/", null, false));
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        client.setState(state);
        post.setRequestEntity(new MultipartRequestEntity(new Part[] { part, csrfPart }, post.getParams()));
        int statusCode = HttpClientUtil.executeMethod(client, post);
        Assert.assertEquals("This request should succeed. Getting status code " + statusCode, HttpStatus.SC_OK, statusCode);
        String resp = post.getResponseBodyAsString();
        Assert.assertNotNull("Response should not be empty", resp);
        Assert.assertTrue("Incorrect HTML response", resp.contains(RESP_STR));
    }

    /**
     * Confirms that <tt>requestId</tt> parameter values are restricted as desired.
     * See bug 99914 and bug 40377.
     * @throws Exception
     */
    @Test
    public void testRequestIdScript() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, false, "<script></script>", "anything");
        Assert.assertFalse("Response does not contain 'script': " + responseContent, responseContent.contains("script"));
        Assert.assertTrue(responseContent, responseContent.startsWith("400,"));
    }
    @Test
    public void testRequestIdAlert() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        URI uri = mbox.getUploadURI();
        String responseContent = postAndVerify(mbox, uri, false, "alert(1)", null);
        Assert.assertFalse("Response does not contain 'alert': " + responseContent, responseContent.contains("alert"));
        Assert.assertTrue(responseContent, responseContent.startsWith("400,"));
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
        Assert.assertEquals(200, status);

        String contentType = getHeaderValue(post, "Content-Type");
        Assert.assertTrue(contentType, contentType.startsWith("text/html"));
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
    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
    }

    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestFileUpload.class);
    }
}
