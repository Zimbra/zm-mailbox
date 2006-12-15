/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mime;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


/**
 * <code>MimeVisitor</code> implementation used for printing
 * the structure of a <code>MimePart</code>.  Used for debugging.
 * 
 * @author bburtin
 */
public class MimeFormatter
extends MimeVisitor {

    private int mIndentLevel = 0;
    private StringBuilder mBuf = new StringBuilder(); 
    
    public boolean visitMessage(MimeMessage msg, VisitPhase visitPhase)
    throws MessagingException {
        if (visitPhase == VisitPhase.VISIT_BEGIN) {
            indent();
            mBuf.append("Message: " + msg.getSubject() + "\n");
            mIndentLevel++;
        } else {
            mIndentLevel--;
        }
        return false;
    }

    public boolean visitMultipart(MimeMultipart mp, VisitPhase visitPhase) {
        if (visitPhase == VisitPhase.VISIT_BEGIN) {
            indent();
            mBuf.append("Multipart:\n");
            mIndentLevel++;
        } else {
            mIndentLevel--;
        }
        return false;
    }

    public boolean visitBodyPart(MimeBodyPart bp) throws MessagingException {
        indent();
        mBuf.append("Part: type=" + bp.getContentType() + ", filename=" + bp.getFileName() + "\n");
        if (bp.getContentType().startsWith("text")) {
            mBuf.append("----------\n");
            try {
                mBuf.append(bp.getContent());
            } catch (IOException e) {
                mBuf.append(e + "\n");
            }
            mBuf.append("----------\n");
        }
        return false;
    }

    private void indent() {
        for (int i = 0; i < mIndentLevel; i++) {
            mBuf.append("  ");
        }
    }
    
    public String toString() {
        return mBuf.toString();
    }
}
