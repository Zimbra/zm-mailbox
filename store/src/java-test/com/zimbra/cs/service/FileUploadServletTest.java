/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.servlet.http.Cookie;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.HeaderUtils.ByteBuilder;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.account.Auth;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.cs.servlet.CsrfFilter;
import com.zimbra.soap.SoapServlet;

public class FileUploadServletTest {
    private static FileUploadServlet servlet;
    private static Account testAccount;

    @BeforeClass
    public static void init() throws Exception {

        LC.zimbra_tmp_directory.setDefault("build/test");

        servlet = new FileUploadServlet();

        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createAccount("test@zimbra.com", "secret", attrs);
        testAccount = prov.get(Key.AccountBy.name, "test@zimbra.com");

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test2@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    public static final String boundary = "----WebKitFormBoundaryBf0g3B57jaNA7SC6";
    public static final String filename1 = "\u6771\u65e5\u672c\u5927\u9707\u707d.txt";
    public static final String filename2 = "\u6771\u5317\u5730\u65b9\u592a\u5e73\u6d0b\u6c96\u5730\u9707.txt";
    public static final String content1 = "3 \u6708 11 \u65e5\u5348\u5f8c 2 \u6642 46 \u5206\u3054\u308d\u3001\u30de\u30b0\u30cb\u30c1\u30e5\u30fc\u30c9 9.0";
    private static final String content2 = "\u884c\u65b9\u4e0d\u660e\u8005\u76f8\u8ac7\u30c0\u30a4\u30e4\u30eb: \u5ca9\u624b\u770c: 0120-801-471";

    public static ByteBuilder addFormField(ByteBuilder bb, String name, String value) {
        bb.append("--").append(boundary).append("\r\n");
        bb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
        bb.append("\r\n");
        bb.append(value == null ? "" : value).append("\r\n");
        return bb;
    }

    public static ByteBuilder addFormFile(ByteBuilder bb, String filename, String ctype, String contents) {
        bb.append("--").append(boundary).append("\r\n");
        bb.append("Content-Disposition: form-data; name=\"_attFile_\"; filename=\"").append(filename == null ? "" : filename).append("\"\r\n");
        bb.append("Content-Type: ").append(ctype == null ? "application/octet-stream" : ctype).append("\r\n");
        bb.append("\r\n");
        bb.append(contents == null ? "" : contents).append("\r\n");
        return bb;
    }

    public static ByteBuilder endForm(ByteBuilder bb) {
        return bb.append("--").append(boundary).append("--\r\n");
    }

    private List<Upload> uploadForm(byte[] form) throws Exception {
        URL url = new URL("http://localhost:7070/service/upload?fmt=extended");
        MockHttpServletRequest req = new MockHttpServletRequest(form, url, "multipart/form-data; boundary=" + boundary);
        HashMap<String, String> headersMap = new HashMap<String, String>();
        headersMap.put("Content-length", Integer.toString(form.length));
        req.headers = headersMap;
        MockHttpServletResponse resp = new MockHttpServletResponse();
        return servlet.handleMultipartUpload(req, resp, "extended", testAccount, false, null, true);
    }

    private void compareUploads(Upload up, String expectedFilename, byte[] expectedContent) throws Exception {
        Assert.assertEquals(expectedFilename, up.getName());
        Assert.assertArrayEquals(expectedContent, ByteUtil.getContent(up.getInputStream(), -1));
    }

    @Test
    public void testFilenames() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        addFormField(bb, "_charset_", "");
        addFormField(bb, "filename1", filename1);
        addFormFile(bb, filename1, "text/plain", content1);
        addFormField(bb, "filename2", filename2);
        addFormFile(bb, filename2, "text/plain", content2);
        addFormField(bb, "filename3", "");
        addFormFile(bb, "", null, null);
        endForm(bb);

        List<Upload> uploads = uploadForm(bb.toByteArray());
        Assert.assertEquals(2, uploads == null ? 0 : uploads.size());
        compareUploads(uploads.get(0), filename1, content1.getBytes(CharsetUtil.UTF_8));
        compareUploads(uploads.get(1), filename2, content2.getBytes(CharsetUtil.UTF_8));
    }

    @Test
    public void testConsecutiveFilenames() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        addFormField(bb, "_charset_", "");
        addFormField(bb, "filename1", filename1 + "\r\n" + filename2);
        addFormFile(bb, filename1, "text/plain", content1);
        addFormFile(bb, filename2, "text/plain", content2);
        addFormField(bb, "filename2", "");
        addFormFile(bb, "", null, null);
        addFormField(bb, "filename3", "");
        addFormFile(bb, "", null, null);
        endForm(bb);

