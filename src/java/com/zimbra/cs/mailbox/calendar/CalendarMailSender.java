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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.L10nUtil.MsgKey;

public class CalendarMailSender {

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

    public static String getReplySubject(Verb verb, Invite inv, Locale lc) {
        MsgKey key = sVerbMsgKeys.get(verb);
        String prefix = L10nUtil.getMessage(key, lc);
        return prefix + ": " + inv.getName();
    }

    public static String getCancelSubject(Invite inv, Locale lc) {
        String prefix = L10nUtil.getMessage(MsgKey.calendarSubjectCancelled, lc);
        return prefix + ": " + inv.getName();
    }

    public static MimeMessage createDefaultReply(Account fromAccount,
                                                 Appointment appt,
                                                 Invite inv,
                                                 MimeMessage mmInv,
                                                 String replySubject,
                                                 Verb verb,
                                                 String additionalMsgBody,
                                                 ZVCalendar iCal)
    throws ServiceException {
        try {
            Locale lc;
            Address organizerAddress;
            Account organizer = inv.getOrganizerAccount();
            if (organizer != null) {
                lc = organizer.getLocale();
                organizerAddress =
                    AccountUtil.getFriendlyEmailAddress(organizer);
            } else {
                lc = fromAccount.getLocale();
                organizerAddress = inv.getOrganizer().getFriendlyAddress();
            }

            String fromDisplayName =
                fromAccount.getAttr(Provisioning.A_displayName,
                                    fromAccount.getName());
            StringBuilder replyText = new StringBuilder();
            MsgKey statusMsgKey;
            boolean isResourceAccount = fromAccount instanceof CalendarResource;
            if (VERB_ACCEPT.equals(verb)) {
                if (isResourceAccount)
                    statusMsgKey = MsgKey.calendarResourceDefaultReplyAccept;
                else
                    statusMsgKey = MsgKey.calendarDefaultReplyAccept;
            } else if (VERB_DECLINE.equals(verb)) {
                if (isResourceAccount)
                    statusMsgKey = MsgKey.calendarResourceDefaultReplyDecline;
                else
                    statusMsgKey = MsgKey.calendarDefaultReplyDecline;
            } else if (VERB_TENTATIVE.equals(verb)) {
                if (isResourceAccount)
                    statusMsgKey = MsgKey.calendarResourceDefaultReplyTentativelyAccept;
                else
                    statusMsgKey = MsgKey.calendarDefaultReplyTentativelyAccept;
            } else
                statusMsgKey = MsgKey.calendarDefaultReplyOther;
            String statusMsg;
            if (!statusMsgKey.equals(MsgKey.calendarDefaultReplyOther))
                statusMsg = L10nUtil.getMessage(statusMsgKey,
                                                lc,
                                                fromDisplayName);
            else
                statusMsg = L10nUtil.getMessage(statusMsgKey,
                                                lc,
                                                fromDisplayName,
                                                verb.toString());
            replyText.append(statusMsg).append("\r\n\r\n");

            if (additionalMsgBody != null) {
                replyText.append(additionalMsgBody).append("\r\n");
            }

            attachInviteSummary(replyText, mmInv, lc);

            List<Address> toList = new ArrayList<Address>(1);
            toList.add(organizerAddress);
            return createDefaultCalendarMessage(
                    AccountUtil.getFriendlyEmailAddress(fromAccount),
                    toList, replySubject,
                    replyText.toString(), inv.getUid(), iCal);
        } catch (UnsupportedEncodingException e) {
            throw MailServiceException.ADDRESS_PARSE_ERROR(e);
        }
    }

