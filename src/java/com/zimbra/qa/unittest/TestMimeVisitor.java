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