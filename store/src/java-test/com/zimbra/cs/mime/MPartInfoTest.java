/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.mime;

import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.util.JMSession;
import java.util.List;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.util.SharedByteArrayInputStream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MPartInfoTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Test
    public void testFullContentTypeAndOther() throws Exception {
        String content =
                "From: user1@example.com\r\n"
                        + "To: user2@example.com\r\n"
                        + "Subject: FullContentType\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "Content-Transfer-Encoding: base64\r\n\r\n"
                        + "R0a1231312ad124svsdsal==";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        MimePart part = Mime.getMimePart(mm, "1");
        Assert.assertEquals("text/plain", part.getContentType());
        List<MPartInfo> parts = Mime.getParts(mm);
        Assert.assertNotNull(parts);
        Assert.assertEquals(1, parts.size());
        MPartInfo info = parts.get(0);
        Assert.assertEquals("text/plain", info.getContentType());
        Assert.assertEquals("text/plain", info.getFullContentType());
        Assert.assertEquals(24, info.getSize());
        Assert.assertFalse(info.isFilterableAttachment());
        Assert.assertEquals(String.valueOf(1), info.getPartName());
        Assert.assertEquals("", info.getFilename());
    }

    @Test
    public void testGetContentId() throws Exception {
        String content =
                "Subject: FullContentType\r\n"
                        + "Content-Type: text/plain\r\n"
                        + "Content-ID:<12345@test.com>\r\n"
                        + "Hello";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        List<MPartInfo> parts = Mime.getParts(mm);
        MPartInfo info = parts.get(0);
        Assert.assertEquals("<12345@test.com>", info.getContentID());
    }

    @Test
    public void testGetFilename() throws Exception {
        String content =
                "Subject: Testing\r\n"
                        + "Content-Type: text/plain \r\n"
                        + "Content-Disposition:attachment; filename=\"sample.txt\"\r\n";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        List<MPartInfo> parts = Mime.getParts(mm);
        MPartInfo info = parts.get(0);
        Assert.assertEquals("sample.txt", info.getFilename());
    }

    @Test
    public void testHasChildren() throws Exception {
        String content =
                "Subject: Testing\r\n"
                        + "Content-Type: multipart/mixed; boundary=\"ABC\"\r\n" +
                        "--ABC\r\n" +
                        "Content-Type: multipart/mixed\r\n" +
                        "part1\r\n" +
                        "Content-Type:multipart/mixed\r\t" +
                        "part2\r\n" +
                        "--ABC\r\n";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        List<MPartInfo> parts = Mime.getParts(mm);
        MPartInfo parent = parts.get(0);
        Assert.assertTrue(parent.hasChildren());
        Assert.assertNotNull(parent.getChildren());
        Assert.assertEquals(2, parent.getChildren().size());
    }

    @Test
    public void testHasNoChildren() throws Exception {
        String content =
                "Subject: Testing\r\n"
                        + "Content-Type:text/plain ; boundary=\"ABC\"\r\n" +
                        "--ABC\r\n";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        List<MPartInfo> parts = Mime.getParts(mm);
        MPartInfo parent = parts.get(0);
        Assert.assertFalse(parent.hasChildren());
    }

    @Test
    public void testContentTypeParameter() throws Exception {
        String content =
                "Subject: Testing\r\n"
                        + "Content-Type:text/plain ; boundary=\"ABC\"; charset=UTF-8\r\n" +
                        "--ABC\r\n";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        List<MPartInfo> parts = Mime.getParts(mm);
        MPartInfo part = parts.get(0);
        Assert.assertEquals("ABC", part.getContentTypeParameter("boundary"));
        Assert.assertEquals("UTF-8", part.getContentTypeParameter("charset"));
        Assert.assertNull(part.getContentTypeParameter("def"));
    }
}
