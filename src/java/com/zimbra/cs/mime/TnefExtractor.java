/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

//import com.zimbra.cs.util.JMSession;

/**
 * Provides access to all TNEF attachments in the MIME structure that
 * accepts this visitor.
 *  
 * @author bburtin
 */
public class TnefExtractor implements MimeVisitor {

    private List /* MimeBodyPart */ mTnefBodyParts = new ArrayList();
    private MimeMessage[] mTnefMessages;

    /**
     * Returns all TNEF attachments, converted into MimeMessages, or an
     * empty array if no TNEF attachments are found.
     */
    public MimeMessage[] getTnefsAsMime()
    throws IOException, MessagingException {
        if (mTnefMessages == null) {
            initialize();
        }
        return mTnefMessages;
    }
    
    private void initialize()
    throws IOException, MessagingException {
        // xxx Perform conversion here
        mTnefMessages = new MimeMessage[0];
    }
    
    ////////// MimeVisitor implementation //////////
    
    public void visitBodyPart(MimeBodyPart bp) throws MessagingException {
        // xxx If the content type of this body part is TNEF, add it to mTnefBodyParts 
    }

    public void visitMessage(MimeMessage msg, int visitKind) {
    }

    public void visitMultipart(MimeMultipart mp, int visitKind) {
    }
}
