/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.io.ByteArrayInputStream;

import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.zimbra.cs.mime.Mime.FixedMimeMessage;
import com.zimbra.cs.util.JMSession;

/**
 * Unit test for {@link FixedMimeMessage}.
 *
 * @author ysasaki
 */
public class FixedMimeMessageTest {

    @Test
    public void messageId() throws Exception {
        String raw = "From: sender@zimbra.com\n" +
            "To: recipient@zimbra.com\n" +
            "Subject: test\n" +
            "\n" +
            "Hello World.";

        MimeMessage message = new FixedMimeMessage(JMSession.getSession(),
                new ByteArrayInputStream(raw.getBytes()));
        Assert.assertNull(message.getMessageID());
        message.setHeader("X-TEST", "test");
        message.saveChanges();
        Assert.assertNotNull(message.getMessageID());

        raw = "From: sender@zimbra.com\n" +
        "To: recipient@zimbra.com\n" +
        "Subject: test\n" +
        "Message-ID: <12345@zimbra.com>" +
        "\n" +
        "Hello World.";

        message = new FixedMimeMessage(JMSession.getSession(),
                new ByteArrayInputStream(raw.getBytes()));
        Assert.assertEquals("<12345@zimbra.com>", message.getMessageID());
        message.setHeader("X-TEST", "test");
        message.saveChanges();
        Assert.assertEquals("<12345@zimbra.com>", message.getMessageID());
    }

    @Test
    public void contentTransferEncoding() throws Exception {
        String raw = "From: sender@zimbra.com\n" +
        "To: recipient@zimbra.com\n" +
        "Subject: test\n" +
        "Content-Type: text/plain; charset=ISO-2022-JP\n" +
        "\n" +
        "\u3042\u3042\u3042\u3044\u3044\u3044\u3046\u3046\u3046\u3048\u3048\u3048\u304a\u304a\u304a";

        MimeMessage message = new FixedMimeMessage(JMSession.getSession(),
                new ByteArrayInputStream(raw.getBytes(Charsets.UTF_8)));
        Assert.assertNull(message.getEncoding());
        message.setHeader("X-TEST", "test");
        message.saveChanges();
        Assert.assertEquals("7bit", message.getEncoding());
    }

}
