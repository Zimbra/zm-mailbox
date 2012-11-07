/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Set;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.zimbra.cs.mime.Mime.FixedMimeMessage;
import com.zimbra.cs.util.JMSession;

/**
 * Unit test for {@link Mime}.
 *
 * @author ysasaki
 */
public class MimeTest {

    private void testCP932(String contentType) throws IOException {
        Reader reader = Mime.getTextReader(getClass().getResourceAsStream("cp932.txt"), contentType, null);
        String result = IOUtils.toString(reader);
        Assert.assertTrue(result
                        .equals("2010/4/2,\u2161\u53f7\u5e97  \u30ab\u30aa\u30b9\u9928,\u3054\u672c\u4eba,1\u56de\u6255\u3044,,'10/05,9960,9960,,,,,\r\n"));
    }

    @Test
    public void getTextReader() throws Exception {
        Reader reader = Mime.getTextReader(getClass().getResourceAsStream("zimbra-shift-jis.txt"), "text/plain", null);
        String result = IOUtils.toString(reader);
        Assert.assertTrue(result
                        .startsWith("Zimbra Collaboration Suite\uff08ZCS\uff09\u306f\u3001Zimbra, Inc. "
                                        + "\u304c\u958b\u767a\u3057\u305f\u30b3\u30e9\u30dc\u30ec\u30fc\u30b7\u30e7\u30f3\u30bd\u30d5\u30c8"
                                        + "\u30a6\u30a7\u30a2\u88fd\u54c1\u3002"));
        Assert.assertTrue(result.endsWith("\u65e5\u672c\u3067\u306f\u4f4f\u53cb\u5546\u4e8b\u304c\u7dcf\u8ca9\u58f2"
                        + "\u4ee3\u7406\u5e97\u3068\u306a\u3063\u3066\u3044\u308b\u3002"));

        // ICU4J thinks it's UTF-32 with confidence 25. We only trust if the
        // confidence is greater than 50.
        reader = Mime.getTextReader(getClass().getResourceAsStream("p4-notification.txt"), "text/plain", null);
        result = IOUtils.toString(reader);
        Assert.assertTrue(result.startsWith("Change 259706"));

        testCP932("text/plain");
        testCP932("text/plain; charset=shift_jis");
        testCP932("text/plain; charset=windows-31j");
        testCP932("text/plain; charset=cp932");
    }

    @Test
    public void parseAddressHeader() throws Exception {
        InternetAddress[] addrs = Mime.parseAddressHeader("\" <test@zimbra.com>");
        Assert.assertEquals(1, addrs.length);
        // Only verify an exception is not thrown. The new parser and the old
        // parser don't get the same result.
    }

