/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.zmime;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.zmime.ZMimeUtility.ByteBuilder;
import org.junit.Assert;
import org.junit.Test;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.SharedByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public class ZMimeParserTest {
    private static String BOUNDARY1 = "-=_sample1";
    private static String BOUNDARY2 = "-=_sample2";

    private ByteBuilder appendMultipartWithoutBoundary(ByteBuilder bb) {
        bb.append("Content-Type: multipart/mixed\r\n");
        bb.append("\r\n");
        bb.append("prologue text goes here\r\n");
        bb.append("--").append(BOUNDARY1).append("\r\n");
        bb.append("Content-Type: text/plain\r\n");
        bb.append("\r\n");
        bb.append("foo!  bar!  loud noises\r\n\r\n");
        bb.append("--").append(BOUNDARY1).append("\r\n");
        bb.append("Content-Type: application/x-unknown\r\n");
        bb.append("Content-Disposition: attachment; filename=x.txt\r\n");
        bb.append("\r\n");
        bb.append("CONTENTS OF ATTACHMENT\r\n\r\n");
        bb.append("--").append(BOUNDARY1).append("--\r\n\r\n");
        return bb;
    }

    private void testMultipartWithoutBoundary(ZMimeMultipart mmp) throws Exception {
        Assert.assertEquals("multipart subtype: mixed", "mixed", new ZContentType(mmp.getContentType()).getSubType());
        Assert.assertEquals("multipart has 2 subparts", 2, mmp.getCount());
        Assert.assertEquals("implicit boundary detection", BOUNDARY1, mmp.getBoundary());
        Assert.assertEquals("first part is text/plain", "text/plain", new ZContentType(mmp.getBodyPart(0).getContentType()).getBaseType());
        Assert.assertEquals("second part is application/x-unknown", "application/x-unknown", new ZContentType(mmp.getBodyPart(1).getContentType()).getBaseType());
    }

    private Session getSession() {
        return Session.getInstance(new Properties());
    }

    @Test
    public void detectBoundary() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("From: <foo@example.com\r\n");
        bb.append("Subject: sample\r\n");
        appendMultipartWithoutBoundary(bb);

        MimeMessage mm = new ZMimeMessage(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
        Assert.assertTrue("content is multipart", mm.getContent() instanceof ZMimeMultipart);
        testMultipartWithoutBoundary((ZMimeMultipart) mm.getContent());

        bb.reset();
        bb.append("From: <foo@example.com\r\n");
        bb.append("Subject: sample\r\n");
        bb.append("Content-Type: multipart/alternative\r\n");
        bb.append("\r\n");
        bb.append("prologue text goes here\r\n");
        bb.append("--").append(BOUNDARY2).append("\r\n");
        appendMultipartWithoutBoundary(bb);
        bb.append("--").append(BOUNDARY2).append("--\r\n");

        mm = new ZMimeMessage(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
        Assert.assertTrue("content is multipart", mm.getContent() instanceof ZMimeMultipart);
        ZMimeMultipart mmp = (ZMimeMultipart) mm.getContent();
        Assert.assertEquals("multipart/alternative", "alternative", new ZContentType(mmp.getContentType()).getSubType());
        Assert.assertEquals("toplevel multipart has 1 subpart", 1, mmp.getCount());
        Assert.assertEquals("implicit boundary detection", BOUNDARY2, mmp.getBoundary());
        Assert.assertEquals("first part is multipart/mixed", "multipart/mixed", new ZContentType(mmp.getBodyPart(0).getContentType()).getBaseType());
        testMultipartWithoutBoundary((ZMimeMultipart) mmp.getBodyPart(0).getContent());
    }

    @Test
    public void multipleContentTypes() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("Content-Type: text/plain\r\n");
        bb.append("From: <foo@example.com\r\n");
        bb.append("Subject: sample\r\n");
        bb.append("Content-Type: multipart/alternative; boundary=").append(BOUNDARY1).append("\r\n");
        bb.append("\r\n");
        bb.append("--").append(BOUNDARY1).append("\r\n");
        bb.append("Content-Type: text/plain\r\n");
        bb.append("\r\n");
        bb.append("foo!  bar!  loud noises\r\n\r\n");
        bb.append("--").append(BOUNDARY1).append("--\r\n");

        try {
            MimeMessage mm = new ZMimeMessage(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
            Assert.assertFalse("content isn't multipart", mm.getContent() instanceof MimeMultipart);
            Assert.assertEquals("text/plain", "text/plain", new ZContentType(mm.getContentType()).getBaseType());
        } catch (ClassCastException e) {
            Assert.fail("mishandled double Content-Type headers");
        }
    }

    @Test
    public void parse() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("Content-Type: text/plain\r\n");
        bb.append("From: <foo@example.com\r\n");
        bb.append("Subject: sample\r\n");
        bb.append("Content-Type: multipart/alternative; boundary=").append(BOUNDARY1).append("\r\n");
        bb.append("\r\n");
        bb.append("--").append(BOUNDARY1).append("\r\n");
        bb.append("Content-Type: text/plain\r\n");
        bb.append("\r\n");
        bb.append("foo!  bar!  loud noises\r\n\r\n");
        bb.append("--").append(BOUNDARY1).append("--\r\n");

        try {
            MimeMessage mm = ZMimeParser.parse(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
            Assert.assertFalse("content isn't multipart", mm.getContent() instanceof MimeMultipart);
            Assert.assertEquals("text/plain", "text/plain", new ZContentType(mm.getContentType()).getBaseType());
        } catch (ClassCastException e) {
            Assert.fail("mishandled double Content-Type headers");
        }
    }

    @Test
    public void repetition() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        String boundary = BOUNDARY1;
        bb.append("From: <foo@example.com\r\n");
        bb.append("Subject: sample\r\n");
        bb.append("Content-Type: multipart/mixed; boundary=").append(boundary).append("\r\n");
        for (int i = 0; i < 100; i++) {
            bb.append("--").append(boundary).append("\r\n");
            bb.append("Content-Type: text/plain\r\n");
            bb.append("\r\n");
            bb.append("foo!  bar!  loud noises\r\n\r\n");
        }
        bb.append("--").append(boundary).append("--\r\n");
        try {
            MimeMessage mm = ZMimeParser.parse(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
            Object content = mm.getContent();
            Assert.assertTrue("content is multipart", content instanceof MimeMultipart);
            MimeMultipart mp = (MimeMultipart) content;
            Assert.assertEquals("count reduced??", 100, mp.getCount());
        } catch (ClassCastException e) {
            Assert.fail("mishandled double Content-Type headers");
        }
    }

    private void addChildren(ByteBuilder bb, int depth) {
        //recursively add children to create deep MIME tree
        if (depth == 100) {
            return;
        }
        String boundary = "-=_level" + depth;
        bb.append("Content-Type: multipart/mixed; boundary=").append(boundary).append("\r\n");
        bb.append("--").append(boundary).append("\r\n");
        bb.append("Content-Type: text/plain\r\n");
        bb.append("\r\n");
        bb.append("foo!  bar!  loud noises\r\n\r\n");
        bb.append("--").append(boundary).append("\r\n");
        addChildren(bb, depth + 1);
        bb.append("--").append(boundary).append("--\r\n");
    }

    private void traverseChildren(ZMimeMultipart mp, int targetDepth) throws MessagingException, IOException {
        //traverse MIME tree and make sure the expected bottom item has no children
        if (targetDepth == 0) {
            Assert.assertTrue("depth at 0", mp.getCount() == 0);
            return;
        }
        BodyPart bp = mp.getBodyPart(1);
        Assert.assertTrue("not multipart?", bp instanceof ZMimeBodyPart);
        ZMimeBodyPart zbp = (ZMimeBodyPart) bp;
        Object content = zbp.getContent();
        Assert.assertTrue("not multipart?", content instanceof ZMimeMultipart);
        ZMimeMultipart zmp = (ZMimeMultipart) content;
        traverseChildren(zmp, targetDepth - 1);
    }

    @Test
    public void recursion() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        String boundary = BOUNDARY1;
        bb.append("From: <foo@example.com\r\n");
        bb.append("Subject: sample\r\n");
        bb.append("Content-Type: multipart/mixed; boundary=").append(boundary).append("\r\n");
        bb.append("Content-Type: text/plain\r\n");
        bb.append("\r\n");
        bb.append("foo!  bar!  loud noises\r\n\r\n");
        bb.append("--").append(boundary).append("\r\n");
        addChildren(bb, 0);
        bb.append("--").append(boundary).append("--\r\n");
        try {
            MimeMessage mm = ZMimeParser.parse(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
            Object content = mm.getContent();
            Assert.assertTrue("content is multipart", content instanceof ZMimeMultipart);
            ZMimeMultipart zmp = (ZMimeMultipart) content;
            Assert.assertEquals("top count", 1, zmp.getCount());
            traverseChildren((ZMimeMultipart) zmp.getBodyPart(0).getContent(), LC.mime_max_recursion.intValue() - 1);
        } catch (ClassCastException e) {
            Assert.fail("mishandled double Content-Type headers");
        }
    }

    //    private static void checkFile(java.io.File file) throws Exception {
//        String name = file.getName();
//        Properties props = new Properties();
//
//        props.put("mail.mime.address.strict", "false");
//
//        String charset = null;
//        if (name.startsWith("gbk") || name.startsWith("gb2312")) {
//            charset = "gb2312";
//        } else if (name.startsWith("iso-8859-1")) {
//            charset = "iso-8859-1";
//        } else if (name.startsWith("iso-8859-2")) {
//            charset = "iso-8859-2";
//        } else if (name.startsWith("iso-2022-jp")) {
//            charset = "iso-2022-jp";
//        } else if (name.startsWith("shift_jis")) {
//            charset = "shift_jis";
//        } else if (name.startsWith("big5")) {
//            charset = "big5";
//        }
//        if (charset != null) {
//            props.put("mail.mime.charset", charset);
//            props.put(com.zimbra.common.mime.MimePart.PROP_CHARSET_DEFAULT, charset);
//        }
//
//        Session s = Session.getInstance(props);
//        MimeMessage zmm = new ZMimeMessage(s, new java.io.FileInputStream(file));
//        MimeMessage jmmm = new com.zimbra.common.mime.shim.JavaMailMimeMessage(s, new javax.mail.util.SharedFileInputStream(file));
//        MimeMessage mm = new MimeMessage(s, new java.io.FileInputStream(file));
//
//        System.out.println("checking file: " + file.getName() + " [zmm/mm]");
//        com.zimbra.common.mime.shim.JavaMailMimeTester.compareStructure(zmm, mm);
//        System.out.println("checking file: " + file.getName() + " [jmmm/zmm]");
//        com.zimbra.common.mime.shim.JavaMailMimeTester.compareStructure(jmmm, zmm);
//    }
//
//    @Test
//    public void simple() throws Exception {
//        System.setProperty("mail.mime.decodetext.strict",   "false");
//        System.setProperty("mail.mime.encodefilename",      "true");
//        System.setProperty("mail.mime.charset",             "utf-8");
//        System.setProperty("mail.mime.base64.ignoreerrors", "true");
//
//        checkFile(new java.io.File("/Users/dkarp/Documents/messages/undisplayed-generated"));
//
//        for (java.io.File file : new java.io.File("/Users/dkarp/Documents/messages").listFiles()) {
//            if (file.isFile()) {
//                checkFile(file);
//            }
//        }
//    }
    // =========================================================================
    // ZBUG-5493 AUTOMATED UNIT TESTS (Directly from Sudha's QA Document)
    // =========================================================================

    /**
     * QA TC1 (P1): Verify LMTP delivery of email with line
     * starting with =0D and missing Content-Transfer-Encoding header
     */
    @Test
    public void testZBUG5493_TC1_NoCTEHeader() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("From: sender@example.com\r\n");
        bb.append("To: recipient@example.com\r\n");
        bb.append("Subject: Test ZBUG-5493 - No CTE Header\r\n");
        bb.append("Content-Type: text/plain; charset=utf-8\r\n\r\n");
        bb.append("=0D This line starts with equals 0D without CTE header.\r\n");

        MimeMessage mm = ZMimeParser.parse(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
        Assert.assertNotNull("Parsed message should not be null", mm);
        Assert.assertEquals("Subject should match QA Test Data", "Test ZBUG-5493 - No CTE Header", mm.getSubject());
    }

    /**
     * QA TC2 (P1): Verify LMTP delivery when
     * message parts list is empty (currentPart() is null)
     */
    @Test
    public void testZBUG5493_TC2_EmptyMultipart() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("From: sender@example.com\r\n");
        bb.append("To: recipient@example.com\r\n");
        bb.append("Subject: Test ZBUG-5493 - Empty Multipart\r\n");
        bb.append("Content-Type: multipart/mixed; boundary=\"boundary_test\"\r\n\r\n");
        bb.append("--boundary_test--\r\n");

        MimeMessage mm = ZMimeParser.parse(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
        Assert.assertNotNull("Parsed message should not be null", mm);
        Assert.assertEquals("Subject should match QA Test Data", "Test ZBUG-5493 - Empty Multipart", mm.getSubject());
    }

    /**
     * QA TC3 (P1): Verify LMTP delivery with valid Content-Transfer-Encoding:
     * quoted-printable and line starting with =0D
     */
    @Test
    public void testZBUG5493_TC3_QuotedPrintable() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("From: sender@example.com\r\n");
        bb.append("To: recipient@example.com\r\n");
        bb.append("Subject: Test ZBUG-5493 - Quoted Printable\r\n");
        bb.append("Content-Type: text/plain; charset=utf-8\r\n");
        bb.append("Content-Transfer-Encoding: quoted-printable\r\n\r\n");
        bb.append("=0D This line starts with =3D0D in quoted-printable encoding.\r\n");

        MimeMessage mm = ZMimeParser.parse(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
        Assert.assertNotNull("Parsed message should not be null", mm);
        Assert.assertEquals("Subject should match QA Test Data", "Test ZBUG-5493 - Quoted Printable", mm.getSubject());
    }

    /**
     * QA TC4 (P1): Verify LMTP delivery with
     * other Content-Transfer-Encoding values (7bit)
     */
    @Test
    public void testZBUG5493_TC4_7bitEncoding() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("From: sender@example.com\r\n");
        bb.append("To: recipient@example.com\r\n");
        bb.append("Subject: Test ZBUG-5493 - 7bit Encoding\r\n");
        bb.append("Content-Type: text/plain; charset=utf-8\r\n");
        bb.append("Content-Transfer-Encoding: 7bit\r\n\r\n");
        bb.append("=0D Sample line with 7bit encoding.\r\n");

        MimeMessage mm = ZMimeParser.parse(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
        Assert.assertNotNull("Parsed message should not be null", mm);
        Assert.assertEquals("Subject should match QA Test Data", "Test ZBUG-5493 - 7bit Encoding", mm.getSubject());
    }

    /**
     * QA TC9 (P3): Verify that Correctly formed Emails with
     * CTE as Base64 continue to deliver correctly
     */
    @Test
    public void testZBUG5493_TC9_Base64EncodedMessage() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("From: sender@example.com\r\n");
        bb.append("To: recipient@zimbra.com\r\n");
        bb.append("Subject: Test Base64 Encoded Message\r\n");
        bb.append("MIME-Version: 1.0\r\n");
        bb.append("Content-Type: text/plain; charset=UTF-8\r\n");
        bb.append("Content-Transfer-Encoding: base64\r\n\r\n");
        bb.append("SGVsbG8sCgpUaGlzIGlzIGEgdGVzdCBlbWFpbCBtZXNzYWdlIGVuY29kZWQgdXNpbmc\r\n");
        bb.append("QmFzZTY0IENvbnRlbnQtVHJhbnNmZXItRW5jb2RpbmcuCgpUaGlzIGlzIHVzZWZ1bCBm\r\n");
        bb.append("b3IgdHJhbnNtaXR0aW5nIGJpbmFyeSBvciBub24tQVNDSUkgY29udGVudCBvdmVyIFNN\r\n");
        bb.append("VFAuCgpSZWdhcmRzLApTZW5kZXI=\r\n");

        MimeMessage mm = ZMimeParser.parse(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
        Assert.assertNotNull("Parsed message should not be null", mm);
        Assert.assertEquals("Subject should match QA Test Data", "Test Base64 Encoded Message", mm.getSubject());
    }

    /**
     * QA TC10 (P3): Verify that Correctly formed Emails with
     * CTE as 7bit continue to deliver correctly
     */
    @Test
    public void testZBUG5493_TC10_7bitEncodedMessage() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("From: sender@example.com\r\n");
        bb.append("To: recipient@zimbra.com\r\n");
        bb.append("Subject: Test 7bit Encoded Message\r\n");
        bb.append("MIME-Version: 1.0\r\n");
        bb.append("Content-Type: text/plain; charset=US-ASCII\r\n");
        bb.append("Content-Transfer-Encoding: 7bit\r\n\r\n");
        bb.append("Hello,\r\n");
        bb.append("This is a test email message using 7bit Content-Transfer-Encoding.\r\n");
        bb.append("7bit means the content contains only standard ASCII characters (0-127),\r\n");
        bb.append("no special characters, no accented letters, no binary data.\r\n");
        bb.append("Each line must not exceed 998 characters as per RFC 2822.\r\n");
        bb.append("Regards,\r\n");
        bb.append("Sender\r\n");

        MimeMessage mm = ZMimeParser.parse(getSession(), new SharedByteArrayInputStream(bb.toByteArray()));
        Assert.assertNotNull("Parsed message should not be null", mm);
        Assert.assertEquals("Subject should match QA Test Data", "Test 7bit Encoded Message", mm.getSubject());
    }
}
