/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.mime.MimeConstants;

public class CalendarMailSender {

    // custom email header indicating this invite email is intended for another user
    public static final String X_ZIMBRA_CALENDAR_INTENDED_FOR = "X-Zimbra-Calendar-Intended-For";

    public final static class Verb {
        String mName;
        String mPartStat;      // XML participant status
        public Verb(String name, String xmlPartStat) {
            mName = name;
            mPartStat = xmlPartStat;
        }
        public String toString() { return mName; }
        public String getXmlPartStat() { return mPartStat; }
    }

    public final static Verb VERB_ACCEPT    = new Verb("ACCEPT",    IcalXmlStrMap.PARTSTAT_ACCEPTED);
    public final static Verb VERB_DECLINE   = new Verb("DECLINE",   IcalXmlStrMap.PARTSTAT_DECLINED);
    public final static Verb VERB_TENTATIVE = new Verb("TENTATIVE", IcalXmlStrMap.PARTSTAT_TENTATIVE);

    protected static Map<String, Verb> sVerbs;
    static {
        sVerbs = new HashMap<String, Verb>();
        sVerbs.put("accept", VERB_ACCEPT);
        sVerbs.put("decline", VERB_DECLINE);
        sVerbs.put("tentative", VERB_TENTATIVE);
    }

    private static Map<Verb, MsgKey> sVerbMsgKeys;
    static {
        sVerbMsgKeys = new HashMap<Verb, MsgKey>();
        sVerbMsgKeys.put(VERB_ACCEPT, MsgKey.calendarReplySubjectAccept);
        sVerbMsgKeys.put(VERB_DECLINE, MsgKey.calendarReplySubjectDecline);
        sVerbMsgKeys.put(VERB_TENTATIVE, MsgKey.calendarReplySubjectTentative);
    }

    public static Verb parseVerb(String str) throws ServiceException {
        Verb verb = sVerbs.get(str.toLowerCase());
        if (verb != null)
            return verb;
        throw ServiceException.INVALID_REQUEST("Unknown Reply Verb: " + str,
                                               null);
    }

    public static String getReplySubject(Verb verb, String subject, Locale lc) {
        MsgKey key = sVerbMsgKeys.get(verb);
        String prefix = L10nUtil.getMessage(key, lc);
        return prefix + ": " + subject;
    }

    public static String getCancelSubject(String subject, Locale lc) {
        String prefix = L10nUtil.getMessage(MsgKey.calendarSubjectCancelled, lc) + ": ";
        if (subject != null && subject.startsWith(prefix))
            return subject;
        else
            return prefix + subject;
    }

    public static MimeMessage createOrganizerChangeMessage(Account fromAccount, Account authAccount, boolean asAdmin,
                                                           CalendarItem calItem, Invite inv, List<Address> rcpts)
    throws ServiceException {
        ZOrganizer organizer = inv.getOrganizer();
        assert(organizer != null);
        boolean onBehalfOf = organizer.hasSentBy();
        String senderAddr = onBehalfOf ? organizer.getSentBy() : organizer.getAddress();

        Locale locale = fromAccount.getLocale();

        boolean hidePrivate = !calItem.isPublic() && !calItem.allowPrivateAccess(authAccount, asAdmin);
        String subject;
        if (hidePrivate)
            subject = L10nUtil.getMessage(MsgKey.calendarSubjectWithheld, locale);
        else
            subject = inv.getName();
        StringBuilder sb = new StringBuilder("Organizer has been changed to " + fromAccount.getName());
        sb.append("\r\n\r\n");

        if (!hidePrivate) {
            MimeMessage mmInv = inv.getMimeMessage();
            if (mmInv != null)
                attachInviteSummary(sb, inv, mmInv, locale);
        }

        ZVCalendar iCal = inv.newToICalendar(true);

        Address from = AccountUtil.getFriendlyEmailAddress(fromAccount);
        Address sender = null;
        if (onBehalfOf) {
            try {
                sender = new InternetAddress(senderAddr);
            } catch (AddressException e) {
                throw MailServiceException.ADDRESS_PARSE_ERROR(e);
            }
        }
        return createCalendarMessage(from, sender, rcpts, subject, sb.toString(), null, inv.getUid(), iCal);
    }

    public static MimeMessage createDefaultReply(Account fromAccount, Account authAccount, boolean asAdmin,
                                                 boolean onBehalfOf,
                                                 CalendarItem calItem, Invite inv,
                                                 MimeMessage mmInv, String replySubject, Verb verb,
                                                 String additionalMsgBody, ZVCalendar iCal)
    throws ServiceException {
        return createDefaultReply(fromAccount, authAccount, asAdmin, onBehalfOf, calItem, inv, mmInv,
                                  replySubject, verb, false, additionalMsgBody, iCal);
    }

