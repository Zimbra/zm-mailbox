/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mime.Mime;

public class MessageBuilder {

    private String mSubject;
    private String mSender;
    private String mRecipient;
    private String mBody;
    private Date mDate;
    private String mContentType;
    
    static String DEFAULT_MESSAGE_BODY =
        "Dude,\r\n\r\nAll I need are some tasty waves, a cool buzz, and I'm fine.\r\n\r\nJeff";

    static String[] MESSAGE_TEMPLATE_LINES = {
        "From: Jeff Spiccoli <${SENDER}>",
        "To: Test User 1 <${RECIPIENT}>",
        "Subject: ${SUBJECT}",
        "Date: ${DATE}",
        "Content-Type: ${CONTENT_TYPE}",
        "",
        "${MESSAGE_BODY}"
    };

    static String MESSAGE_TEMPLATE = StringUtil.join("\r\n", MESSAGE_TEMPLATE_LINES);
    
    public MessageBuilder withSubject(String subject) {
        mSubject = subject;
        return this;
    }
    
    public MessageBuilder withSender(String sender) {
        mSender = sender;
        return this;
    }
    
    public MessageBuilder withRecipient(String recipient) {
        mRecipient = recipient;
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
    
    public String create()
    throws ServiceException {
        if (mRecipient == null) {
            mRecipient = "user1";
        }
        if (mSender == null) {
            mSender = "jspiccoli";
        }
        if (mDate == null) {
            mDate = new Date();
        }
        if (mContentType == null) {
            mContentType = Mime.CT_TEXT_PLAIN;
        }
        if (mBody == null) {
            mBody = MessageBuilder.DEFAULT_MESSAGE_BODY;
        }
        mSender = TestUtil.addDomainIfNecessary(mSender);
        mRecipient = TestUtil.addDomainIfNecessary(mRecipient);
        
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("SUBJECT", mSubject);
        vars.put("SENDER", mSender);
        vars.put("RECIPIENT", mRecipient);
        vars.put("DATE", getDateHeaderValue(mDate));
        vars.put("CONTENT_TYPE", mContentType);
        vars.put("MESSAGE_BODY", mBody);
        
        String rawMessage = StringUtil.fillTemplate(MessageBuilder.MESSAGE_TEMPLATE, vars);
        reset();
        return rawMessage;
    }
    
    private void reset() {
        mSubject = null;
        mSender = null;
        mRecipient = null;
        mContentType = null;
        mDate = null;
        mBody = null;
    }

    private static String getDateHeaderValue(Date date) {
        return String.format("%1$ta, %1$td %1$tb %1$tY %1$tH:%1$tM:%1$tS %1$tz (%1$tZ)", date);
    }

}
