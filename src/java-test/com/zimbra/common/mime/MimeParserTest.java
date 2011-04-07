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
package com.zimbra.common.mime;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.mime.HeaderUtils.ByteBuilder;
import com.zimbra.common.util.CharsetUtil;

public class MimeParserTest {
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

    private void testMultipartWithoutBoundary(MimeMultipart mmp) {
        Assert.assertEquals("multipart subtype: mixed", "mixed", mmp.getContentType().getSubType());
        Assert.assertEquals("multipart has 2 subparts", 2, mmp.getCount());
        Assert.assertEquals("implicit boundary detection", BOUNDARY1, mmp.getBoundary());
        Assert.assertEquals("first part is text/plain", "text/plain", mmp.getSubpart(0).getContentType().getContentType());
        Assert.assertEquals("second part is application/x-unknown", "application/x-unknown", mmp.getSubpart(1).getContentType().getContentType());
    }

    @Test
    public void detectBoundary() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("From: <foo@example.com\r\n");
        bb.append("Subject: sample\r\n");
        appendMultipartWithoutBoundary(bb);

        MimeMessage mm = new MimeMessage(bb.toByteArray());
        Assert.assertTrue("content is multipart", mm.getBodyPart() instanceof MimeMultipart);
        testMultipartWithoutBoundary((MimeMultipart) mm.getBodyPart());

        bb.reset();
        bb.append("From: <foo@example.com\r\n");
        bb.append("Subject: sample\r\n");
        bb.append("Content-Type: multipart/alternative\r\n");
        bb.append("\r\n");
        bb.append("prologue text goes here\r\n");
        bb.append("--").append(BOUNDARY2).append("\r\n");
        appendMultipartWithoutBoundary(bb);
        bb.append("--").append(BOUNDARY2).append("--\r\n");

        mm = new MimeMessage(bb.toByteArray());
        Assert.assertTrue("content is multipart", mm.getBodyPart() instanceof MimeMultipart);
        MimeMultipart mmp = (MimeMultipart) mm.getBodyPart();
        Assert.assertEquals("multipart/alternative", "alternative", mmp.getContentType().getSubType());
        Assert.assertEquals("toplevel multipart has 1 subpart", 1, mmp.getCount());
        Assert.assertEquals("implicit boundary detection", BOUNDARY2, mmp.getBoundary());
        Assert.assertEquals("first part is multipart/mixed", "multipart/mixed", mmp.getSubpart(0).getContentType().getContentType());
        testMultipartWithoutBoundary((MimeMultipart) mmp.getSubpart(0));
    }

    @Test
    public void multipleContentTypes() throws Exception {
        ByteBuilder bb = new ByteBuilder(CharsetUtil.UTF_8);
        bb.append("Content-Type: multipart/alternative; boundary=").append(BOUNDARY1).append("\r\n");
        bb.append("From: <foo@example.com\r\n");
        bb.append("Subject: sample\r\n");
        bb.append("Content-Type: text/plain\r\n");
        bb.append("\r\n");
        bb.append("--").append(BOUNDARY1).append("\r\n");
        bb.append("Content-Type: text/plain\r\n");
        bb.append("\r\n");
        bb.append("foo!  bar!  loud noises\r\n\r\n");
        bb.append("--").append(BOUNDARY1).append("--\r\n");

        try {
            MimeMessage mm = new MimeMessage(bb.toByteArray());
            Assert.assertTrue("content isn't multipart", mm.getBodyPart() instanceof MimeBodyPart);
            Assert.assertEquals("text/plain", "text/plain", mm.getBodyPart().getContentType().getContentType());
        } catch (ClassCastException e) {
            Assert.fail("mishandled double Content-Type headers");
        }
    }
}