    public static MimeMessage createDefaultReply(Account fromAccount, Account authAccount, boolean asAdmin,
                                                 boolean onBehalfOf,
                                                 CalendarItem calItem, Invite inv,
                                                 MimeMessage mmInv, String replySubject, Verb verb,
                                                 boolean partialAccept,
                                                 String additionalMsgBody, ZVCalendar iCal)
    throws ServiceException {
        Locale lc;
        InternetAddress organizerAddress;
        if (inv.hasOrganizer()) {
            ZOrganizer org = inv.getOrganizer();
            organizerAddress = org.getReplyAddress();  // organizer or sent-by
            Account organizer = Provisioning.getInstance().get(AccountBy.name, organizerAddress.getAddress());
            lc = organizer != null ? organizer.getLocale() : authAccount.getLocale();
        } else {
            organizerAddress = null;
            lc = authAccount.getLocale();
        }

        String fromDisplayName = fromAccount.getAttr(Provisioning.A_displayName, fromAccount.getName());
        StringBuilder replyText = new StringBuilder();
        boolean isResourceAccount = fromAccount instanceof CalendarResource;

        MsgKey statusMsgKey;
        if (VERB_ACCEPT.equals(verb)) {
            if (isResourceAccount) {
                if (partialAccept)
                    statusMsgKey = MsgKey.calendarResourceDefaultReplyPartiallyAccept;
                else
                    statusMsgKey = MsgKey.calendarResourceDefaultReplyAccept;
            } else {
                statusMsgKey = MsgKey.calendarDefaultReplyAccept;
            }
        } else if (VERB_DECLINE.equals(verb)) {
            if (isResourceAccount) {
                if (partialAccept)
                    statusMsgKey = MsgKey.calendarResourceDefaultReplyPartiallyDecline;
                else
                    statusMsgKey = MsgKey.calendarResourceDefaultReplyDecline;
            } else {
                statusMsgKey = MsgKey.calendarDefaultReplyDecline;
            }
        } else if (VERB_TENTATIVE.equals(verb)) {
            if (isResourceAccount)
                statusMsgKey = MsgKey.calendarResourceDefaultReplyTentativelyAccept;
            else
                statusMsgKey = MsgKey.calendarDefaultReplyTentativelyAccept;
        } else {
            statusMsgKey = MsgKey.calendarDefaultReplyOther;
        }

        String statusMsg;
        if (!statusMsgKey.equals(MsgKey.calendarDefaultReplyOther))
            statusMsg = L10nUtil.getMessage(statusMsgKey, lc, fromDisplayName);
        else
            statusMsg = L10nUtil.getMessage(statusMsgKey, lc, fromDisplayName, verb.toString());
        replyText.append(statusMsg).append("\r\n\r\n");

        if (additionalMsgBody != null) {
            replyText.append(additionalMsgBody).append("\r\n");
        }

        boolean allowPrivateAccess = calItem != null ? calItem.allowPrivateAccess(authAccount, asAdmin) : true;
        if (inv.isPublic() || allowPrivateAccess) {
            attachInviteSummary(replyText, inv, mmInv, lc);
        }

        List<Address> toList = new ArrayList<Address>(1);
        if (organizerAddress != null)
            toList.add(organizerAddress);
        Address senderAddr = null;
        if (onBehalfOf)
            senderAddr = AccountUtil.getFriendlyEmailAddress(authAccount);
        return createCalendarMessage(AccountUtil.getFriendlyEmailAddress(fromAccount),
                senderAddr, toList, replySubject, replyText.toString(), null, inv.getUid(), iCal);
    }

    private static void attachInviteSummary(StringBuilder sb, Invite inv, MimeMessage mmInv, Locale lc)
    throws ServiceException {
        String notes = inv.getDescription();
        if ((notes == null || notes.length() < 1) && mmInv != null)
            notes = Invite.getDescription(mmInv, MimeConstants.CT_TEXT_PLAIN);
        if (notes != null && notes.length() > 0) {
            // Remove Outlook's special "*~*~*~*" delimiter from original
            // body. If we leave it in, Outlook will hide all text above
            // that line.
            notes = notes.replaceAll("[\\r\\n]+[\\*~]+[\\r\\n]+",
                                     "\r\n\r\n ~ ~ ~ ~ ~ ~ ~ ~ ~\r\n\r\n");
            sb.append("\r\n-----");
            sb.append(L10nUtil.getMessage(
                    MsgKey.calendarResourceReplyOriginalInviteSeparatorLabel, lc));
            sb.append("-----\r\n\r\n");
            sb.append(notes);
        }
    }

