/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.Mime.FixedMimeMessage;
import com.zimbra.cs.util.JMSession;

/**
 * Unit test for {@link FixedMimeMessage}.
 *
 * @author ysasaki
 */
public final class FixedMimeMessageTest {


    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Test
    public void messageId() throws Exception {
        String raw = "From: sender@zimbra.com\n" +
            "To: recipient@zimbra.com\n" +
            "Subject: test\n" +
            "\n" +
            "Hello World.";

        MimeMessage message = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(raw.getBytes()));
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

        message = new FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(raw.getBytes()));
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
                new SharedByteArrayInputStream(raw.getBytes(Charsets.UTF_8)));
        Assert.assertNull(message.getEncoding());
        message.setHeader("X-TEST", "test");
        message.saveChanges();
//        Assert.assertNull(message.getEncoding());

        message = new FixedMimeMessage(JMSession.getSession());
        message.setHeader("X-TEST", "test");
        message.setText("\u3042\u3042\u3042\u3044\u3044\u3044\u3046\u3046\u3046\u3048\u3048\u3048\u304a\u304a\u304a",
                "ISO-2022-JP");
        message.saveChanges();
        Assert.assertEquals("7bit", message.getEncoding());

        message = new FixedMimeMessage(JMSession.getSession());
        message.setHeader("X-TEST", "test");
        message.setText("\u3042\u3042\u3042\u3044\u3044\u3044\u3046\u3046\u3046\u3048\u3048\u3048\u304a\u304a\u304a",
                "UTF-8");
        message.saveChanges();
        Assert.assertEquals("8bit", message.getEncoding());
    }

}
