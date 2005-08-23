/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.mime;

import java.io.IOException;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.freeutils.tnef.TNEFInputStream;
import net.freeutils.tnef.TNEFUtils;
import net.freeutils.tnef.mime.TNEFMime;

import com.zimbra.cs.util.JMSession;


/**
 * Converts each TNEF MimeBodyPart to a multipart/alternative that contains
 * the original TNEF file and its MIME counterpart.<p>
 * 
 * For example, the following structure:
 * 
 * <ul>
 *   <li>MimeMessage + MimeMultipart (multipart/mixed)</li>
 *   <ul>
 *     <li>MimeBodyPart (text/plain)</li>
 *     <li><b>MimeBodyPart (application/ms-tnef)</b></li>
 *   </ul>
 * </ul>
 * 
 * would be converted to:
 *   
 * <ul>
 *   <li>MimeMessage + MimeMultipart (multipart/mixed)</li>
 *   <ul>
 *     <li>MimeBodyPart (text/plain)</li>
 *     <li><b>MimeMultipart (multipart/alternative)</b></li>
 *     <ul>
 *       <li><b>MimeBodyPart (application/ms-tnef)</b></li>
 *       <li><b>MimeMessage + MimeMultipart (multipart/mixed)</b></li>
 *     </ul>
 *   </ul>
 * </ul>
 * @author bburtin
 */
public class TnefConverter implements MimeVisitor {

    public void visitBodyPart(MimeBodyPart tnefPart) {
    }

    /**
     * Performs the TNEF->MIME conversion on any TNEF body parts that
     * make up the given message. 
     */
    public void visitMessage(MimeMessage msg, int visitKind)
    throws MessagingException, IOException {
        if (visitKind != VISIT_END) {
            return;
        }
        
        if (msg.isMimeType(Mime.CT_MULTIPART_WILD)) {
            Multipart mp = (Multipart) msg.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (TNEFUtils.isTNEFMimeType(bp.getContentType())) {
                    // Convert TNEF to a Message and remove it from the parent
                    TNEFInputStream in = new TNEFInputStream(bp.getInputStream());
                    MimeMessage converted = TNEFMime.convert(JMSession.getSession(), in);
                    mp.removeBodyPart(i);
                    
                    // Create a MimeBodyPart for the converted data.  Currently we're throwing
                    // away the top-level message because its content shows up as blank after
                    // the conversion.
                    MimeMultipart convertedMulti = (MimeMultipart) converted.getContent();
                    MimeBodyPart convertedPart = new MimeBodyPart();
                    convertedPart.setContent(convertedMulti);
                    
                    // Create a multipart/alternative for the TNEF and its MIME version
                    MimeMultipart altMulti = new MimeMultipart("alternative");
                    altMulti.addBodyPart(bp);
                    altMulti.addBodyPart(convertedPart);
                    MimeBodyPart altBodyPart = new MimeBodyPart();
                    altBodyPart.setContent(altMulti);
                    
                    // Put the new multipart/alternative message where the TNEF used to be
                    mp.addBodyPart(altBodyPart, i);
                    msg.setContent(mp);
                    msg.saveChanges();
                }
            }
        }
    }

    public void visitMultipart(MimeMultipart multipart, int visitKind) {
    }
}