    public static String formatDateTime(Date d, TimeZone tz, Locale lc) {
        String dateTimeFmt =
            L10nUtil.getMessage(MsgKey.calendarResourceConflictDateTimeFormat, lc);
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat(dateTimeFmt, lc);
        dateTimeFormat.setTimeZone(tz);
        return dateTimeFormat.format(d);
    }

    public static String formatTime(Date t, TimeZone tz, Locale lc) {
        String timeOnlyFmt =
            L10nUtil.getMessage(MsgKey.calendarResourceConflictTimeOnlyFormat, lc);
        SimpleDateFormat timeFormat = new SimpleDateFormat(timeOnlyFmt, lc);
        timeFormat.setTimeZone(tz);
        return timeFormat.format(t);
    }

    public static String formatDate(Date t, TimeZone tz, Locale lc) {
        String dateOnlyFmt =
            L10nUtil.getMessage(MsgKey.calendarResourceConflictDateOnlyFormat, lc);
        SimpleDateFormat timeFormat = new SimpleDateFormat(dateOnlyFmt, lc);
        timeFormat.setTimeZone(tz);
        return timeFormat.format(t);
    }

    /**
     * Builds the TO: list for calendar item updates by iterating over
     * the list of ATTENDEEs
     * 
     * @param iter
     * @return
     */
    public static List<Address> toListFromAttendees(List<ZAttendee> list)
    throws MailServiceException {
        List<Address> toList = new ArrayList<Address>(list.size());
        for (ZAttendee attendee : list) {
            toList.add(attendee.getFriendlyAddress());
        }
        return toList;
    }

    public static MimeMessage createCancelMessage(Account fromAccount, Account senderAccount, boolean asAdmin,
                                                  boolean onBehalfOf,
                                                  List<Address> toAddrs,
                                                  CalendarItem calItem, Invite inv, String text, ZVCalendar iCal)
    throws ServiceException {
        Locale locale = !onBehalfOf ? fromAccount.getLocale() : senderAccount.getLocale();
        Invite defaultInv = calItem.getDefaultInviteOrNull();

        boolean hidePrivate = !calItem.isPublic() && !calItem.allowPrivateAccess(senderAccount, asAdmin);
        String defaultSubject;
        if (hidePrivate)
            defaultSubject = L10nUtil.getMessage(MsgKey.calendarSubjectWithheld, locale);
        else
            defaultSubject = defaultInv != null ? defaultInv.getName() : "";
        String sbj = getCancelSubject(defaultSubject, locale);
        StringBuilder sb = new StringBuilder(text);
        sb.append("\r\n\r\n");

        if (!inv.equals(defaultInv) && inv.getStartTime() != null) {
            sb.append(L10nUtil.getMessage(MsgKey.calendarCancelAppointmentInstanceWhich, locale));
            sb.append(" ");
            ParsedDateTime start = inv.getStartTime();
            TimeZone tz = start.getTimeZone();
            Date startDate = new Date(start.getUtcTime());
            sb.append(CalendarMailSender.formatDateTime(startDate, tz, locale));
            sb.append("\r\n\r\n");
        }

        if (!hidePrivate) {
            MimeMessage mmInv = inv.getMimeMessage();
            if (mmInv == null && defaultInv != null)
                mmInv = defaultInv.getMimeMessage();
            if (mmInv != null)
                attachInviteSummary(sb, inv, mmInv, locale);
        }
        
        Address from = AccountUtil.getFriendlyEmailAddress(fromAccount);
        Address sender = null;
        if (onBehalfOf)
            sender = AccountUtil.getFriendlyEmailAddress(senderAccount);

        return createCalendarMessage(
                from, sender, toAddrs, sbj, sb.toString(), null,
                defaultInv != null ? defaultInv.getUid() : "unknown", iCal);
    }

    public static MimeMessage createCalendarMessage(Invite inv)
    throws ServiceException {
        String subject = inv.getName();
        String desc = inv.getDescription();
        String descHtml = inv.getDescriptionHtml();
        String uid = inv.getUid();
        ZVCalendar cal = inv.newToICalendar(true);
        return createCalendarMessage(null, null, null, subject, desc, descHtml, uid, cal);
    }