    private static void attachInviteSummary(StringBuilder sb,
                                            MimeMessage mmInv,
                                            Locale lc)
    throws ServiceException {
        String notes = Invite.getNotes(mmInv);
        if (notes != null) {
            // Remove Outlook's special "*~*~*~*" delimiter from original
            // body. If we leave it in, Outlook will hide all text above
            // that line.
            notes = notes.replaceAll("[\\r\\n]+[\\*~]+[\\r\\n]+",
                                     "\r\n\r\n ~ ~ ~ ~ ~ ~ ~ ~ ~\r\n\r\n");
            sb.append("\r\n-----");
            sb.append(L10nUtil.getMessage(
                    MsgKey.calendarResourceReplyOriginalInviteSeparatorLabel, lc));
            sb.append("-----\r\n");
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

    /**
     * Builds the TO: list for appointment updates by iterating over
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

    public static MimeMessage createCancelMessage(Account fromAccount,
                                                  List<Address> toAddrs,
                                                  Invite defaultInv,
                                                  Invite cancelInstanceInv,
                                                  String text,
                                                  ZVCalendar iCal)
    throws ServiceException {
        Locale locale = fromAccount.getLocale();
        String sbj = getCancelSubject(defaultInv, locale);
        StringBuilder sb = new StringBuilder(text);
        sb.append("\r\n\r\n");
        if (cancelInstanceInv != null) {
            sb.append(L10nUtil.getMessage(MsgKey.calendarCancelAppointmentInstanceWhich, locale));
            sb.append(" ");
            ParsedDateTime start = cancelInstanceInv.getStartTime();
            TimeZone tz = start.getTimeZone();
            Date startDate = new Date(start.getUtcTime());
            sb.append(CalendarMailSender.formatDateTime(startDate, tz, locale));
            sb.append("\r\n\r\n");
        }

        MimeMessage mmInv = defaultInv.getMimeMessage();
        if (mmInv != null)
            attachInviteSummary(sb, mmInv, locale);
        
        Address sender;
        try {
            sender = AccountUtil.getFriendlyEmailAddress(fromAccount);
        } catch (UnsupportedEncodingException e) {
            throw MailServiceException.ADDRESS_PARSE_ERROR(e);
        }

        return createDefaultCalendarMessage(
                sender, toAddrs, sbj, sb.toString(), defaultInv.getUid(), iCal);
    }

    private static MimeMessage createDefaultCalendarMessage(
            Address fromAddr, List<Address> toAddrs,
            String subject, String text,
            String uid, ZCalendar.ZVCalendar cal)
    throws ServiceException {
        try {
            MimeMessage mm = new MimeMessage(JMSession.getSession()) {
                protected void updateHeaders() throws MessagingException {
                    String msgid = getMessageID();
                    super.updateHeaders();
                    if (msgid != null)
                        setHeader("Message-ID", msgid);
                }
            };
            MimeMultipart mmp = new MimeMultipart("alternative");
            mm.setContent(mmp);

            // ///////
            // TEXT part (add me first!)
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(text, Mime.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // HTML part is needed to keep Outlook happy as it doesn't know
            // how to deal with a message with only text/plain but no HTML.
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(text)));
            mmp.addBodyPart(htmlPart);

            // ///////
            // CALENDAR part
            MimeBodyPart icalPart = makeICalIntoMimePart(uid, cal);
            mmp.addBodyPart(icalPart);

            // ///////
            // MESSAGE HEADERS
            mm.setSubject(subject, Mime.P_CHARSET_UTF8);

            Address[] addrs = new Address[toAddrs.size()];
            toAddrs.toArray(addrs);
            mm.addRecipients(javax.mail.Message.RecipientType.TO, addrs);
            mm.setFrom(fromAddr);
            mm.setSentDate(new Date());
            mm.saveChanges();

            return mm;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Messaging Exception while building InviteReply", e);
        }
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
     * @param acct
     * @param oldInv
     * @param verb
     * @param replySubject
     * @return
     * @throws ServiceException
     */
    public static Invite replyToInvite(Account acct, Invite oldInv,
                                       Verb verb, String replySubject,
                                       ParsedDateTime exceptDt)
    throws ServiceException {
        Invite reply = new Invite(ICalTok.REPLY.toString(), new TimeZoneMap(
                acct.getTimeZone()));

        reply.getTimeZoneMap().add(oldInv.getTimeZoneMap());

        // ATTENDEE -- send back this attendee with the proper status
        ZAttendee meReply = null;
        ZAttendee me = oldInv.getMatchingAttendee(acct);
        if (me != null) {
            meReply = new ZAttendee(me.getAddress());
            meReply.setPartStat(verb.getXmlPartStat());
            if (me.hasRole())
                meReply.setRole(me.getRole());
            if (me.hasCUType())
                meReply.setCUType(me.getCUType());
            if (me.hasCn())
                meReply.setCn(me.getCn());
            reply.addAttendee(meReply);
        } else {
            String name = acct.getName();
            meReply = new ZAttendee(name);
            meReply.setPartStat(verb.getXmlPartStat());
            reply.addAttendee(meReply);
        }

        // DTSTART (outlook seems to require this, even though it shouldn't)
        reply.setDtStart(oldInv.getStartTime());

        // ORGANIZER
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

    public static MimeBodyPart makeICalIntoMimePart(String uid,
            ZCalendar.ZVCalendar iCal) throws ServiceException {
        try {
            MimeBodyPart mbp = new MimeBodyPart();

            String filename = "meeting.ics";
            mbp.setDataHandler(new DataHandler(new CalendarDataSource(iCal,
                    uid, filename)));

            return mbp;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Failure creating MimeBodyPart for InviteReply", e);
        }
    }

    public static int sendReply(OperationContext octxt,
                                Mailbox mbox,
                                boolean saveToSent,
                                Verb verb,
                                String additionalMsgBody,
                                Appointment appt,
                                Invite inv,
                                MimeMessage mmInv)
    throws ServiceException {
        Account acct = mbox.getAccount();
        Locale lc;
        Account organizer = inv.getOrganizerAccount();
        if (organizer != null)
            lc = organizer.getLocale();
        else
            lc = acct.getLocale();
        String replySubject = getReplySubject(verb, inv, lc);

        String replyType = MailSender.MSGTYPE_REPLY;
        // TODO: Handle Exception ID. (last arg of replyToInvite)
        Invite replyInv = replyToInvite(acct, inv, verb, replySubject, null);

        ZVCalendar iCal = replyInv.newToICalendar();
        MimeMessage mm = createDefaultReply(
                acct, appt, inv, mmInv, replySubject,
                verb, additionalMsgBody, iCal);

        int replyMsgId = MailSender.sendMimeMessage(
                octxt, mbox, saveToSent, mm,
                null, null, inv.getMailItemId(),
                replyType, false);
        return replyMsgId;
    }

    private static class HtmlPartDataSource implements DataSource {
        private static final String CONTENT_TYPE =
            Mime.CT_TEXT_HTML + "; " + Mime.P_CHARSET + "=" + Mime.P_CHARSET_UTF8;
        private static final String HEAD =
            "<HTML><BODY>\n" +
            "<PRE style=\"font-family: monospace; font-size: 14px\">\n";
        private static final String TAIL = "</PRE>\n</BODY></HTML>\n";
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
                        new OutputStreamWriter(buf, Mime.P_CHARSET_UTF8);
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

        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }
        
    }
}
