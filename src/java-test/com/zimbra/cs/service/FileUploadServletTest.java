/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.service;

import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.HeaderUtils.ByteBuilder;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.FileUploadServlet.Upload;

public class FileUploadServletTest {
    private static FileUploadServlet servlet;
    private static Account testAccount;
    private static final String accountId = "11122233-1111-1111-1111-111222333444";

    @BeforeClass
    public static void init() throws Exception {
        MockProvisioning prov = new MockProvisioning();
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, accountId);
        attrs.put(Provisioning.A_zimbraMailHost, "localhost");
        testAccount = prov.createAccount("test@zimbra.com", "secret", attrs);
        Provisioning.setInstance(prov);

        LC.zimbra_tmp_directory.setDefault("build/test");

        servlet = new FileUploadServlet();
    }

    private static final String boundary = "----WebKitFormBoundaryBf0g3B57jaNA7SC6";

    private static final String filename1 = "\u6771\u65e5\u672c\u5927\u9707\u707d.txt";
    private static final String filename2 = "\u6771\u5317\u5730\u65b9\u592a\u5e73\u6d0b\u6c96\u5730\u9707.txt";
    private static final String content1 = "3 \u6708 11 \u65e5\u5348\u5f8c 2 \u6642 46 \u5206\u3054\u308d\u3001\u30de\u30b0\u30cb\u30c1\u30e5\u30fc\u30c9 9.0";
    private static final String content2 = "\u884c\u65b9\u4e0d\u660e\u8005\u76f8\u8ac7\u30c0\u30a4\u30e4\u30eb: \u5ca9\u624b\u770c: 0120-801-471";

    private ByteBuilder addFormField(ByteBuilder bb, String name, String value) {
        bb.append("--").append(boundary).append("\r\n");
        bb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
        bb.append("\r\n");
        bb.append(value == null ? "" : value).append("\r\n");
        return bb;
    }

    private ByteBuilder addFormFile(ByteBuilder bb, String filename, String ctype, String contents) {
        bb.append("--").append(boundary).append("\r\n");
        bb.append("Content-Disposition: form-data; name=\"_attFile_\"; filename=\"").append(filename == null ? "" : filename).append("\"\r\n");
        bb.append("Content-Type: ").append(ctype == null ? "application/octet-stream" : ctype).append("\r\n");
        bb.append("\r\n");
        bb.append(contents == null ? "" : contents).append("\r\n");
        return bb;
    }

    private ByteBuilder endForm(ByteBuilder bb) {
        return bb.append("--").append(boundary).append("--\r\n");
    }

    private List<Upload> uploadForm(byte[] form) throws Exception {
        URL url = new URL("http://localhost:7070/service/upload?fmt=extended");
        MockHttpServletRequest req = new MockHttpServletRequest(form, url, "multipart/form-data; boundary=" + boundary);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        return servlet.handleMultipartUpload(req, resp, "extended", testAccount, false);
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
}
