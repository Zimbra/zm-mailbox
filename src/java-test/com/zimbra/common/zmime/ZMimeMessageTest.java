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

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

public class ZMimeMessageTest {
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
    public void addHeaderLine() throws Exception {
        MimeMessage mm = new MimeMessageWithId("<sample-823745-asd-23432452345@example.com>");
        for (String line : HEADERS) {
            mm.addHeaderLine(line + "\r\n");
        }
        mm.setContent("", mm.getContentType());
        mm.writeTo(System.out);
    }
}
