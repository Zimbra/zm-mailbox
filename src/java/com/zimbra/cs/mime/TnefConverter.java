/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.tnef.DefaultTnefToICalendar;
import com.zimbra.cs.util.tnef.TnefToICalendar;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;

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
    private MimeMessage mMimeMessage;

    protected boolean visitBodyPart(MimeBodyPart bp)  { return false; }

    protected boolean visitMessage(MimeMessage msg, VisitPhase visitKind) throws MessagingException {
        if (visitKind == VisitPhase.VISIT_BEGIN) {
            mMimeMessage = msg;
            return false;
        }

        // do the decode in the exit phase
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
     * @throws ServiceException 
     */

    private MimeMultipart expandTNEF(MimeBodyPart bp) throws MessagingException, IOException {
        if (!TNEFUtils.isTNEFMimeType(bp.getContentType()))
            return null;

        MimeMessage converted = null;
        
        // convert TNEF to a MimeMessage and remove it from the parent
        InputStream is = null;
        try {
            TNEFInputStream tnefis = new TNEFInputStream(is = bp.getInputStream());
            converted = TNEFMime.convert(JMSession.getSession(), tnefis);
        // XXX bburtin: nasty hack.  Don't handle OOME since JTNEF can allocate a huge byte
        // array when the TNEF file is malformed.  See bug 42649.
        // } catch (OutOfMemoryError e) {
        //    Zimbra.halt("Ran out of memory while expanding TNEF attachment", e);
        } catch (Throwable t) {
            ZimbraLog.extensions.warn("Conversion failed.  TNEF attachment will not be expanded.", t);
            return null;
        } finally {
            ByteUtil.closeStream(is);
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

        // If the TNEF object contains calendar data, create an iCalendar version.
        MimeBodyPart icalPart = null;
        if (DebugConfig.enableTnefToICalendarConversion) {
            try {
                TnefToICalendar calConverter = new DefaultTnefToICalendar();
                ZCalendar.DefaultContentHandler icalHandler = new ZCalendar.DefaultContentHandler();
                if (calConverter.convert(mMimeMessage, bp.getInputStream(), icalHandler)) {
                    if (icalHandler.getNumCals() > 0) {
                        List<ZVCalendar> cals = icalHandler.getCals();
                        Writer writer = new StringWriter(1024);
                        ICalTok method = null;
                        for (ZVCalendar cal : cals) {
                            cal.toICalendar(writer);
                            if (method == null)
                                method = cal.getMethod();
                        }
                        writer.close();
                        icalPart = new MimeBodyPart();
                        icalPart.setText(writer.toString());
                        ContentType ct = new ContentType(MimeConstants.CT_TEXT_CALENDAR);
                        ct.setCharset(MimeConstants.P_CHARSET_UTF8);
                        if (method != null)
                            ct.setParameter("method", method.toString());
                        icalPart.setHeader("Content-Type", ct.toString());
                    }
                }
            } catch (ServiceException e) {
                throw new MessagingException("TNEF to iCalendar conversion failure: " + e.getMessage(), e);
            }
        }

        // create a multipart/alternative for the TNEF and its MIME version
        MimeMultipart altMulti = new MimeMultipart("alternative");
        altMulti.addBodyPart(bp);
        altMulti.addBodyPart(convertedPart);
        if (icalPart != null)
            altMulti.addBodyPart(icalPart);

        return altMulti;
    }
}
