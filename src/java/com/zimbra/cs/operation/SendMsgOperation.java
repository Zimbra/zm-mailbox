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
package com.zimbra.cs.operation;

import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarDataSource;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;

public class SendMsgOperation extends Operation {

    private static int LOAD = 3;
    static {
        Operation.Config c = loadConfig(SendMsgOperation.class);
        if (c != null)
            LOAD = c.mLoad;
    }

    private MimeMessage mMm;
    private List<InternetAddress> mNewContacts;
    private List<Upload> mUploads;
    private int mOrigMsgId;
    private String mReplyType;
    private String mIdentityId;
    private boolean mNoSaveToSent;
    private boolean mIgnoreFailedAddresses;
    private boolean mNeedCalendarSentByFixup;

    private ItemId mMsgId;

    public SendMsgOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
            MimeMessage mm, List<InternetAddress> newContacts, List<Upload> uploads,
            int origMsgId, String replyType, String identityId,
            boolean ignoreFailedAddresses, boolean needCalendarSentByFixup)
    {
        this(session, oc, mbox, req, mm, newContacts, uploads,
             origMsgId, replyType, identityId, false,
             ignoreFailedAddresses, needCalendarSentByFixup);
    }

    public SendMsgOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
            MimeMessage mm, List<InternetAddress> newContacts,  List<Upload> uploads,
            int origMsgId, String replyType, String identityId, boolean noSaveToSent,
            boolean ignoreFailedAddresses, boolean needCalendarSentByFixup) {
        super(session, oc, mbox, req, LOAD);

        mMm = mm;
        mNewContacts = newContacts;
        mUploads = uploads;
        mOrigMsgId = origMsgId;
        mReplyType = replyType;
        mIdentityId = identityId;
        mNoSaveToSent = noSaveToSent;
        mIgnoreFailedAddresses = ignoreFailedAddresses;
        mNeedCalendarSentByFixup = needCalendarSentByFixup;
    }

    protected void callback() throws ServiceException {
        Mailbox mbox = getMailbox();
        if (mNeedCalendarSentByFixup)
            fixupICalendarFromOutlook(mbox);

        if (mNoSaveToSent)
            mMsgId = mbox.getMailSender().sendMimeMessage(getOpCtxt(), mbox, false, mMm, mNewContacts, mUploads,
                                                          mOrigMsgId, mReplyType, null,
                                                          mIgnoreFailedAddresses, false);
        else
            mMsgId = mbox.getMailSender().sendMimeMessage(getOpCtxt(), mbox, mMm, mNewContacts, mUploads,
                    mOrigMsgId, mReplyType, mIdentityId,
                    mIgnoreFailedAddresses, false);
    }

    public ItemId getMsgId() { return mMsgId; }

    private void fixupICalendarFromOutlook(Mailbox ownerMbox)
    throws ServiceException {
        MimeVisitor mv = new OutlookICalendarFixupMimeVisitor();
        try {
            mv.accept(mMm);
        } catch (MessagingException e) {
             throw ServiceException.PARSE_ERROR("Error while fixing up SendMsg for SENT-BY", e);
        }
    }

    /**
     * When Outlook/ZCO sends a calendar invitation or reply message on behalf
     * of another user, the From and Sender message headers are set correctly
     * but the iCalendar object doesn't set SENT-BY parameter on ORGANIZER or
     * ATTENDEE property of VEVENT/VTODO components.  These need to be fixed
     * up.
     * 
     * @author jhahm
     *
     */
    private static class OutlookICalendarFixupMimeVisitor extends MimeVisitor {
        private boolean mNeedFixup;
        private int mMsgDepth;
        private String[] mFromEmails;
        private String mSentBy;

        OutlookICalendarFixupMimeVisitor() {
            mNeedFixup = false;
            mMsgDepth = 0;
        }

        @Override
        protected boolean visitMessage(MimeMessage mm, VisitPhase visitKind) throws MessagingException {
            if (VisitPhase.VISIT_BEGIN.equals(visitKind)) {
                mMsgDepth++;
                if (mMsgDepth == 1) {
                    Address[] fromAddrs = mm.getFrom();
                    Address senderAddr = mm.getSender();
                    if (senderAddr != null && fromAddrs != null) {
                        // If one of the From's is same as Sender, there is nothing to fixup.
                        for (Address from : fromAddrs) {
                            if (from.equals(senderAddr))
                                return false;
                        }

                        if (!(senderAddr instanceof InternetAddress))
                            return false;
                        String sender = ((InternetAddress) senderAddr).getAddress();
                        if (sender == null)
                            return false;

                        String froms[] = new String[fromAddrs.length];
                        for (int i = 0; i < fromAddrs.length; i++) {
                            Address fromAddr = fromAddrs[i];
                            if (fromAddr == null)
                                return false;
                            if (!(fromAddr instanceof InternetAddress))
                                return false;
                            String from = ((InternetAddress) fromAddr).getAddress();
                            if (from == null)
                                return false;
                            froms[i] = "MAILTO:" + from;
                        }

                        mNeedFixup = true;
                        mFromEmails = froms;
                        mSentBy = "MAILTO:" + sender;
                    }
                }
            } else if (VisitPhase.VISIT_END.equals(visitKind)) {
                mMsgDepth--;
            }

            return false;
        }

        @Override
        protected boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) {
            return false;
        }

        @Override
        protected boolean visitBodyPart(MimeBodyPart bp) throws MessagingException {
            // Ignore any forwarded message parts.
            if (mMsgDepth != 1 || !mNeedFixup)
                return false;

            boolean modified = false;

            String ct = Mime.getContentType(bp);
            if (Mime.CT_TEXT_CALENDAR.compareToIgnoreCase(ct) != 0)
                return false;

            ZVCalendar ical;
            try {
                DataSource ds = bp.getDataHandler().getDataSource();
                Reader reader = Mime.getTextReader(ds.getInputStream(), ds.getContentType());
                ical = ZCalendarBuilder.build(reader);
            } catch (Exception e) {
                throw new MessagingException("Unable to parse iCalendar part: " + e.getMessage(), e);
            }

            String uid = null;
            for (Iterator<ZComponent> compIter = ical.getComponentIterator(); compIter.hasNext(); ) {
                ZComponent comp = compIter.next();
                ICalTok compType = comp.getTok();
                if (!ICalTok.VEVENT.equals(compType) && !ICalTok.VTODO.equals(compType))
                    continue;

                for (Iterator<ZProperty> propIter = comp.getPropertyIterator(); propIter.hasNext(); ) {
                    ZProperty prop = propIter.next();
                    ICalTok token = prop.getToken();
                    if (token == null)
                        continue;
                    switch (token) {
                    case UID:
                        uid = prop.getValue();
                        break;
                    case ORGANIZER:
                    case ATTENDEE:
                        String addr = prop.getValue();
                        if (addressMatchesFrom(addr)) {
                            ZParameter sentBy = prop.getParameter(ICalTok.SENT_BY);
                            if (sentBy == null) {
                                prop.addParameter(new ZParameter(ICalTok.SENT_BY, mSentBy));
                                modified = true;
                                ZimbraLog.calendar.info(
                                        "Fixed up " + token + " (" + addr +
                                        ") by adding SENT-BY=" + mSentBy);
                            }
                        }
                        break;
                    }
                }
            }

            if (modified) {
                String filename = bp.getFileName();
                if (filename == null)
                    filename = "meeting.ics";
                String dsname = uid != null ? uid : "fixup";
                bp.setDataHandler(new DataHandler(new CalendarDataSource(ical, dsname, filename)));
            }

            return modified;
        }

        private boolean addressMatchesFrom(String addr) {
            if (addr == null)
                return false;
            for (String from : mFromEmails) {
                if (from.compareToIgnoreCase(addr) == 0)
                    return true;
            }
            return false;
        }
    }
}
