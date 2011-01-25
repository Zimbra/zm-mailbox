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
package com.zimbra.common.mime.shim;

import java.io.IOException;

import javax.mail.MessagingException;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.ByteUtil;

public class JavaMailMimeBodyPartTest {

    @Test
    public void setStringContent() throws MessagingException, IOException {
        String content = "these are the times\nthat try men's souls";
        JavaMailMimeBodyPart mbp = new JavaMailMimeBodyPart();

        mbp.setContent(content, "text/plain");
        Assert.assertArrayEquals(content.getBytes(), ByteUtil.getContent(mbp.getContentStream(), -1));

        mbp.setText(content);
        Assert.assertArrayEquals(content.getBytes(), ByteUtil.getContent(mbp.getContentStream(), -1));

        mbp.setContent(content, "text/plain; charset=utf-8");
        Assert.assertArrayEquals(content.getBytes("utf-8"), ByteUtil.getContent(mbp.getContentStream(), -1));

        mbp.setText(content, "utf-8");
        Assert.assertArrayEquals(content.getBytes("utf-8"), ByteUtil.getContent(mbp.getContentStream(), -1));

        mbp.setContent(content, "xml/x-zimbra-share");
        Assert.assertArrayEquals(content.getBytes(), ByteUtil.getContent(mbp.getContentStream(), -1));
    }
}
