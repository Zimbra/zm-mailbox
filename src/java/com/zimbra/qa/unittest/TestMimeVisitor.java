/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2010 Zimbra, Inc.
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

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.cs.mime.MimeVisitor;

/**
 * Replaces <tt>"oldsubject"</tt> with <tt>"newsubject"</tt> in a <tt>MimeMessage</tt>'s
 * subject.
 */
public class TestMimeVisitor
extends MimeVisitor {
    
    public TestMimeVisitor() {
    }
    
    @Override
    protected boolean visitBodyPart(MimeBodyPart bp) {
        return false;
    }

    @Override
    protected boolean visitMessage(MimeMessage mm, VisitPhase visitKind) throws MessagingException {
        if (visitKind != VisitPhase.VISIT_BEGIN) {
            return false;
        }
        String subject = mm.getSubject();
        if (subject.contains("oldsubject")) {
            if (mCallback != null && mCallback.onModification() == false) {
                return false;
            }
            mm.setSubject(subject.replaceAll("oldsubject", "newsubject"));
            mm.saveChanges();
            return true;
        }
        return false;
    }

    @Override
    protected boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) {
        return false;
    }
}