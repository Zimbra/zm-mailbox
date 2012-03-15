/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 VMware, Inc.
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
package com.zimbra.common.zmime;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import javax.mail.Session;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.zmime.ZMimeUtility.ByteBuilder;
import com.zimbra.common.zmime.ZTransferEncoding.Base64EncoderStream;

public class ZMimeMultipartTest {
    @Test
    public void encoded() throws Exception {
        final String boundary = "dfghjkl";
        final String plain = "The Rain in Spain.";
        final String html = "The <u>Rain</u> in <em>Spain</em>.";

        ByteBuilder bbmulti = new ByteBuilder();
        bbmulti.append("--").append(boundary).append("\r\n");
        bbmulti.append("Content-Type: text/plain\r\n");
        bbmulti.append("\r\n");
        bbmulti.append(plain).append("\r\n");
        bbmulti.append("--").append(boundary).append("\r\n");
        bbmulti.append("Content-Type: text/html\r\n");
        bbmulti.append("\r\n");
        bbmulti.append(html).append("\r\n");
        bbmulti.append("--").append(boundary).append("--\r\n");

        ByteBuilder bb = new ByteBuilder();
        bb.append("From: test@example.com\r\n");
        bb.append("To: rcpt@example.com\r\n");
        bb.append("Subject: message subject\r\n");
        bb.append("Message-ID: <11e1-b0c4-0800200c9a66@example.com>\r\n");
        bb.append("Content-Transfer-Encoding: base64\r\n");
        bb.append("Content-Type: multipart/alternative; boundary=").append(boundary).append("\r\n");
        bb.append("\r\n");
        bb.append(ByteUtil.getContent(new Base64EncoderStream(new ByteArrayInputStream(bbmulti.toByteArray())), -1));

        System.setProperty("mail.mime.ignoremultipartencoding", "false");

        ZMimeMessage mm = new ZMimeMessage(Session.getDefaultInstance(new Properties()), new ByteArrayInputStream(bb.toByteArray()));
        Object o = mm.getContent();
        Assert.assertTrue("content is ZMimeMultipart", o instanceof ZMimeMultipart);
        ZMimeMultipart multi = (ZMimeMultipart) o;
        Assert.assertEquals("2 subparts", 2, multi.getCount());
        Assert.assertEquals("part 1 content match", plain, multi.getBodyPart(0).getContent());
        Assert.assertEquals("part 2 content match", html, multi.getBodyPart(1).getContent());
    }
}