        List<Upload> uploads = uploadForm(bb.toByteArray());
        Assert.assertEquals(2, uploads == null ? 0 : uploads.size());
        compareUploads(uploads.get(0), filename1, content1.getBytes(CharsetUtil.UTF_8));
        compareUploads(uploads.get(1), filename2, content2.getBytes(CharsetUtil.UTF_8));
    }

    @Test
    public void testExtraFilenames() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        addFormField(bb, "_charset_", "");
        addFormField(bb, "filename1", filename1 + "\r\nextra\r\ndata.txt");
        addFormFile(bb, filename1, "text/plain", content1);
        addFormField(bb, "filename2", filename2);
        addFormFile(bb, filename2, "text/plain", content2);
        addFormField(bb, "filename3", "bar.gif");
        addFormFile(bb, "", null, null);
        endForm(bb);

        List<Upload> uploads = uploadForm(bb.toByteArray());
        Assert.assertEquals(2, uploads == null ? 0 : uploads.size());
        compareUploads(uploads.get(0), filename1, content1.getBytes(CharsetUtil.UTF_8));
        compareUploads(uploads.get(1), filename2, content2.getBytes(CharsetUtil.UTF_8));
    }

    @Test
    public void testMissingFilenames() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        addFormField(bb, "_charset_", "");
        addFormField(bb, "filename1", "");
        addFormFile(bb, "x", "text/plain", content1);
        addFormFile(bb, "y", "text/plain", content2);
        addFormField(bb, "filename2", "");
        addFormFile(bb, "", null, null);
        addFormField(bb, "filename3", "");
        addFormFile(bb, "", null, null);
        endForm(bb);

        List<Upload> uploads = uploadForm(bb.toByteArray());
        Assert.assertEquals(2, uploads == null ? 0 : uploads.size());
        compareUploads(uploads.get(0), "x", content1.getBytes(CharsetUtil.UTF_8));
        compareUploads(uploads.get(1), "y", content2.getBytes(CharsetUtil.UTF_8));
    }


    @Test
    public void testFileUploadAuthTokenNotCsrfEnabled() throws Exception {
        URL url = new URL("http://localhost:7070/service/upload?lbfums=");
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        addFormField(bb, "_charset_", "");
        addFormField(bb, "filename1", filename1);
        addFormFile(bb, filename1, "text/plain", content1);

        endForm(bb);

        byte [] form = bb.toByteArray();
        HashMap<String, String> headers = new HashMap<String, String>();
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        XMLElement req = new XMLElement(AccountConstants.AUTH_REQUEST);
        com.zimbra.common.soap.Element a = req.addUniqueElement(AccountConstants.E_ACCOUNT);
        a.addAttribute(AccountConstants.A_BY, "name");
        a.setText(acct.getName());
        req.addUniqueElement(AccountConstants.E_PASSWORD).setText("secret");
        Element response = new Auth().handle(req, ServiceTestUtil.getRequestContext(acct));
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();

        MockHttpServletRequest mockreq = new MockHttpServletRequest(form, url, "multipart/form-data; boundary=" + boundary,
            7070, "test", headers);
        mockreq.setAttribute(CsrfFilter.CSRF_TOKEN_CHECK, Boolean.FALSE);

        Cookie cookie = new Cookie( "ZM_AUTH_TOKEN", authToken);
        mockreq.setCookies(cookie);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        servlet.doPost(mockreq, resp);
        String respStrg = resp.output.toString();
        System.out.println(respStrg);
        assertTrue(respStrg.contains("200"));
    }


    @Test
    public void testFileUploadAuthTokenCsrfEnabled() throws Exception {
        URL url = new URL("http://localhost:7070/service/upload?lbfums=");
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        addFormField(bb, "_charset_", "");
        addFormField(bb, "filename1", filename1);
        addFormFile(bb, filename1, "text/plain", content1);

        endForm(bb);

        byte [] form = bb.toByteArray();
        HashMap<String, String> headers = new HashMap<String, String>();
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

        XMLElement req = new XMLElement(AccountConstants.AUTH_REQUEST);
        req.addAttribute(AccountConstants.A_CSRF_SUPPORT, "1");
        com.zimbra.common.soap.Element a = req.addUniqueElement(AccountConstants.E_ACCOUNT);
        a.addAttribute(AccountConstants.A_BY, "name");
        a.setText(acct.getName());
        req.addUniqueElement(AccountConstants.E_PASSWORD).setText("secret");
        Map<String, Object>context = ServiceTestUtil.getRequestContext(acct);
        MockHttpServletRequest authReq = (MockHttpServletRequest)context.get(SoapServlet.SERVLET_REQUEST);
        authReq.setAttribute(Provisioning.A_zimbraCsrfTokenCheckEnabled, Boolean.TRUE);
        Random nonceGen = new Random();
        authReq.setAttribute(CsrfFilter.CSRF_SALT,nonceGen.nextInt() + 1);
        Element response = new Auth().handle(req, context);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        String csrfToken = response.getElement("csrfToken").getText();
        headers.put(Constants.CSRF_TOKEN, csrfToken);

        MockHttpServletRequest mockreq = new MockHttpServletRequest(form, url, "multipart/form-data; boundary=" + boundary,
            7070, "test", headers);
        mockreq.setAttribute(CsrfFilter.CSRF_TOKEN_CHECK, Boolean.TRUE);

        Cookie cookie = new Cookie( "ZM_AUTH_TOKEN", authToken);
        mockreq.setCookies(cookie);

        MockHttpServletResponse resp = new MockHttpServletResponse();

        servlet.doPost(mockreq, resp);
        String respStrg = resp.output.toString();
        System.out.println(respStrg);
        assertTrue(respStrg.contains("200"));
    }

    @Test
    public void testFileUploadAuthTokenCsrfEnabledButNoCsrfToken() throws Exception {
        URL url = new URL("http://localhost:7070/service/upload?lbfums=");
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        addFormField(bb, "_charset_", "");
        addFormField(bb, "filename1", filename1);
        addFormFile(bb, filename1, "text/plain", content1);

        endForm(bb);

        byte [] form = bb.toByteArray();
        HashMap<String, String> headers = new HashMap<String, String>();
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        XMLElement req = new XMLElement(AccountConstants.AUTH_REQUEST);
        req.addAttribute(AccountConstants.A_CSRF_SUPPORT, "1");
        com.zimbra.common.soap.Element a = req.addUniqueElement(AccountConstants.E_ACCOUNT);
        a.addAttribute(AccountConstants.A_BY, "name");
        a.setText(acct.getName());
        req.addUniqueElement(AccountConstants.E_PASSWORD).setText("secret");
        Map<String, Object>context = ServiceTestUtil.getRequestContext(acct);
        MockHttpServletRequest authReq = (MockHttpServletRequest)context.get(SoapServlet.SERVLET_REQUEST);
        authReq.setAttribute(Provisioning.A_zimbraCsrfTokenCheckEnabled, Boolean.TRUE);
        Random nonceGen = new Random();
        authReq.setAttribute(CsrfFilter.CSRF_SALT,nonceGen.nextInt() + 1);
        Element response = new Auth().handle(req, context);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();

        MockHttpServletRequest mockreq = new MockHttpServletRequest(form, url, "multipart/form-data; boundary=" + boundary,
            7070, "test", headers);
        mockreq.setAttribute(CsrfFilter.CSRF_TOKEN_CHECK, Boolean.TRUE);

        Cookie cookie = new Cookie( "ZM_AUTH_TOKEN", authToken);
        mockreq.setCookies(cookie);

        MockHttpServletResponse resp = new MockHttpServletResponse();

        servlet.doPost(mockreq, resp);
        //<html><head><script language='javascript'>function doit() { window.parent._uploadManager.loaded(401,'null'); }
        //</script></head><body onload='doit()'></body></html>
        String respStrg = resp.output.toString();
        assertTrue(respStrg.contains("401"));
    }

    @Test
    public void testFileUploadAuthTokenCsrfEnabled2() throws Exception {

        HashMap<String, String> headers = new HashMap<String, String>();
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

        XMLElement req = new XMLElement(AccountConstants.AUTH_REQUEST);
        req.addAttribute(AccountConstants.A_CSRF_SUPPORT, "1");
        com.zimbra.common.soap.Element a = req.addUniqueElement(AccountConstants.E_ACCOUNT);
        a.addAttribute(AccountConstants.A_BY, "name");
        a.setText(acct.getName());
        req.addUniqueElement(AccountConstants.E_PASSWORD).setText("secret");
        Map<String, Object>context = ServiceTestUtil.getRequestContext(acct);
        MockHttpServletRequest authReq = (MockHttpServletRequest)context.get(SoapServlet.SERVLET_REQUEST);
        authReq.setAttribute(Provisioning.A_zimbraCsrfTokenCheckEnabled, Boolean.TRUE);
        Random nonceGen = new Random();
        authReq.setAttribute(CsrfFilter.CSRF_SALT,nonceGen.nextInt() + 1);
        Element response = new Auth().handle(req, context);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        String csrfToken = response.getElement("csrfToken").getText();

        URL url = new URL("http://localhost:7070/service/upload?lbfums=");
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        addFormField(bb, "_charset_", "");
        addFormField(bb, "filename1", filename1);
        addFormField(bb, "csrfToken", csrfToken);
        addFormFile(bb, filename1, "text/plain", content1);

        endForm(bb);

        byte [] form = bb.toByteArray();

        MockHttpServletRequest mockreq = new MockHttpServletRequest(form, url, "multipart/form-data; boundary=" + boundary,
            7070, "test", headers);
        mockreq.setAttribute(CsrfFilter.CSRF_TOKEN_CHECK, Boolean.TRUE);

        Cookie cookie = new Cookie( "ZM_AUTH_TOKEN", authToken);
        mockreq.setCookies(cookie);

        MockHttpServletResponse resp = new MockHttpServletResponse();

        servlet.doPost(mockreq, resp);
        String respStrg = resp.output.toString();
        assertTrue(respStrg.contains("200"));
    }
}