    @Test
    public void multipartReport() throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), getClass().getResourceAsStream("NDR.txt"));
        List<MPartInfo> parts = Mime.getParts(mm);
        Assert.assertNotNull(parts);
        Assert.assertEquals(8, parts.size());

        Set<MPartInfo> bodies = Mime.getBody(parts, true);
        Assert.assertEquals(2, bodies.size());
        boolean foundFirstBody = false;
        boolean foundSecondBody = false;
        for (MPartInfo body : bodies) {
            Object content = body.getMimePart().getContent();
            Assert.assertTrue(content instanceof String);
            String text = (String) content;
            if (body.getPartNum() == 1 && text.startsWith("This is an automated message")) {
                foundFirstBody = true;
            } else if (body.getPartNum() == 2 && text.startsWith("<badaddress@gmailllllll.com>")) {
                foundSecondBody = true;
            }
        }

        Assert.assertTrue("first body found", foundFirstBody);
        Assert.assertTrue("second body found", foundSecondBody);

    }

    String boundary = "----------1111971890AC3BB91";

    String baseNdrContent = "From: MAILER-DAEMON@example.com\r\n" + "To: user2@example.com\r\n" + "Subject: Postmaster Copy: Undelivered Mail\r\n"
                    + "Content-Type: multipart/report; report-type=delivery-status;\r\n" + "boundary=\"" + boundary + "\"\r\n";

    @Test
    public void multipartReportRfc822Headers() throws Exception {
        String content = baseNdrContent + boundary + "\r\n" +
            "Content-Description: Notification\r\n" +
            "Content-Type: text/plain;charset=us-ascii\r\n\r\n" +
            "<mta@example.com>: host mta.example.com[255.255.255.0] said: 554 delivery error: This user doesn't have an example.com account (in reply to end of DATA command)\r\n\r\n" +
            boundary + "\r\n" +
            "Content-Description: Delivery report\r\n" +
            "Content-Type: message/delivery-status\r\n\r\n" +
            "X-Postfix-Queue-ID: 12345\r\n" +
            "X-Postfix-Sender: rfc822; test123@example.com\r\n" +
            "Arrival-Date: Wed,  8 Aug 2012 00:05:30 +0900 (JST)\r\n\r\n" +
            boundary + "\r\n" +
            "Content-Description: Undelivered Message Headers\r\n" +
            "Content-Type: text/rfc822-headers\r\n" +
            "Content-Transfer-Encoding: 7bit\r\n\r\n" +
            "From: admin@example\r\n" +
            "To: test15@example.com\r\n" +
            "Message-ID: <123456@client.example.com>\r\n" +
            "Subject: test msg";

        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                        content.getBytes()));

        List<MPartInfo> parts = Mime.getParts(mm);
        Assert.assertNotNull(parts);
        Assert.assertEquals(4, parts.size());
        MPartInfo mpart = parts.get(0);
        Assert.assertEquals("multipart/report", mpart.getContentType());
        List<MPartInfo> children = mpart.getChildren();
        Assert.assertEquals(3, children.size());
        Set<String> types = Sets.newHashSet("text/plain","message/delivery-status", "text/rfc822-headers");
        for (MPartInfo child : children) {
            Assert.assertTrue("Expected: " + child.getContentType(), types.remove(child.getContentType()));
        }

        Set<MPartInfo> bodies = Mime.getBody(parts, false);
        Assert.assertEquals(2, bodies.size());

        types = Sets.newHashSet("text/plain","text/rfc822-headers");
        for (MPartInfo body : bodies) {
            Assert.assertTrue("Expected: " + body.getContentType(), types.remove(body.getContentType()));
        }
    }

    @Test
    public void emptyMultipart() throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), getClass().getResourceAsStream(
                        "bug50275.txt"));
        List<MPartInfo> parts = Mime.getParts(mm);
        Assert.assertNotNull(parts);
        Assert.assertEquals(1, parts.size());
        MPartInfo mpart = parts.get(0);
        Assert.assertEquals("text/plain", mpart.getContentType());
        Assert.assertTrue(((String) mpart.getMimePart().getContent())
                        .indexOf("por favor visite http://www.linux-magazine.es/Readers/Newsletter.") > -1);

    }

    String baseMpMixedContent = "From: user1@example.com\r\n" + "To: user2@example.com\r\n" + "Subject: test\r\n"
                    + "Content-Type: multipart/mixed;\r\n" + "boundary=\"----------1111971890AC3BB91\"\r\n";

    @Test
    public void multiTextBody() throws Exception {

        StringBuilder content = new StringBuilder(baseMpMixedContent);
        int count = 5;
        for (int i = 0; i < count; i++) {
            content.append( "------------1111971890AC3BB91\r\n")
                .append("Content-Type: text/html; charset=windows-1250\r\n")
                .append("Content-Transfer-Encoding: quoted-printable\r\n\r\n")
                .append("<html>Body ").append(i).append("</html>\r\n");
        }
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                        content.toString().getBytes()));

        List<MPartInfo> parts = Mime.getParts(mm);
        Assert.assertNotNull(parts);
        Assert.assertEquals(count + 1, parts.size());
        MPartInfo mpart = parts.get(0);
        Assert.assertEquals("multipart/mixed", mpart.getContentType());
        List<MPartInfo> children = mpart.getChildren();
        Assert.assertEquals(count, children.size());

        Set<MPartInfo> bodies = Mime.getBody(parts, false);
        Assert.assertEquals(count, bodies.size());
        for (MPartInfo body : bodies) {
            Assert.assertEquals("text/html", body.getContentType());
            Object mimeContent = body.getMimePart().getContent();
            Assert.assertTrue(mimeContent instanceof String);
            String string = (String) mimeContent;
            int idx = string.indexOf("" + (body.getPartNum() - 1)) ;
            Assert.assertTrue(idx > 0); //body 0 is part 1, b1 is p2, and so on
        }
    }

    @Test
    public void imgCid() throws Exception {
        String content = baseMpMixedContent
                        + "------------1111971890AC3BB91\r\n"
                        + "Content-Type: text/html; charset=windows-1250\r\n"
                        + "Content-Transfer-Encoding: quoted-printable\r\n\r\n"
                        + "<html>Email with img<img src=\"cid:12345_testemail\"/></html>\r\n"
                        + "------------1111971890AC3BB91\r\n"
                        + "Content-Type: image/jpeg;\r\n"
                        + "name=\"img.jpg\"\r\n"
                        + "Content-Transfer-Encoding: base64\r\n"
                        + "Content-ID: <12345_testemail>\r\n\r\n"
                        + "R0a1231312ad124svsdsal=="; //obviously not a real image
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                        content.getBytes()));

        List<MPartInfo> parts = Mime.getParts(mm);
        Assert.assertNotNull(parts);
        Assert.assertEquals(3, parts.size());
        MPartInfo mpart = parts.get(0);
        Assert.assertEquals("multipart/mixed", mpart.getContentType());
        List<MPartInfo> children = mpart.getChildren();
        Assert.assertEquals(2, children.size());

        Set<MPartInfo> bodies = Mime.getBody(parts, false);
        Assert.assertEquals(1, bodies.size());
        MPartInfo body = bodies.iterator().next();
        Assert.assertEquals("text/html", body.getContentType());
    }

    @Test
    public void textCid() throws Exception {
        String content = baseMpMixedContent
                        + "------------1111971890AC3BB91\r\n"
                        + "Content-Type: text/html; charset=windows-1250\r\n"
                        + "Content-Transfer-Encoding: quoted-printable\r\n"
                        + "Content-ID: <text_testemail>\r\n\r\n" //barely valid, but we shouldn't break if a bad agent sends like this
                        + "<html>Email with img<img src=\"cid:12345_testemail\"/></html>\r\n"
                        + "------------1111971890AC3BB91\r\n"
                        + "Content-Type: image/jpeg;\r\n"
                        + "name=\"img.jpg\"\r\n"
                        + "Content-Transfer-Encoding: base64\r\n"
                        + "Content-ID: <12345_testemail>\r\n\r\n"
                        + "R0a1231312ad124svsdsal=="; //obviously not a real image
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                        content.getBytes()));

        List<MPartInfo> parts = Mime.getParts(mm);
        Assert.assertNotNull(parts);
        Assert.assertEquals(3, parts.size());
        MPartInfo mpart = parts.get(0);
        Assert.assertEquals("multipart/mixed", mpart.getContentType());
        List<MPartInfo> children = mpart.getChildren();
        Assert.assertEquals(2, children.size());

        Set<MPartInfo> bodies = Mime.getBody(parts, false);
        Assert.assertEquals(1, bodies.size());
        MPartInfo body = bodies.iterator().next();
        Assert.assertEquals("text/html", body.getContentType());
    }

    @Test
    public void imgNoCid() throws Exception {
        String content = baseMpMixedContent
                        + "------------1111971890AC3BB91\r\n"
                        + "Content-Type: text/html; charset=windows-1250\r\n"
                        + "Content-Transfer-Encoding: quoted-printable\r\n\r\n"
                        + "<html>Email no img</html>\r\n"
                        + "------------1111971890AC3BB91\r\n"
                        + "Content-Type: image/jpeg;\r\n"
                        + "name=\"img.jpg\"\r\n"
                        + "Content-Transfer-Encoding: base64\r\n\r\n"
                        //no CID here, so sender means for us to show it as body
                        + "R0a1231312ad124svsdsal=="; //obviously not a real image
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                        content.getBytes()));

        List<MPartInfo> parts = Mime.getParts(mm);
        Assert.assertNotNull(parts);
        Assert.assertEquals(3, parts.size());
        MPartInfo mpart = parts.get(0);
        Assert.assertEquals("multipart/mixed", mpart.getContentType());
        List<MPartInfo> children = mpart.getChildren();
        Assert.assertEquals(2, children.size());

        Set<MPartInfo> bodies = Mime.getBody(parts, false);
        Assert.assertEquals(2, bodies.size());
        Set<String> types = Sets.newHashSet("text/html","image/jpeg");
        for (MPartInfo body : bodies) {
            Assert.assertTrue("Expected: " +body.getContentType(), types.remove(body.getContentType()));
        }
    }
}
