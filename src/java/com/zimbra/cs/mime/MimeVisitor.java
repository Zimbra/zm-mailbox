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

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * A class that implements this interface can be passed to {@link Mime#accept}
 * to walk a MIME node tree.
 *   
 * @author bburtin
 */
public interface MimeVisitor {
    /**
     * This flag is passed to the <code>visitXXX</code> methods
     * before a node's children are visited.
     */
    public static int VISIT_BEGIN = 1;
    /**
     * This flag is passed to the <code>visitXXX</code> methods
     * after a node's children have been visited.
     */
    public static int VISIT_END = 2;
    
    /**
     * @see #VISIT_BEGIN
     * @see #VISIT_END
     */
    public void visitMessage(MimeMessage msg, int visitKind)
    throws MessagingException, IOException;
    
    /**
     * @see #VISIT_BEGIN
     * @see #VISIT_END
     */
    public void visitMultipart(MimeMultipart mp, int visitKind)
    throws MessagingException, IOException;
    
    public void visitBodyPart(MimeBodyPart bp)
    throws MessagingException, IOException;
}
