/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Message.RecipientType;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarDataSource;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class ForwardCalendarItem extends CalendarRequest {

    // bug 49820: If called from ZDesktop, use the requested/target account as authenticated account because
    // ZD's authenticated account is the fake local@host.local account and we shouldn't use that value to set the
    // Sender MIME header and SENT-BY iCalendar parameter in ORGANIZER property.
    protected static Account getZDesktopSafeAuthenticatedAccount(ZimbraSoapContext zsc) throws ServiceException {
        Account authAcct = getAuthenticatedAccount(zsc);
        if (AccountUtil.isZDesktopLocalAccount(authAcct.getId()))
            return getRequestedAccount(zsc);
        else
            return authAcct;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account senderAcct = getZDesktopSafeAuthenticatedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        // proxy handling

        ItemId iid = new ItemId(request.getAttribute(MailConstants.A_ID), zsc);
        if (!iid.belongsTo(getRequestedAccount(zsc))) {
            // Proxy it.
            return proxyRequest(request, context, iid.getAccountId());
        }

        Element msgElem = request.getElement(MailConstants.E_MSG);
        ParseMimeMessage.MimeMessageData parsedMessageData = new ParseMimeMessage.MimeMessageData();
        MimeMessage mm =
            ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, mbox, msgElem, 
                null, ParseMimeMessage.NO_INV_ALLOWED_PARSER, parsedMessageData);

        MimeMessage[] fwdMsgs = null;
        synchronized(mbox) {
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId());
            if (calItem == null) {
                throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
            }

            RecurId rid = null;
            Element exc = request.getOptionalElement(MailConstants.E_CAL_EXCEPTION_ID);
            if (exc != null) {
                TimeZoneMap tzmap = calItem.getTimeZoneMap();
                Element tzElem = request.getOptionalElement(MailConstants.E_CAL_TZ);
                ICalTimeZone tz = null;
                if (tzElem != null) {
                    tz = CalendarUtils.parseTzElement(tzElem);
                    tzmap.add(tz);
                }
                ParsedDateTime exceptDt = CalendarUtils.parseDateTime(exc, tzmap);
                rid = new RecurId(exceptDt, RecurId.RANGE_NONE);
            }
            if (rid == null) {
                // Forwarding entire appointment
                fwdMsgs = getSeriesFwdMsgs(octxt, senderAcct, calItem, mm);
            } else {
                // Forwarding an instance
                Invite inv = calItem.getInvite(rid);
                MimeMessage mmInv = null;
                if (inv != null) {
                    mmInv = calItem.getSubpartMessage(inv.getMailItemId());
                } else {
                    if (rid != null) {
                        // No invite found matching the RECURRENCE-ID.  It must be a non-exception instance.
                        // Create an invite based on the series invite.
                        Invite seriesInv = calItem.getDefaultInviteOrNull();
                        if (seriesInv == null)
                            throw ServiceException.INVALID_REQUEST("Instance specified but no recurrence series found", null);
                        Invite exceptInv = seriesInv.newCopy();
                        exceptInv.setRecurrence(null);
                        exceptInv.setRecurId(rid);
                        long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
                        exceptInv.setDtStamp(now);
                        ParsedDateTime dtStart = rid.getDt();
                        ParsedDateTime dtEnd = dtStart.add(exceptInv.getEffectiveDuration());
                        exceptInv.setDtStart(dtStart);
                        exceptInv.setDtEnd(dtEnd);
                        inv = exceptInv;

                        // Carry over the MimeMessage/ParsedMessage to preserve any attachments.
                        mmInv = calItem.getSubpartMessage(seriesInv.getMailItemId());
                    } else {
                        // No RECURRENCE-ID given and no invite found.
                        throw ServiceException.INVALID_REQUEST("Invite not found for the requested RECURRENCE-ID", null);
                    }
                }
                ZVCalendar cal = inv.newToICalendar(true);
                MimeMessage mmFwd = getInstanceFwdMsg(senderAcct, inv, cal, mmInv, mm);
                fwdMsgs = new MimeMessage[] { mmFwd };
            }
        }

        for (MimeMessage mmFwd : fwdMsgs) {
            sendFwdMsg(octxt, mbox, mmFwd);
        }

        Element response = getResponseElement(zsc);
        return response;
    }

    protected static ItemId sendFwdMsg(OperationContext octxt, Mailbox mbox, MimeMessage mmFwd)
    throws ServiceException {
        return mbox.getMailSender().sendMimeMessage(octxt, mbox, mmFwd, null, null,
                null, null, null, true, false);
    }

    private static MimeMessage[] getSeriesFwdMsgs(
            OperationContext octxt, Account senderAcct, CalendarItem calItem, MimeMessage mmFwdWrapper)
    throws ServiceException {
        // Get plain and html texts entered by the forwarder.
        DescDetectVisitor visitor = new DescDetectVisitor();
        try {
            visitor.accept(mmFwdWrapper);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Messaging Exception while retrieving description text", e);
        }
        MimeBodyPart plainDescPart = visitor.getPlainDescPart();
        MimeBodyPart htmlDescPart = visitor.getHtmlDescPart();

        try {
            List<MimeMessage> msgs = new ArrayList<MimeMessage>();
            long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
            Invite[] invites = calItem.getInvites();
            // Get canceled instances in the future.  These will be included in the series part.
            List<Invite> cancels = new ArrayList<Invite>();
            for (Invite inv : invites) {
                if (inv.isCancel() && inv.hasRecurId() && inviteIsAfterTime(inv, now))
                    cancels.add(inv);
            }
            // Make sure the series invite is the first one in the list.
            LinkedList<Invite> invOrderedList = new LinkedList<Invite>();
            for (Invite inv : invites) {
                // Ignore exceptions in the past.
                if (inv.hasRecurId() && !inviteIsAfterTime(inv, now))
                    continue;
                if (!inv.isCancel()) {
                    if (inv.isRecurrence())
                        invOrderedList.addFirst(inv);
                    else
                        invOrderedList.addLast(inv);
                }
            }
            boolean didCancels = false;
            boolean firstInv = true;
            for (Invite inv : invOrderedList) {
                // Make the new iCalendar part.
                ZVCalendar cal = inv.newToICalendar(true);
                // For series invite, append the canceled instances.
                if (inv.isRecurrence() && !didCancels) {
                    didCancels = true;
                    for (Invite cancel : cancels) {
                        ZComponent cancelComp = cancel.newToVComponent(true, true);
                        cal.addComponent(cancelComp);
                    }
                }

                MimeMessage mmInv = calItem.getSubpartMessage(inv.getMailItemId());
                MimeMessage mmFwd = makeFwdMsg(senderAcct, inv, mmInv, cal, mmFwdWrapper, plainDescPart, htmlDescPart, firstInv);
                msgs.add(mmFwd);
                firstInv = false;
            }
            return msgs.toArray(new MimeMessage[0]);
        } catch (IOException e) {
            throw ServiceException.FAILURE("error creating forward message", e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("error creating forward message", e);
        }
    }

    private static void setSentByAndAttendees(ZVCalendar cal, String sentBy, Address[] rcpts)
    throws ServiceException {
        // Set SENT-BY to sender's email address in ORGANIZER property of all VEVENT/VTODO components.
        // Required by Outlook.
        // Also, remove existing ATTENDEEs and add ATTENDEE lines for forwardees.
        String sentByAddr = "mailto:" + sentBy;
        for (Iterator<ZComponent> compIter = cal.getComponentIterator(); compIter.hasNext(); ) {
            ZComponent comp = compIter.next();
            ICalTok compName = ICalTok.lookup(comp.getName());
            if (ICalTok.VEVENT.equals(compName) || ICalTok.VTODO.equals(compName)) {
                // Remove existing ATTENDEEs and X-MS-OLK-SENDER.
                for (Iterator<ZProperty> propIter = comp.getPropertyIterator(); propIter.hasNext(); ) {
                    ZProperty prop = propIter.next();
                    if (ICalTok.ATTENDEE.equals(prop.getToken())
                        || "X-MS-OLK-SENDER".equalsIgnoreCase(prop.getName()))
                        propIter.remove();
                }
                // SENT-BY
                ZProperty orgProp = comp.getProperty(ICalTok.ORGANIZER);
                if (orgProp != null) {
                    ZParameter sentByParam = orgProp.getParameter(ICalTok.SENT_BY);
                    if (sentByParam != null) {
                        sentByParam.setValue(sentByAddr);
                    } else {
                        sentByParam = new ZParameter(ICalTok.SENT_BY, sentByAddr);
                        orgProp.addParameter(sentByParam);
                    }
                    // Set X-MS-OLK-SENDER, another Outlook special.
                    ZProperty xMsOlkSender = new ZProperty("X-MS-OLK-SENDER");
                    xMsOlkSender.setValue(sentByAddr);
                    comp.addProperty(xMsOlkSender);
                }
                // ATTENDEEs
                if (rcpts == null)
                    throw ServiceException.INVALID_REQUEST("Missing forwardees", null);
                for (Address r : rcpts) {
                    InternetAddress rcpt = (InternetAddress) r;
                    String email = "mailto:" + rcpt.getAddress();
                    ZProperty att = new ZProperty(ICalTok.ATTENDEE, email);
                    String name = rcpt.getPersonal();
                    if (name != null && name.length() > 0)
                        att.addParameter(new ZParameter(ICalTok.CN, name));
                    att.addParameter(new ZParameter(ICalTok.PARTSTAT, ICalTok.NEEDS_ACTION.toString()));
                    att.addParameter(new ZParameter(ICalTok.RSVP, "TRUE"));
                    comp.addProperty(att);
                }
            }
        }
    }

    private static void setDescProps(ZVCalendar cal, String descPlain, String descHtml) {
        for (Iterator<ZComponent> compIter = cal.getComponentIterator(); compIter.hasNext(); ) {
            ZComponent comp = compIter.next();
            ICalTok compName = ICalTok.lookup(comp.getName());
            if (ICalTok.VEVENT.equals(compName) || ICalTok.VTODO.equals(compName)) {
                // Remove existing DESCRIPTION and X-ALT-DESC properties.
                for (Iterator<ZProperty> propIter = comp.getPropertyIterator(); propIter.hasNext(); ) {
                    ZProperty prop = propIter.next();
                    ICalTok tok = prop.getToken();
                    if (ICalTok.DESCRIPTION.equals(tok) || ICalTok.X_ALT_DESC.equals(tok))
                        propIter.remove();
                }

                if (descPlain != null && descPlain.length() > 0) {
                    // Remove Outlook-style *~*~*~ header block.  Remove separator plus two newlines.
                    int delim = descPlain.indexOf(Invite.HEADER_SEPARATOR);
                    if (delim >= 0) {
                        descPlain = descPlain.substring(delim + Invite.HEADER_SEPARATOR.length());
                        descPlain = descPlain.replaceFirst("^\\r?\\n\\r?\\n", "");
                    }
                    if (descPlain.length() > 0)
                        comp.addProperty(new ZProperty(ICalTok.DESCRIPTION, descPlain));
                }
                if (descHtml != null && descHtml.length() > 0) {
                    ZProperty prop = new ZProperty(ICalTok.X_ALT_DESC, descHtml);
                    prop.addParameter(new ZParameter(ICalTok.FMTTYPE, MimeConstants.CT_TEXT_HTML));
                    comp.addProperty(prop);
                }
                break;  // only update the first component (comps are ordered correctly)
            }
        }
    }

    protected static MimeMessage getInstanceFwdMsg(
            Account senderAcct, Invite inv, ZVCalendar cal, MimeMessage mmInv, MimeMessage mmFwdWrapper)
    throws ServiceException {
        // Get plain and html texts entered by the forwarder.
        DescDetectVisitor visitor = new DescDetectVisitor();
        try {
            visitor.accept(mmFwdWrapper);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Messaging Exception while retrieving description text", e);
        }
        MimeBodyPart plainDescPart = visitor.getPlainDescPart();
        MimeBodyPart htmlDescPart = visitor.getHtmlDescPart();

        try {
            return makeFwdMsg(senderAcct, inv, mmInv, cal, mmFwdWrapper, plainDescPart, htmlDescPart, true);
        } catch (IOException e) {
            throw ServiceException.FAILURE("error creating forward message", e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("error creating forward message", e);
        }
    }

    private static MimeMessage makeFwdMsg(
            Account senderAcct, Invite inv, MimeMessage mmInv, ZVCalendar cal,
            MimeMessage mmFwdWrapper, MimeBodyPart plainDesc, MimeBodyPart htmlDesc,
            boolean useFwdText)
    throws ServiceException, MessagingException, IOException {
        // Set SENT-BY to sender's email address.  Required by Outlook.
        // Also, set ATTENDEEs to the forwardees.  For consistency with Outlook.
        setSentByAndAttendees(cal, AccountUtil.getFriendlyEmailAddress(senderAcct).getAddress(),
                              mmFwdWrapper.getAllRecipients());

        // From: and Sender: headers
        Address from = null;
        Address sender = null;
        sender = AccountUtil.getFriendlyEmailAddress(senderAcct);
        ZOrganizer org = inv.getOrganizer();
        if (org != null) {
            if (org.hasCn())
                from = new InternetAddress(org.getAddress(), org.getCn(), MimeConstants.P_CHARSET_UTF8);
            else
                from = new InternetAddress(org.getAddress());
        } else {
            from = sender;
        }

        MimeMessage mm;
        if (useFwdText) {
            String plainDescStr = null;
            String htmlDescStr = null;
            if (plainDesc != null)
                plainDescStr = (String) plainDesc.getContent();
            if (htmlDesc != null)
                htmlDescStr = (String) htmlDesc.getContent();
            setDescProps(cal, plainDescStr, htmlDescStr);
            mm = createMergedMessage(from, sender, mmInv, inv, cal, plainDesc, htmlDesc);
        } else {
            mm = CalendarMailSender.createCalendarMessage(from, sender, null, mmInv, inv, cal, false);
        }
        // Copy recipient headers from forward wrapper msg.
        RecipientType rcptTypes[] = { RecipientType.TO, RecipientType.CC, RecipientType.BCC };
        for (RecipientType rcptType : rcptTypes) {
            Address[] rcpts = mmFwdWrapper.getRecipients(rcptType);
            mm.setRecipients(rcptType, rcpts);
        }
        mm.setSubject(mmFwdWrapper.getSubject());
        mm.saveChanges();
        return mm;
    }

    // Take mmInv and mutate it.  text/calendar part is replaced by cal and plain and html parts
    // are replaced by plainDescPart and htmlDescPart.
    private static MimeMessage createMergedMessage(
            Address fromAddr, Address senderAddr,
            MimeMessage mmInv, Invite inv, ZVCalendar cal,
            MimeBodyPart plainDescPart, MimeBodyPart htmlDescPart)
    throws ServiceException {
        try {
            String uid = inv.getUid();
            if (mmInv != null) {
                MimeMessage mm = new MimeMessage(mmInv);  // Get a copy so we can modify it.
                // Discard all old headers except Subject and Content-*.
                Enumeration eh = mmInv.getAllHeaders();
                while (eh.hasMoreElements()) {
                    Header hdr = (Header) eh.nextElement();
                    String hdrNameUpper = hdr.getName().toUpperCase();
                    if (!hdrNameUpper.startsWith("CONTENT-") && !hdrNameUpper.equals("SUBJECT")) {
                        mm.removeHeader(hdr.getName());
                    }
                }

                mm.setSentDate(new Date());
                mm.setRecipients(javax.mail.Message.RecipientType.TO, (Address[]) null);
                mm.setRecipients(javax.mail.Message.RecipientType.CC, (Address[]) null);
                mm.setRecipients(javax.mail.Message.RecipientType.BCC, (Address[]) null);

                if (fromAddr != null)
                    mm.setFrom(fromAddr);
                if (senderAddr != null)
                    mm.setSender(senderAddr);

                // Find and replace the existing calendar part with the new calendar object.
                ReplacingVisitor visitor = new ReplacingVisitor(uid, cal, plainDescPart, htmlDescPart);
                visitor.accept(mm);

                mm.saveChanges();
                return mm;
            } else {
                String subject = inv.getName();
                String desc, descHtml;
                try {
                    Object plainContent = plainDescPart != null ? plainDescPart.getContent() : null;
                    desc = plainContent != null ? plainContent.toString() : null;
                    Object htmlContent = htmlDescPart != null ? htmlDescPart.getContent() : null;
                    descHtml = htmlContent != null ? htmlContent.toString() : null;
                } catch (MessagingException e) {
                    throw ServiceException.FAILURE(
                            "Messaging Exception while retrieving description", e);
                } catch (IOException e) {
                    throw ServiceException.FAILURE(
                            "Messaging Exception while retrieving description", e);
                }
                return CalendarMailSender.createCalendarMessage(fromAddr, senderAddr, null, subject, desc, descHtml, uid, cal, false);
            }
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Messaging Exception while building calendar message from source MimeMessage", e);
        }
    }

    // MimeVisitor that detects text/plain and text/html description parts
    // It just looks for the first part encountered, for plain and html.
    private static class DescDetectVisitor extends MimeVisitor {
        private MimeBodyPart mPlainPart;
        private MimeBodyPart mHtmlPart;

        public DescDetectVisitor() {
        }

        public MimeBodyPart getPlainDescPart() { return mPlainPart; }
        public MimeBodyPart getHtmlDescPart()  { return mHtmlPart; }

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
            if (mPlainPart == null && matchingType(bp, MimeConstants.CT_TEXT_PLAIN))
                mPlainPart = bp;
            if (mHtmlPart == null && matchingType(bp, MimeConstants.CT_TEXT_HTML))
                mHtmlPart = bp;
            return false;
        }

        @Override
        protected boolean visitMessage(MimeMessage mm, VisitPhase visitKind) throws MessagingException {
            return false;
        }

        @Override
        protected boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) throws MessagingException {
            return false;
        }
    }

    // MimeVisitor that replaces text/plain, text/html, and text/calendar parts
    // First part of each type encountered is replaced.
    private static class ReplacingVisitor extends MimeVisitor {

        private String mUid;
        private ZVCalendar mCalNew;          // data from which to generate new text/calendar part
        private MimeBodyPart mPlainNew;      // new text/plain part to replace with
        private MimeBodyPart mHtmlNew;       // new text/html part to replace with
        private MimeBodyPart mCalendarPart;  // existing text/calendar part
        private MimeBodyPart mPlainPart;     // existing text/plain part
        private MimeBodyPart mHtmlPart;      // existing text/html part
        private boolean mCalendarPartReplaced;
        private boolean mPlainPartReplaced;
        private boolean mHtmlPartReplaced;

        public ReplacingVisitor(String uid, ZVCalendar cal, MimeBodyPart plainNew, MimeBodyPart htmlNew) {
            mUid = uid;
            mCalNew = cal;
            mPlainNew = plainNew;
            mHtmlNew = htmlNew;
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
            // Look for first encounter for each type.
            if (mCalendarPart == null && matchingType(bp, MimeConstants.CT_TEXT_CALENDAR))
                mCalendarPart = bp;
            if (mPlainPart == null && matchingType(bp, MimeConstants.CT_TEXT_PLAIN))
                mPlainPart = bp;
            if (mHtmlPart == null && matchingType(bp, MimeConstants.CT_TEXT_HTML))
                mHtmlPart = bp;
            return false;
        }

        @Override
        protected boolean visitMessage(MimeMessage mm, VisitPhase visitKind) throws MessagingException {
            if (VisitPhase.VISIT_END.equals(visitKind)) {
                if (!mCalendarPartReplaced) {
                    // This message either had text/calendar at top level or none at all.
                    // In both cases, set the new calendar as top level content.
                    setCalendarContent(mm, mUid, mCalNew);
                }
                return true;
            } else {
                return false;
            }
        }

        private static void setCalendarContent(Part part, String uid, ZVCalendar cal) throws MessagingException {
            String filename = "meeting.ics";
            part.setDataHandler(new DataHandler(new CalendarDataSource(cal, uid, filename)));
        }

        @Override
        protected boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) throws MessagingException {
            boolean modified = false;
            if (VisitPhase.VISIT_END.equals(visitKind)) {
                if (!mPlainPartReplaced && mPlainPart != null) {
                    modified = true;
                    // We have a plain part and we haven't replaced yet.  The plain part must be
                    // a child of this multipart.
                    if (mp.removeBodyPart(mPlainPart)) {
                        if (mPlainNew != null)
                            mp.addBodyPart(mPlainNew);
                        mPlainPartReplaced = true;
                    } else {
                        throw new MessagingException("Unable to remove old plain part");
                    }
                }
                if (!mHtmlPartReplaced && mHtmlPart != null) {
                    modified = true;
                    // We have a html part and we haven't replaced yet.  The html part must be
                    // a child of this multipart.
                    if (mp.removeBodyPart(mHtmlPart)) {
                        if (mHtmlNew != null)
                            mp.addBodyPart(mHtmlNew);
                        mHtmlPartReplaced = true;
                    } else {
                        throw new MessagingException("Unable to remove old html part");
                    }
                }
                if (!mCalendarPartReplaced && mCalendarPart != null) {
                    modified = true;
                    // We have a calendar part and we haven't replaced yet.  The calendar part must be
                    // a child of this multipart.
                    if (mp.removeBodyPart(mCalendarPart)) {
                        MimeBodyPart newCalendarPart = new MimeBodyPart();
                        setCalendarContent(newCalendarPart, mUid, mCalNew);
                        mp.addBodyPart(newCalendarPart);
                        mCalendarPartReplaced = true;
                    } else {
                        throw new MessagingException("Unable to remove old calendar part");
                    }
                }
            }
            return modified;
        }
    }
}