    public static MimeMessage createCalendarMessage(
            Address fromAddr, Address senderAddr, List<Address> toAddrs,
            String subject, String desc, String descHtml,
            String uid, ZCalendar.ZVCalendar cal)
    throws ServiceException {
        return createCalendarMessage(fromAddr, senderAddr, toAddrs, subject, desc, descHtml, uid, cal, true);
    }

    public static MimeMessage createCalendarMessage(
            Address fromAddr, Address senderAddr, List<Address> toAddrs,
            String subject, String desc, String descHtml,
            String uid, ZCalendar.ZVCalendar cal, boolean replyToSender)
    throws ServiceException {
        if (desc == null)
            desc = "";
        try {
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());

            MimeMultipart mmp = new MimeMultipart("alternative");
            mm.setContent(mmp);

            // Add the text as DESCRIPTION property in the iCalendar part.
            // MS Entourage for Mac wants this.  It ignores text/plain and
            // text/html MIME parts.
            cal.addDescription(desc, null);

            // ///////
            // TEXT part (add me first!)
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(desc, MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // HTML part is needed to keep Outlook happy as it doesn't know
            // how to deal with a message with only text/plain but no HTML.
            MimeBodyPart htmlPart = new MimeBodyPart();
            if (descHtml != null) {
                ContentType ct = new ContentType(MimeConstants.CT_TEXT_HTML);
                ct.setParameter(MimeConstants.P_CHARSET, MimeConstants.P_CHARSET_UTF8);
                htmlPart.setText(descHtml, MimeConstants.P_CHARSET_UTF8);
                htmlPart.setHeader("Content-Type", ct.toString());
            } else {
                htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(desc)));
            }
            mmp.addBodyPart(htmlPart);

            // ///////
            // CALENDAR part
            MimeBodyPart icalPart = makeICalIntoMimePart(uid, cal);
            mmp.addBodyPart(icalPart);

            // ///////
            // MESSAGE HEADERS
            if (subject != null)
                mm.setSubject(subject, MimeConstants.P_CHARSET_UTF8);

            if (toAddrs != null) {
                Address[] addrs = new Address[toAddrs.size()];
                toAddrs.toArray(addrs);
                mm.addRecipients(javax.mail.Message.RecipientType.TO, addrs);
            }
            if (fromAddr != null)
                mm.setFrom(fromAddr);
            if (senderAddr != null) {
                mm.setSender(senderAddr);
                if (replyToSender)
                    mm.setReplyTo(new Address[]{senderAddr});
            }
            mm.setSentDate(new Date());
            mm.saveChanges();

