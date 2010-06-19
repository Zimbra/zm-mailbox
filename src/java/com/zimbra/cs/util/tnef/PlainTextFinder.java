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

package com.zimbra.cs.util.tnef;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.util.ByteUtil;

// for finding the first text/plain part of a MimeMessage
public class PlainTextFinder extends MimeVisitor {
    private MimePart mPlainTextPart;

    public PlainTextFinder() {}

    public String getPlainText() throws MessagingException, IOException {
        if (mPlainTextPart == null)
            return null;
        ContentType ct = new ContentType(mPlainTextPart.getContentType());
        String charset = ct.getParameter(MimeConstants.P_CHARSET);
        if (charset == null) charset = MimeConstants.P_CHARSET_DEFAULT;
        byte[] descBytes = ByteUtil.getContent(mPlainTextPart.getInputStream(), mPlainTextPart.getSize());
        return new String(descBytes, charset);
    }

    private static boolean matchingType(Part part, String ct) throws MessagingException {
        String mmCtStr = part.getContentType();
        if (mmCtStr != null) {
            ContentType mmCt = new ContentType(mmCtStr);
            return mmCt.match(ct);
        }
        return false;
    }

    @Override
    protected boolean visitBodyPart(MimeBodyPart bp) throws MessagingException {
        if (mPlainTextPart == null && matchingType(bp, MimeConstants.CT_TEXT_PLAIN))
            mPlainTextPart = bp;
        return false;
    }

    @Override
    protected boolean visitMessage(MimeMessage mm, VisitPhase visitKind) throws MessagingException {
        if (mPlainTextPart == null && matchingType(mm, MimeConstants.CT_TEXT_PLAIN))
            mPlainTextPart = mm;
        return false;
    }

    @Override
    protected boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) throws MessagingException {
        return false;
    }
}

