/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
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

import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.SharedByteArrayInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.zmime.ZMimeUtility.ByteBuilder;

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
}