            return mm;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Messaging Exception while building MimeMessage from invite", e);
        }
    }

    private static class CalendarPartReplacingVisitor extends MimeVisitor {

        private String mUid;
        private ZVCalendar mCal;
        private boolean mReplaced;
        private MimeBodyPart mCalendarPart;

        public CalendarPartReplacingVisitor(String uid, ZVCalendar cal) {
            mUid = uid;
            mCal = cal;
        }

        private static boolean isCalendarPart(Part part) throws MessagingException {
            String mmCtStr = part.getContentType();
            if (mmCtStr != null) {
                ContentType mmCt = new ContentType(mmCtStr);
                return mmCt.match(MimeConstants.CT_TEXT_CALENDAR);
            }
            return false;
        }

        @Override
        protected boolean visitBodyPart(MimeBodyPart bp) throws MessagingException {
            // Look for the first text/calendar part encountered.
            if (mCalendarPart == null && isCalendarPart(bp))
                mCalendarPart = bp;
            return false;
        }

        @Override
        protected boolean visitMessage(MimeMessage mm, VisitPhase visitKind) throws MessagingException {
            if (VisitPhase.VISIT_END.equals(visitKind)) {
                if (!mReplaced) {
                    // This message either had text/calendar at top level or none at all.
                    // In both cases, set the new calendar as top level content.
                    setCalendarContent(mm, mUid, mCal);
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) throws MessagingException {
            if (VisitPhase.VISIT_END.equals(visitKind)) {
                if (!mReplaced && mCalendarPart != null) {
                    // We have a calendar part and we haven't replaced yet.  The calendar part must be
                    // a child of this multipart.
                    if (mp.removeBodyPart(mCalendarPart)) {
                        MimeBodyPart newCalendarPart = new MimeBodyPart();
                        setCalendarContent(newCalendarPart, mUid, mCal);
                        mp.addBodyPart(newCalendarPart);
                        mReplaced = true;
                        return true;
                    } else {
                        throw new MessagingException("Unable to remove old calendar part");
                    }
                }
            }
            return false;
        }
    }

    public static MimeMessage createCalendarMessage(
            Address fromAddr, Address senderAddr, List<Address> toAddrs,
            MimeMessage srcMm, Invite inv, ZVCalendar cal,
            boolean replyToSender)
    throws ServiceException {
        try {
            String uid = inv.getUid();
            if (srcMm != null) {
                MimeMessage mm = new MimeMessage(srcMm);  // Get a copy so we can modify it.
                // Discard all old headers except Subject and Content-*.
                Enumeration eh = srcMm.getAllHeaders();
                while (eh.hasMoreElements()) {
                    Header hdr = (Header) eh.nextElement();
                    String hdrNameUpper = hdr.getName().toUpperCase();
                    if (!hdrNameUpper.startsWith("CONTENT-") && !hdrNameUpper.equals("SUBJECT")) {
                        mm.removeHeader(hdr.getName());
                    }
                }

                mm.setSentDate(new Date());
    
                if (toAddrs != null) {
                    Address[] addrs = new Address[toAddrs.size()];
                    toAddrs.toArray(addrs);
                    mm.setRecipients(javax.mail.Message.RecipientType.TO, addrs);
                } else {
                    mm.setRecipients(javax.mail.Message.RecipientType.TO, (Address[]) null);
                }
                mm.setRecipients(javax.mail.Message.RecipientType.CC, (Address[]) null);
                mm.setRecipients(javax.mail.Message.RecipientType.BCC, (Address[]) null);

                if (fromAddr != null)
                    mm.setFrom(fromAddr);
                if (senderAddr != null) {
                    mm.setSender(senderAddr);
                    if (replyToSender)
                        mm.setReplyTo(new Address[]{senderAddr});
                }
    
                // Find and replace the existing calendar part with the new calendar object.
                CalendarPartReplacingVisitor visitor = new CalendarPartReplacingVisitor(uid, cal);
                visitor.accept(mm);
    
                mm.saveChanges();
                return mm;
            } else {
                String subject = inv.getName();
                String desc = inv.getDescription();
                String descHtml = inv.getDescriptionHtml();
                return createCalendarMessage(fromAddr, senderAddr, toAddrs, subject, desc, descHtml, uid, cal, false);
            }
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Messaging Exception while building calendar message from source MimeMessage", e);
        }
    }

    public static MimeMessage createForwardedInviteMessage(MimeMessage mmOrig, String origSenderEmail, String forwarderEmail, String[] forwardTo)
    throws ServiceException {
        List<Address> rcpts = new ArrayList<Address>(forwardTo.length);
        for (String to : forwardTo) {
            try {
                rcpts.add(new InternetAddress(to));
            } catch (AddressException e) {
                ZimbraLog.calendar.warn("Ignoring invalid address \"" + to + "\" during invite forward");
            }
        }
        if (rcpts.isEmpty())
            return null;
        MimeMessage mm = null;
        try {
            mm = new MimeMessage(mmOrig);
            mm.removeHeader("To");
            mm.removeHeader("Cc");
            mm.removeHeader("Bcc");
            mm.addRecipients(RecipientType.TO, rcpts.toArray(new Address[0]));
            // Set Reply-To to the original sender.
            mm.setReplyTo(new Address[] { new InternetAddress(origSenderEmail) });
            mm.removeHeader("Date");
            mm.removeHeader("Message-ID");
            mm.removeHeader("Return-Path");
            mm.removeHeader("Received");

            // Set special header to indicate the forwarding attendee.
            mm.setHeader(CalendarMailSender.X_ZIMBRA_CALENDAR_INTENDED_FOR, forwarderEmail);

            mm.saveChanges();
        } catch (MessagingException e) {
            ZimbraLog.calendar.warn("Unable to compose email for invite forwarding", e);
        }
        return mm;
    }

    private static MimeMessage createCalendarInviteDeniedMessage(
            Account fromAccount, Account senderAccount, boolean onBehalfOf, boolean allowPrivateAccess,
            Address toAddr, Invite inv, MsgKey bodyTextKey)
    throws ServiceException {
        Locale locale = !onBehalfOf ? fromAccount.getLocale() : senderAccount.getLocale();
        String subject = L10nUtil.getMessage(MsgKey.calendarReplySubjectDecline, locale) + ": " + inv.getName();
        String text = L10nUtil.getMessage(bodyTextKey, locale);
        String uid = inv.getUid();
        ParsedDateTime exceptDt = null;
        if (inv.hasRecurId())
            exceptDt = inv.getRecurId().getDt();
        Invite replyInv = replyToInvite(fromAccount, senderAccount, onBehalfOf, allowPrivateAccess, inv, VERB_DECLINE, subject, exceptDt);
        ZVCalendar iCal = replyInv.newToICalendar(true);
        Address fromAddr = AccountUtil.getFriendlyEmailAddress(fromAccount);
        Address senderAddr = null;
        if (onBehalfOf)
            senderAddr = AccountUtil.getFriendlyEmailAddress(senderAccount);
        List<Address> toAddrs = new ArrayList<Address>(1);
        toAddrs.add(toAddr);
        return createCalendarMessage(fromAddr, senderAddr, toAddrs, subject, text, null, uid, iCal);
    }

    public static void sendInviteDeniedMessage(
            final OperationContext octxt, Account fromAccount, Account senderAccount,
            boolean onBehalfOf, boolean allowPrivateAccess,
            final Mailbox mbox, final ItemId origMsgId, String toEmail, Invite inv)
    throws ServiceException {
        Address toAddr;
        try {
            toAddr = new InternetAddress(toEmail);
        } catch (AddressException e) {
            throw ServiceException.FAILURE("Bad address: " + toEmail, e);
        }
        MsgKey bodyTextKey;
        if (fromAccount instanceof CalendarResource)
            bodyTextKey = MsgKey.calendarResourceDefaultReplyPermissionDenied;
        else
            bodyTextKey = MsgKey.calendarUserReplyPermissionDenied;
        final MimeMessage mm = createCalendarInviteDeniedMessage(
                fromAccount, senderAccount, onBehalfOf, allowPrivateAccess, toAddr, inv, bodyTextKey);

        // Send in a separate thread to avoid nested transaction error when saving a copy to Sent folder.
        Runnable r = new Runnable() {
            public void run() {
                try {
                    mbox.getMailSender().sendMimeMessage(octxt, mbox, true, mm, null, null,
                            origMsgId, MailSender.MSGTYPE_REPLY, null, true, false);
                } catch (ServiceException e) {
                    ZimbraLog.calendar.warn("Ignoring error while sending permission-denied auto reply", e);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("OutOfMemoryError while sending permission-denied auto reply", e);
                }
            }
        };
        Thread senderThread = new Thread(r, "CalendarPermDeniedReplySender");
        senderThread.setDaemon(true);
        senderThread.start();
    }

    public static void sendInviteForwardMessage(
            final OperationContext octxt, final Mailbox mbox, final ItemId origMsgId, final MimeMessage mm)
    throws ServiceException {
        // Send in a separate thread to avoid nested transaction error when saving a copy to Sent folder.
        Runnable r = new Runnable() {
            public void run() {
                try {
                    MailSender sender = mbox.getMailSender().setSaveToSent(true)
                        .setOriginalMessageId(origMsgId).setReplyType(MailSender.MSGTYPE_REPLY)
                        .setForceSendPartial(true).setSkipSendAsCheck(true);
                    sender.sendMimeMessage(octxt, mbox, mm);
                } catch (ServiceException e) {
                    ZimbraLog.calendar.warn("Ignoring error while sending permission-denied auto reply", e);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("OutOfMemoryError while sending permission-denied auto reply", e);
                }
            }
        };
        Thread senderThread = new Thread(r, "CalendarInviteForwardSender");
        senderThread.setDaemon(true);
        senderThread.start();
    }

    public static Invite replyToInvite(Account acct, Account authAcct,
            boolean onBehalfOf, boolean allowPrivateAccess,
            Invite oldInv,
            Verb verb, String replySubject,
            ParsedDateTime exceptDt)
    throws ServiceException {
        return replyToInvite(acct, null, authAcct, onBehalfOf, allowPrivateAccess,
                             oldInv, verb, replySubject, exceptDt);
    }

    /**
     * RFC2446 4.2.2:
     * 
     * BEGIN:VCALENDAR PRODID:-//ACME/DesktopCalendar//EN METHOD:REPLY
     * VERSION:2.0 BEGIN:VEVENT ATTENDEE;PARTSTAT=ACCEPTED:Mailto:B@example.com
     * ORGANIZER:MAILTO:A@example.com
     * UID:calsrv.example.com-873970198738777@example.com SEQUENCE:0
     * REQUEST-STATUS:2.0;Success DTSTAMP:19970612T190000Z END:VEVENT
     * END:VCALENDAR
     * 
     * @param acct replying account
     * @param identityId use this identity/persona in the reply
     * @param authAcct authenticated account acting on behalf of acct
     * @param oldInv
     * @param verb
     * @param replySubject
     * @return
     * @throws ServiceException
     */
    public static Invite replyToInvite(Account acct, String identityId, Account authAcct,
                                       boolean onBehalfOf, boolean allowPrivateAccess,
                                       Invite oldInv,
                                       Verb verb, String replySubject,
                                       ParsedDateTime exceptDt)
    throws ServiceException {
        Invite reply =
            new Invite(oldInv.getItemType(), ICalTok.REPLY.toString(),
                       new TimeZoneMap(
                               ICalTimeZone.getAccountTimeZone(onBehalfOf ? authAcct : acct)),
                       oldInv.isOrganizer());
        reply.setLocalOnly(false);  // suppress X-ZIMBRA-LOCAL-ONLY property

        reply.getTimeZoneMap().add(oldInv.getTimeZoneMap());
//        reply.setIsAllDayEvent(oldInv.isAllDayEvent());

        Identity identity = null;
        if (identityId != null) {
            identity = acct.getIdentityById(identityId);
            if (identity == null) {
                ZimbraLog.calendar.warn("No such identity " + identityId + " for account " + acct.getName());
                identity = acct.getDefaultIdentity();
            }
        } else {
            identity = acct.getDefaultIdentity();
        }
        String identityAddr = identity.getAttr(Provisioning.A_zimbraPrefFromAddress);
        String identityCn = identity.getAttr(Provisioning.A_zimbraPrefFromDisplay);

        // ATTENDEE -- send back this attendee with the proper status
        ZAttendee meReply = null;
        ZAttendee me = oldInv.getMatchingAttendee(acct, identityId);
        if (me != null) {
            String atAddr = me.getAddress();
            // Use identity's address/cn if possible, overriding the case/name used by the organizer.
            if (identityAddr.equalsIgnoreCase(atAddr)) {
                meReply = new ZAttendee(identityAddr);
                if (identityCn != null)
                    meReply.setCn(identityCn);
            } else {
                meReply = new ZAttendee(atAddr);
                if (me.hasCn())
                    meReply.setCn(me.getCn());
            }
            meReply.setPartStat(verb.getXmlPartStat());
            if (me.hasRole())
                meReply.setRole(me.getRole());
            if (me.hasCUType())
                meReply.setCUType(me.getCUType());
        } else {
            meReply = new ZAttendee(identityAddr);
            meReply.setPartStat(verb.getXmlPartStat());
            if (identityCn != null)
                meReply.setCn(identityCn);
        }
        if (onBehalfOf)
            meReply.setSentBy(authAcct.getName());
        reply.addAttendee(meReply);

        boolean hidePrivate = !oldInv.isPublic() && !allowPrivateAccess;
        reply.setClassProp(oldInv.getClassProp());

        // DTSTART, DTEND, LOCATION (outlook seems to require these,
        // even though it shouldn't)
        reply.setDtStart(oldInv.getStartTime());
        reply.setDtEnd(oldInv.getEffectiveEndTime());
        if (!hidePrivate)
            reply.setLocation(oldInv.getLocation());
        else
            reply.setLocation("");

        // ORGANIZER
        if (oldInv.hasOrganizer())
            reply.setOrganizer(oldInv.getOrganizer());

        // UID
        reply.setUid(oldInv.getUid());

        // RECURRENCE-ID (if necessary)
        if (exceptDt != null) {
            reply.setRecurId(new RecurId(exceptDt, RecurId.RANGE_NONE));
        } else if (oldInv.hasRecurId()) {
            reply.setRecurId(oldInv.getRecurId());
        }

        // SEQUENCE
        reply.setSeqNo(oldInv.getSeqNo());

        // DTSTAMP
        // we should pick "now" -- but the dtstamp MUST be >= the one sent by
        // the organizer,
        // so we'll use theirs if it is after "now"...
        Date now = new Date();
        Date dtStampDate = new Date(oldInv.getDTStamp());
        if (now.after(dtStampDate)) {
            dtStampDate = now;
        }
        reply.setDtStamp(dtStampDate.getTime());

        // SUMMARY
        reply.setName(replySubject);

        // System.out.println("REPLY: "+reply.toVEvent().toString());

        return reply;
    }

    private static void setCalendarContent(Part part, String uid, ZVCalendar cal) throws MessagingException {
        String filename = "meeting.ics";
        part.setDataHandler(new DataHandler(new CalendarDataSource(cal, uid, filename)));
    }

    public static MimeBodyPart makeICalIntoMimePart(String uid, ZVCalendar cal) throws ServiceException {
        try {
            MimeBodyPart mbp = new MimeBodyPart();
            setCalendarContent(mbp, uid, cal);
            return mbp;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Failure creating MimeBodyPart from calendar", e);
        }
    }

    public static MimeMessage createResourceAutoReply(OperationContext octxt, Mailbox mbox,
                                                      Verb verb, boolean partialAccept,
                                                      String additionalMsgBody, CalendarItem calItem,
                                                      Invite inv, Invite[] replies, MimeMessage mmInv)
    throws ServiceException {
        boolean onBehalfOf = false;
        Account acct = mbox.getAccount();
        Account authAcct = acct;
        if (octxt != null) {
            Account authuser = octxt.getAuthenticatedUser();
            if (authuser != null) {
                onBehalfOf = !acct.getId().equalsIgnoreCase(authuser.getId());
                if (onBehalfOf)
                    authAcct = authuser;
            }
        }
        Locale lc;
        Account organizer = inv.getOrganizerAccount();
        if (organizer != null)
            lc = organizer.getLocale();
        else
            lc = authAcct.getLocale();
        boolean asAdmin = octxt != null ? octxt.isUsingAdminPrivileges() : false;
        boolean allowPrivateAccess = calItem.allowPrivateAccess(authAcct, asAdmin);
        boolean hidePrivate = !inv.isPublic() && !allowPrivateAccess;
        String subject;
        if (hidePrivate)
            subject = L10nUtil.getMessage(MsgKey.calendarSubjectWithheld, lc);
        else
            subject = inv.getName();
        String replySubject = getReplySubject(verb, subject, lc);

        // Put all REPLY VEVENTs into a single VCALENDAR object.
        ZVCalendar iCal = null;
        for (Invite replyInv : replies) {
            if (iCal == null) {
                iCal = replyInv.newToICalendar(!hidePrivate);
            } else {
                ZComponent cancelComp = replyInv.newToVComponent(true, !hidePrivate);
                iCal.addComponent(cancelComp);
            }
        }
        return createDefaultReply(acct, authAcct, asAdmin, onBehalfOf, calItem, inv, mmInv,
                                  replySubject, verb, partialAccept, additionalMsgBody, iCal);
    }

    public static void sendResourceAutoReply(final OperationContext octxt, final Mailbox mbox,
                                             final boolean saveToSent,
                                             Verb verb, boolean partialAccept,
                                             String additionalMsgBody, CalendarItem calItem,
                                             Invite inv, Invite[] replies, MimeMessage mmInv)
    throws ServiceException {
        final MimeMessage mm = createResourceAutoReply(octxt, mbox, verb, partialAccept, additionalMsgBody, calItem, inv, replies, mmInv);
        final String replyType = MailSender.MSGTYPE_REPLY;
        final int invId = inv.getMailItemId();

        // Send in a separate thread to avoid nested transaction error when saving a copy to Sent folder.
        Runnable r = new Runnable() {
            public void run() {
                try {
                    mbox.getMailSender().sendMimeMessage(octxt, mbox, saveToSent, mm, null, null,
                            new ItemId(mbox, invId), replyType, null, true, false);
                } catch (ServiceException e) {
                    ZimbraLog.calendar.warn("Ignoring error while sending auto accept/decline reply", e);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("OutOfMemoryError while sending calendar resource auto accept/decline reply", e);
                }
            }
        };
        Thread senderThread = new Thread(r, "CalendarAutoAcceptDeclineReplySender");
        senderThread.setDaemon(true);
        senderThread.start();
    }

    private static class HtmlPartDataSource implements DataSource {
        private static final String CONTENT_TYPE =
            MimeConstants.CT_TEXT_HTML + "; " + MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8;
        private static final String HEAD =
            "<html><body>\n" +
            "<pre style=\"font-family: monospace; font-size: 14px\">\n";
        private static final String TAIL = "</pre>\n</body></html>\n";
        private static final String NAME = "HtmlDataSource";

        private String mText;
        private byte[] mBuf = null;

        public HtmlPartDataSource(String text) {
            mText = text;
            mText = mText.replaceAll("&", "&amp;");
            mText = mText.replaceAll("<", "&lt;");
            mText = mText.replaceAll(">", "&gt;");
        }

        public String getContentType() {
            return CONTENT_TYPE;
        }

        public InputStream getInputStream() throws IOException {
            synchronized(this) {
                if (mBuf == null) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    OutputStreamWriter wout =
                        new OutputStreamWriter(buf, MimeConstants.P_CHARSET_UTF8);
                    String text = HEAD + mText + TAIL;
                    wout.write(text);
                    wout.flush();
                    mBuf = buf.toByteArray();
                }
            }
            ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
            return in;
        }

        public String getName() {
            return NAME;
        }

        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }
    }
}
