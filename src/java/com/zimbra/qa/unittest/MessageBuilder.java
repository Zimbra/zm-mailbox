/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.activation.DataHandler;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.util.JMSession;

public class MessageBuilder {

    private String subject;
    private String from;
    private String sender;
    private String toRecipient;
    private String ccRecipient;
    private String body;
    private Date date;
    private String contentType;
    private Object attachment;
    private String attachmentFilename;
    private String attachmentContentType;
    private boolean addMessageIdHeader = false;

    static String DEFAULT_MESSAGE_BODY = "Dude,\r\n\r\nAll I need are some tasty waves, a cool buzz, and I'm fine.\r\n\r\nJeff";

    /**
     * Used to generate a message with no <tt>Message-ID</tt> header.  This
     * allows us to inject the same message multiple times without being deduped.
     */
    private class MimeMessageWithNoId extends ZMimeMessage {
        MimeMessageWithNoId() {
            super(JMSession.getSession());
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            removeHeader("Message-ID");
        }
    }

    public MessageBuilder withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public MessageBuilder withFrom(String from) {
        this.from = from;
        return this;
    }

    public MessageBuilder withSender(String address) {
        sender = address;
        return this;
    }

    public MessageBuilder withToRecipient(String recipient) {
        toRecipient = recipient;
        return this;
    }

    public MessageBuilder withCcRecipient(String recipient) {
        ccRecipient = recipient;
        return this;
    }

    public MessageBuilder withBody(String body) {
        this.body = body;
        return this;
    }

    public MessageBuilder withDate(Date date) {
        this.date = date;
        return this;
    }

    public MessageBuilder withContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public MessageBuilder withAttachment(Object content, String filename, String contentType) {
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        if (StringUtil.isNullOrEmpty(contentType)) {
            throw new IllegalArgumentException("contentType cannot be null or empty");
        }
        if (StringUtil.isNullOrEmpty(filename)) {
            throw new IllegalArgumentException("filename cannot be null or empty");
        }
        attachment = content;
        attachmentFilename = filename;
        attachmentContentType = contentType;
        return this;
    }

    public MessageBuilder withMessageIdHeader() {
        this.addMessageIdHeader = true;
        return this;
    }

    public String create() throws MessagingException, ServiceException, IOException {
        if (toRecipient == null) {
            toRecipient = "user1";
        }
        if (from == null) {
            from = "jspiccoli";
        }
        if (date == null) {
            date = new Date();
        }
        if (contentType == null) {
            contentType = MimeConstants.CT_TEXT_PLAIN;
        }
        if (body == null) {
            body = MessageBuilder.DEFAULT_MESSAGE_BODY;
        }
        from = TestUtil.addDomainIfNecessary(from);
        toRecipient = TestUtil.addDomainIfNecessary(toRecipient);
        sender = TestUtil.addDomainIfNecessary(sender);

        MimeMessage msg =
                addMessageIdHeader ? new ZMimeMessage(JMSession.getSession()) : new MimeMessageWithNoId();
        msg.setRecipient(RecipientType.TO, new JavaMailInternetAddress(toRecipient));
        if (ccRecipient != null) {
            ccRecipient = TestUtil.addDomainIfNecessary(ccRecipient);
            msg.setRecipient(RecipientType.CC, new JavaMailInternetAddress(ccRecipient));
        }
        msg.setFrom(new JavaMailInternetAddress(from));
        if (sender != null) {
            msg.setSender(new JavaMailInternetAddress(sender));
        }
        msg.setSentDate(date);
        msg.setSubject(subject);

        if (attachment == null) {
            // Need to specify the data handler explicitly because JavaMail
            // doesn't know what to do with text/enriched.
            msg.setDataHandler(new DataHandler(new ByteArrayDataSource(body.getBytes(), contentType)));
        } else {
            MimeMultipart multi = new ZMimeMultipart("mixed");
            MimeBodyPart body = new ZMimeBodyPart();
            body.setDataHandler(new DataHandler(new ByteArrayDataSource(this.body.getBytes(), contentType)));
            multi.addBodyPart(body);

            MimeBodyPart attachment = new ZMimeBodyPart();
            attachment.setContent(this.attachment, attachmentContentType);
            attachment.setHeader("Content-Disposition", "attachment; filename=" + attachmentFilename);
            multi.addBodyPart(attachment);

            msg.setContent(multi);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out);
        return new String(out.toByteArray());
    }

    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();
        System.out.println(new MessageBuilder().withSubject("attachment test").withAttachment("attachment", "test.txt", "text/plain").create());
    }
}
