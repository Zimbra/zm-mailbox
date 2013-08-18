/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.zmime;

import java.util.Enumeration;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.Assert;

import org.junit.Test;

import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.util.MailUtil;

public class ZMimeMessageMailUtilTest {
    private class MimeMessageWithId extends ZMimeMessage {
        private final String mMessageId;

        MimeMessageWithId(String messageId) {
            super(Session.getDefaultInstance(new Properties()));
            mMessageId = messageId;
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            setHeader("Message-ID", mMessageId);
        }
    }

    private static String[] HEADERS = {
        "Date: Mon, 18 Jul 2011 11:30:12 -0700",
        "MIME-Version: 1.0",
        "Subject: re: Your Brains",
        "From: DONOTREPLY@example.com",
        "To: otheruser@example.com",
        "Content-Type: text/plain",
        "X-Face: :/"
    };

    @Test
    public void sendGeneratedMailAddress() throws Exception {
        MimeMessage mm = new MimeMessageWithId("<sample-823745-asd-23432452345@example.com>");
        for (String line : HEADERS) {
            mm.addHeaderLine(line + "\r\n");
        }
        mm.setContent("", mm.getContentType());
        mm.writeTo(System.out);
        
        String from = "donotreply@host.local";
        String expected = "Failed Delivery Notifier <donotreply@host.local>";

        InternetAddress iAddr = new JavaMailInternetAddress(from,  "Failed Delivery Notifier");

        MailUtil.populateFailureDeliveryMessageFields(mm, mm.getSubject(), "Test", null,iAddr);

        Assert.assertEquals(expected, mm.getFrom()[0].toString());
    }
}
