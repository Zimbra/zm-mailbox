/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.cs.mime.Mime;
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
        String subject = Mime.getSubject(mm);
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