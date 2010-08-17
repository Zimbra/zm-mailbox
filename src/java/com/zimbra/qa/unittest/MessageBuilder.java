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
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.util.ByteArrayDataSource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.util.JMSession;

public class MessageBuilder {

    private String mSubject;
    private String mFrom;
    private String mSender;
    private String mToRecipient;
    private String mCcRecipient;
    private String mBody;
    private Date mDate;
    private String mContentType;
    private Object mAttachment;
    private String mAttachmentFilename;
    private String mAttachmentContentType;
    
    static String DEFAULT_MESSAGE_BODY =
        "Dude,\r\n\r\nAll I need are some tasty waves, a cool buzz, and I'm fine.\r\n\r\nJeff";

    /**
     * Used to generate a message with no <tt>Message-ID</tt> header.  This
     * allows us to inject the same message multiple times without being deduped. 
     */
    private class MimeMessageWithNoId
    extends MimeMessage {
        private MimeMessageWithNoId() throws MessagingException {
            super(JMSession.getSession());
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            removeHeader("Message-ID");
        }
    }
    
    public MessageBuilder withSubject(String subject) {
        mSubject = subject;
        return this;
    }
    
    public MessageBuilder withFrom(String from) {
        mFrom = from;
        return this;
    }
    
    public MessageBuilder withSender(String address) {
        mSender = address;
        return this;
    }
    
    public MessageBuilder withToRecipient(String recipient) {
        mToRecipient = recipient;
        return this;
    }
    
    public MessageBuilder withCcRecipient(String recipient) {
        mCcRecipient = recipient;
        return this;
    }

    public MessageBuilder withBody(String body) {
        mBody = body;
        return this;
    }
    
    public MessageBuilder withDate(Date date) {
        mDate = date;
        return this;
    }
    
    public MessageBuilder withContentType(String contentType) {
        mContentType = contentType;
        return this;
    }
    
    public MessageBuilder withAttachment(Object content, String filename, String contentType) {
        if (content == null ) {
            throw new IllegalArgumentException("content cannot be null");
        }
        if (StringUtil.isNullOrEmpty(contentType)) {
            throw new IllegalArgumentException("contentType cannot be null or empty");
        }
        if (StringUtil.isNullOrEmpty(filename)) {
            throw new IllegalArgumentException("filename cannot be null or empty");
        }
        mAttachment = content;
        mAttachmentFilename = filename;
        mAttachmentContentType = contentType;
        return this;
    }
    
    public String create()
    throws MessagingException, ServiceException, IOException {
        if (mToRecipient == null) {
            mToRecipient = "user1";
        }
        if (mFrom == null) {
            mFrom = "jspiccoli";
        }
        if (mDate == null) {
            mDate = new Date();
        }
        if (mContentType == null) {
            mContentType = MimeConstants.CT_TEXT_PLAIN;
        }
        if (mBody == null) {
            mBody = MessageBuilder.DEFAULT_MESSAGE_BODY;
        }
        mFrom = TestUtil.addDomainIfNecessary(mFrom);
        mToRecipient = TestUtil.addDomainIfNecessary(mToRecipient);
        mSender = TestUtil.addDomainIfNecessary(mSender);
        
        MimeMessage msg = new MimeMessageWithNoId();
        msg.setRecipient(RecipientType.TO, new InternetAddress(mToRecipient));
        if (mCcRecipient != null) {
            mCcRecipient = TestUtil.addDomainIfNecessary(mCcRecipient);
            msg.setRecipient(RecipientType.CC, new InternetAddress(mCcRecipient));
        }
        msg.setFrom(new InternetAddress(mFrom));
        if (mSender != null) {
            msg.setSender(new InternetAddress(mSender));
        }
        msg.setSentDate(mDate);
        msg.setSubject(mSubject);
        
        if (mAttachment == null) {
            // Need to specify the data handler explicitly because JavaMail
            // doesn't know what to do with text/enriched.
            msg.setDataHandler(new DataHandler(new ByteArrayDataSource(mBody.getBytes(), mContentType)));
        } else {
            MimeMultipart multi = new MimeMultipart("mixed");
            MimeBodyPart body = new MimeBodyPart();
            body.setDataHandler(new DataHandler(new ByteArrayDataSource(mBody.getBytes(), mContentType)));
            multi.addBodyPart(body);

            MimeBodyPart attachment = new MimeBodyPart();
            attachment.setContent(mAttachment, mAttachmentContentType);
            attachment.setDisposition("attachment; filename=" + mAttachmentFilename);
            multi.addBodyPart(attachment);
            
            msg.setContent(multi);
        }
        msg.removeHeader("Message-ID");
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out);
        return new String(out.toByteArray());
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        System.out.println(new MessageBuilder().withSubject("attachment test").withAttachment("attachment", "test.txt", "text/plain").create());
    }
}
