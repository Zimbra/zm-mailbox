/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009 Zimbra, Inc.
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

package com.zimbra.cs.mime;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;

import net.freeutils.tnef.RawInputStream;
import net.freeutils.tnef.TNEFInputStream;
import net.freeutils.tnef.TNEFUtils;
import net.freeutils.tnef.mime.TNEFMime;


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
public class TnefConverter extends MimeVisitor {
    protected boolean visitBodyPart(MimeBodyPart bp)  { return false; }

    protected boolean visitMessage(MimeMessage msg, VisitPhase visitKind) throws MessagingException {
        // do the decode in the exit phase
        if (visitKind != VisitPhase.VISIT_END)
            return false;

        MimeMultipart multi = null;
        try {
            // we only care about "application/ms-tnef" content
            if (!TNEFUtils.isTNEFMimeType(msg.getContentType()))
                return false;
    
            Object content = msg.getContent();
            if (!(content instanceof MimeBodyPart))
                return false;
            // try to expand the TNEF into a suitable Multipart
            multi = expandTNEF((MimeBodyPart) content);
            if (multi == null)
                return false;
        } catch (MessagingException e) {
            ZimbraLog.extensions.warn("exception while uudecoding message part; skipping part", e);
            return false;
        } catch (IOException e) {
            ZimbraLog.extensions.warn("exception while uudecoding message part; skipping part", e);
            return false;
        }

        // check to make sure that the caller's OK with altering the message
        if (mCallback != null && !mCallback.onModification())
            return false;
        // and put the new multipart/alternative where the TNEF used to be
        msg.setContent(multi);
        msg.setHeader("Content-Type", multi.getContentType() + "; generated=true");
        return false;
    }

    protected boolean visitMultipart(MimeMultipart mmp, VisitPhase visitKind) throws MessagingException {
        // do the decode in the exit phase
        if (visitKind != VisitPhase.VISIT_END)
            return false;
        // proactively ignore already-converted TNEF attachments
        if (MimeConstants.CT_MULTIPART_ALTERNATIVE.equals(mmp.getContentType()))
            return false;

        Map<Integer, MimeBodyPart> changedParts = null;
        try {
            for (int i = 0; i < mmp.getCount(); i++) {
                BodyPart bp = mmp.getBodyPart(i);
                if (bp instanceof MimeBodyPart && TNEFUtils.isTNEFMimeType(bp.getContentType())) {
                    // try to expand the TNEF into a suitable Multipart
                    MimeMultipart multi = null;
                    try {
                        multi = expandTNEF((MimeBodyPart) bp);
                    } catch (Exception e) {
                        ZimbraLog.extensions.warn("exception while decoding TNEF; skipping part", e);
                        continue;
                    }
                    if (multi == null)
                        continue;

                    // create a BodyPart to contain the new Multipart (JavaMail bookkeeping)
                    MimeBodyPart replacement = new MimeBodyPart();
                    replacement.setContent(multi);
                    // and keep track of it for later
                    if (changedParts == null)
                        changedParts = new HashMap<Integer, MimeBodyPart>();
                    changedParts.put(i, replacement);
                }
            }
        } catch (MessagingException e) {
            ZimbraLog.extensions.warn("exception while traversing multipart; skipping", e);
            return false;
        }

        if (changedParts == null || changedParts.isEmpty())
            return false;
        // check to make sure that the caller's OK with altering the message
        if (mCallback != null && !mCallback.onModification())
            return false;
        // and put the new multipart/alternatives where the TNEF used to be
        for (Map.Entry<Integer, MimeBodyPart> change : changedParts.entrySet()) {
            mmp.removeBodyPart(change.getKey());
            mmp.addBodyPart(change.getValue(), change.getKey());
        }
        return true;
    }

    /**
     * Performs the TNEF->MIME conversion on any TNEF body parts that
     * make up the given message. 
     */

    private MimeMultipart expandTNEF(MimeBodyPart bp) throws MessagingException, IOException {
        if (!TNEFUtils.isTNEFMimeType(bp.getContentType()))
            return null;

        // convert TNEF to a MimeMessage and remove it from the parent
        MimeMessage converted = null;
        File tmpfile = null;
        RawInputStream rawis = null;
        TNEFInputStream tnefis = null;
        try {
            tmpfile = File.createTempFile("expand", ".tnef",
                new File(LC.zimbra_tmp_directory.value()));
            bp.saveFile(tmpfile);
            rawis = new RawInputStream(tmpfile);
            tnefis = new TNEFInputStream(rawis);
            converted = TNEFMime.convert(JMSession.getSession(), tnefis);
        } catch (OutOfMemoryError e) {
            Zimbra.halt("Ran out of memory while expanding TNEF attachment", e);
        } catch (Throwable t) {
            ZimbraLog.extensions.warn("TNEF attachment expansion failed: " +
                t.getLocalizedMessage());
            return null;
        } finally {
            if (tnefis != null)
                tnefis.close();
            if (rawis != null)
                rawis.close();
            if (tmpfile != null)
                tmpfile.delete();
        }

        MimeMultipart convertedMulti = (MimeMultipart) converted.getContent();
        // make sure that all the attachments are marked as attachments
        for (int i = 0; i < convertedMulti.getCount(); i++) {
            BodyPart subpart = convertedMulti.getBodyPart(i);
            if (subpart.getHeader("Content-Disposition") == null)
                subpart.setHeader("Content-Disposition", Part.ATTACHMENT);
        }

        // Create a MimeBodyPart for the converted data.  Currently we're throwing
        // away the top-level message because its content shows up as blank after
        // the conversion.
        MimeBodyPart convertedPart = new MimeBodyPart();
        convertedPart.setContent(convertedMulti);

        // create a multipart/alternative for the TNEF and its MIME version
        MimeMultipart altMulti = new MimeMultipart("alternative");
        altMulti.addBodyPart(bp);
        altMulti.addBodyPart(convertedPart);

        return altMulti;
    }

    public static void main(String[] args) throws MessagingException, IOException {
        MimeMessage mm = new MimeMessage(com.zimbra.cs.util.JMSession.getSession(), new java.io.FileInputStream("c:\\tmp\\tnef"));
        new TnefConverter().accept(mm);
        mm.writeTo(new java.io.FileOutputStream("c:\\tmp\\decoded-tnef"));
    }
}
