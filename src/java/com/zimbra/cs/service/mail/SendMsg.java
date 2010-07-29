/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Sep 17, 2004
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarDataSource;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.mail.ParseMimeMessage.MimeMessageData;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;


/**
 * Process the <SendMsg> request from the client and send an email message.
 */
public class SendMsg extends MailDocumentHandler {

    static Log sLog = LogFactory.getLog(SendMsg.class);

    private enum SendState { NEW, SENT, PENDING };

    private static final long MAX_IN_FLIGHT_DELAY_MSECS = 4 * Constants.MILLIS_PER_SECOND;
    private static final long RETRY_CHECK_PERIOD_MSECS = 500;

    private static final ItemId NO_MESSAGE_SAVED_TO_SENT = new ItemId((String) null, -1);

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Account account = getRequestedAccount(zsc);
        long quota = account.getMailQuota();
        if (account.isMailAllowReceiveButNotSendWhenOverQuota() && quota != 0 && mbox.getSize() > quota) {
            throw MailServiceException.QUOTA_EXCEEDED(quota);
        }
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        // <m>
        Element msgElem = request.getElement(MailConstants.E_MSG);

        // check to see whether the entire message has been uploaded under separate cover
        String attachId = msgElem.getAttribute(MailConstants.A_ATTACHMENT_ID, null);

        boolean needCalendarSentByFixup = request.getAttributeBool(MailConstants.A_NEED_CALENDAR_SENTBY_FIXUP, false);
        boolean noSaveToSent = request.getAttributeBool(MailConstants.A_NO_SAVE_TO_SENT, false);

        String origId = msgElem.getAttribute(MailConstants.A_ORIG_ID, null);
        ItemId iidOrigId = origId == null ? null : new ItemId(origId, zsc);
        String replyType = msgElem.getAttribute(MailConstants.A_REPLY_TYPE, MailSender.MSGTYPE_REPLY);
        String identityId = msgElem.getAttribute(MailConstants.A_IDENTITY_ID, null);


        SendState state = SendState.NEW;
        ItemId savedMsgId = null;
        Pair<String, ItemId> sendRecord = null;

        // get the "send uid" and check that this isn't a retry of a pending send
        String sendUid = request.getAttribute(MailConstants.A_SEND_UID, null);
        if (sendUid != null) {
            long delay = MAX_IN_FLIGHT_DELAY_MSECS;
            do {
                if (state == SendState.PENDING) {
                    try {
                        delay -= RETRY_CHECK_PERIOD_MSECS;  Thread.sleep(RETRY_CHECK_PERIOD_MSECS);
                    } catch (InterruptedException ie) { }
                }

                Pair<SendState, Pair<String, ItemId>> result = findPendingSend(mbox.getId(), sendUid);
                state = result.getFirst();
                sendRecord = result.getSecond();
            } while (state == SendState.PENDING && delay >= 0);
        }

        if (state == SendState.SENT) {
            // message successfully sent by another thread
            savedMsgId = sendRecord.getSecond();
        } else if (state == SendState.PENDING) {
            // tired of waiting for another thread to complete the send
            throw MailServiceException.TRY_AGAIN("message send already in progress: " + sendUid);
        } else if (state == SendState.NEW) {
            try {
                // holds return data about the MimeMessage
                MimeMessageData mimeData = new MimeMessageData();
                MimeMessage mm;
                if (attachId != null) {
                    mm = parseUploadedMessage(zsc, attachId, mimeData, needCalendarSentByFixup);
                } else {
                    mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, mbox, msgElem, null, mimeData);
                }

                savedMsgId = doSendMessage(octxt, mbox, mm, mimeData.newContacts, mimeData.uploads, iidOrigId,
                    replyType, identityId, noSaveToSent, needCalendarSentByFixup);

                // (need to make sure that *something* gets recorded, because caching
                //   a null ItemId makes the send appear to still be PENDING)
                if (savedMsgId == null)
                    savedMsgId = NO_MESSAGE_SAVED_TO_SENT;

                // and record it in the table in case the client retries the send
                if (sendRecord != null)
                    sendRecord.setSecond(savedMsgId);
            } catch (ServiceException e) {
                clearPendingSend(mbox.getId(), sendRecord);
                throw e;
            } catch (RuntimeException re) {
                clearPendingSend(mbox.getId(), sendRecord);
                throw re;
            }
        }

        Element response = zsc.createElement(MailConstants.SEND_MSG_RESPONSE);
        Element respElement = response.addElement(MailConstants.E_MSG);
        if (savedMsgId != null && savedMsgId != NO_MESSAGE_SAVED_TO_SENT && savedMsgId.getId() > 0)
            respElement.addAttribute(MailConstants.A_ID, ifmt.formatItemId(savedMsgId));
        return response;
    }

    public static ItemId doSendMessage(OperationContext oc, Mailbox mbox, MimeMessage mm, List<InternetAddress> newContacts,
                                       List<Upload> uploads, ItemId origMsgId, String replyType, String identityId,
                                       boolean noSaveToSent, boolean needCalendarSentByFixup)
    throws ServiceException {
        
        if (needCalendarSentByFixup)
            fixupICalendarFromOutlook(mbox, mm);

        if (noSaveToSent)
            return mbox.getMailSender().sendMimeMessage(oc, mbox, false, mm, newContacts, uploads,
                                                        origMsgId, replyType, null, false, false);
        else
            return mbox.getMailSender().sendMimeMessage(oc, mbox, mm, newContacts, uploads,
                                                        origMsgId, replyType, identityId, false, false);
    }

    static MimeMessage parseUploadedMessage(ZimbraSoapContext zsc, String attachId, MimeMessageData mimeData) throws ServiceException {
        return parseUploadedMessage(zsc, attachId, mimeData, false);
    }

    static MimeMessage parseUploadedMessage(ZimbraSoapContext zsc, String attachId, MimeMessageData mimeData,
                                            boolean needCalendarSentByFixup)
    throws ServiceException {
        boolean anySystemMutators = MimeVisitor.anyMutatorsRegistered();

        Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachId, zsc.getAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        (mimeData.uploads = new ArrayList<Upload>(1)).add(up);
        try {
            // if we may need to mutate the message, we can't use the "updateHeaders" hack...
            if (anySystemMutators || needCalendarSentByFixup) {
                MimeMessage mm = new MimeMessage(JMSession.getSession(), up.getInputStream());
                if (anySystemMutators)
                    return mm;

                OutlookICalendarFixupMimeVisitor.ICalendarModificationCallback callback = new OutlookICalendarFixupMimeVisitor.ICalendarModificationCallback();
                MimeVisitor mv = new OutlookICalendarFixupMimeVisitor(getRequestedAccount(zsc), getRequestedMailbox(zsc)).setCallback(callback);
                try {
                    mv.accept(mm);
                } catch (MessagingException e) { }
                if (callback.wouldCauseModification())
                    return mm;
            }

            // ... but in general, for most installs this is safe
            return new MimeMessage(JMSession.getSession(), up.getInputStream()) {
                @Override protected void updateHeaders() throws MessagingException {
                    setHeader("MIME-Version", "1.0");  if (getMessageID() == null) updateMessageID();
                }
            };
        } catch (MessagingException e) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException when parsing upload", e);
        }
    }


    private static final Map<Long, List<Pair<String, ItemId>>> sSentTokens = new HashMap<Long, List<Pair<String, ItemId>>>(100);
    private static final int MAX_SEND_UID_CACHE = 5;

    private static Pair<SendState, Pair<String, ItemId>> findPendingSend(Long mailboxId, String sendUid) {
        SendState state = SendState.NEW;
        Pair<String, ItemId> sendRecord = null;

        synchronized (sSentTokens) {
            List<Pair<String, ItemId>> sendData = sSentTokens.get(mailboxId);
            if (sendData == null)
                sSentTokens.put(mailboxId, sendData = new ArrayList<Pair<String, ItemId>>(MAX_SEND_UID_CACHE));

            for (Pair<String, ItemId> record : sendData) {
                if (record.getFirst().equals(sendUid)) {
                    if (record.getSecond() == null)
                        state = SendState.PENDING;
                    else
                        state = SendState.SENT;
                    sendRecord = record;
                    break;
                }
            }

            if (state == SendState.NEW) {
                if (sendData.size() >= MAX_SEND_UID_CACHE)
                    sendData.remove(0);
                sendRecord = new Pair<String, ItemId>(sendUid, null);
                sendData.add(sendRecord);
            }
        }

        return new Pair<SendState, Pair<String, ItemId>>(state, sendRecord);
    }

    private static void clearPendingSend(Long mailboxId, Pair<String, ItemId> sendRecord) {
        if (sendRecord != null) {
            synchronized (sSentTokens) {
                sSentTokens.get(mailboxId).remove(sendRecord);
            }
        }
    }
    
    private static void fixupICalendarFromOutlook(Mailbox ownerMbox, MimeMessage mm)
    throws ServiceException {
        MimeVisitor mv = new OutlookICalendarFixupMimeVisitor(ownerMbox.getAccount(), ownerMbox);
        try {
            mv.accept(mm);
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
        private Account mAccount;
        private Mailbox mMailbox;
        private int mMsgDepth;
        private String[] mFromEmails;
        private String mSentBy;
        private String mDefaultCharset;

        static class ICalendarModificationCallback implements MimeVisitor.ModificationCallback {
            private boolean mWouldModify;
            public boolean wouldCauseModification()  { return mWouldModify; }
            public boolean onModification()          { mWouldModify = true; return false; }
        }

        OutlookICalendarFixupMimeVisitor(Account acct, Mailbox mbox) {
            mAccount = acct;
            mMailbox = mbox;
            mMsgDepth = 0;
            mDefaultCharset = (acct == null ? null : acct.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, null));
        }

        @Override protected boolean visitMessage(MimeMessage mm, VisitPhase visitKind) throws MessagingException {
            boolean modified = false;
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

                        mFromEmails = froms;
                        mSentBy = "MAILTO:" + sender;

                        // Set Reply-To header because Outlook doesn't. (bug 19283)
                        mm.setReplyTo(new Address[] { senderAddr });
                        modified = true;
                    }
                }
            } else if (VisitPhase.VISIT_END.equals(visitKind)) {
                mMsgDepth--;
            }

            return modified;
        }

        @Override
        protected boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) {
            return false;
        }

        @Override
        protected boolean visitBodyPart(MimeBodyPart bp) throws MessagingException {
            // Ignore any forwarded message parts.
            if (mMsgDepth != 1)
                return false;

            boolean modified = false;

            String ct = Mime.getContentType(bp);
            if (MimeConstants.CT_TEXT_CALENDAR.compareToIgnoreCase(ct) != 0)
                return false;

            ZVCalendar ical;
            InputStream is = null;
            try {
                DataSource source = bp.getDataHandler().getDataSource();
                is = source.getInputStream();
                String charset = mDefaultCharset;
                String cs = Mime.getCharset(source.getContentType());
                if (cs != null)
                    charset = cs;
                ical = ZCalendarBuilder.build(is, charset);
            } catch (Exception e) {
                throw new MessagingException("Unable to parse iCalendar part: " + e.getMessage(), e);
            } finally {
                ByteUtil.closeStream(is);
            }

            String method = ical.getPropVal(ICalTok.METHOD, ICalTok.REQUEST.toString());
            boolean isReply = method.equalsIgnoreCase(ICalTok.REPLY.toString());

            if (!isReply)
                modified = fixupRequest(ical);
            else {
                try {
                    modified = fixupReply(ical);
                } catch (ServiceException e) {
                    sLog.warn("Unable perform fixup of calendar reply from Outlook for mailbox " + mMailbox.getId() +
                              "; ignoring error", e);
                }
            }

            if (modified) {
                // check to make sure that the caller's OK with altering the message
                if (mCallback != null && !mCallback.onModification())
                    return false;

                String filename = bp.getFileName();
                if (filename == null)
                    filename = "meeting.ics";
                bp.setDataHandler(new DataHandler(new CalendarDataSource(ical, "fixup", filename)));
            }

            return modified;
        }

        private boolean fixupRequest(ZVCalendar ical) {
            boolean modified = false;
            for (Iterator<ZComponent> compIter = ical.getComponentIterator(); compIter.hasNext(); ) {
                ZComponent comp = compIter.next();
                ICalTok compType = comp.getTok();
                if (!ICalTok.VEVENT.equals(compType) && !ICalTok.VTODO.equals(compType))
                    continue;

                boolean isSeries = false;
                for (Iterator<ZProperty> propIter = comp.getPropertyIterator(); propIter.hasNext(); ) {
                    ZProperty prop = propIter.next();
                    ICalTok token = prop.getToken();
                    if (token == null)
                        continue;
                    switch (token) {
                    case ORGANIZER:
                    case ATTENDEE:
                        if (mFromEmails != null && mSentBy != null) {
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
                        }
                        break;
                    case RRULE:
                        isSeries = true;
                        break;
                    }
                }
                if (isSeries) {
                    comp.addProperty(new ZProperty(ICalTok.X_ZIMBRA_DISCARD_EXCEPTIONS, true));
                    modified = true;
                }
            }
            return modified;
        }

        private boolean fixupReply(ZVCalendar ical) throws ServiceException {
            // key = UID, value = ORGANIZER
            // No need to track VEVENTS/VTODOs with same UID but different RECURRENCE-ID separately.
            // Almost all replies should contain exactly one VEVENT/VTODO.
            Map<String, ZProperty> uidToOrganizer = new HashMap<String, ZProperty>();

            // Turn things into Invite objects.  For each Invite, check if organizer fixup is necessary.
            // Needed fixups are recorded in the uidToOrganizer map.
            List<Invite> replyInvs = Invite.createFromCalendar(mAccount, null, ical, false);
            for (Invite replyInv : replyInvs) {
                ZOrganizer replyOrg = replyInv.getOrganizer();
                // If ORGANIZER property already has SENT-BY parameter, assume the client sent
                // the correct value.  No need to second guess it.
                if (replyOrg != null && replyOrg.hasSentBy())
                    continue;

                String uid = replyInv.getUid();
                synchronized (mMailbox) {
                    CalendarItem calItem = mMailbox.getCalendarItemByUid(uid);
                    if (calItem != null) {
                        RecurId rid = replyInv.getRecurId();
                        Invite inv = calItem.getInvite(rid);
                        if (inv == null && rid != null)  // replying to a non-exception instance
                            inv = calItem.getInvite((RecurId) null);
                        if (inv != null) {
                            ZOrganizer org = inv.getOrganizer();
                            if (org != null) {
                                ZProperty orgProp = org.toProperty();
                                if (replyOrg == null) {
                                    // Looks like Outlook 2007.  It has a habit of dropping ORGANIZER entirely.
                                    uidToOrganizer.put(uid, orgProp);
                                } else {
                                    // Is it Outlook 2003?  Outlook 2003 will either leave out SENT-BY, or show
                                    // the wrong organizer.
                                    String replyOrgAddr = replyOrg.getAddress();
                                    String orgAddr = org.getAddress();
                                    if (org.hasSentBy() ||
                                        (orgAddr != null && !orgAddr.equalsIgnoreCase(replyOrgAddr))) {
                                        uidToOrganizer.put(uid, orgProp);
                                    }
                                }
                                // Else, original organizer doesn't have SENT-BY and its address matches
                                // the address in reply's organizer.  We already know reply organizer didn't
                                // have SENT-BY.  This means the ORGANIZER line in the reply was already correct.
                            }
                        }
                    }
                }
            }

            if (uidToOrganizer.isEmpty())
                return false;  // We're not making any changes.

            // Now go through the components again, this time as ZComponents.  Fixup as necessary.
            for (Iterator<ZComponent> compIter = ical.getComponentIterator(); compIter.hasNext(); ) {
                ZComponent comp = compIter.next();
                ICalTok compType = comp.getTok();
                if (!ICalTok.VEVENT.equals(compType) && !ICalTok.VTODO.equals(compType))
                    continue;

                String uid = comp.getPropVal(ICalTok.UID, null);
                ZProperty fixedOrganizer = uidToOrganizer.get(uid);
                if (fixedOrganizer == null)
                    continue;

                // Remove any existing ORGANIZER property.
                for (Iterator<ZProperty> propIter = comp.getPropertyIterator(); propIter.hasNext(); ) {
                    ZProperty prop = propIter.next();
                    ICalTok token = prop.getToken();
                    if (ICalTok.ORGANIZER.equals(token)) {
                        propIter.remove();
                        break;
                    }
                }

                // Put the correct organizer.
                comp.addProperty(fixedOrganizer);
                ZimbraLog.calendar.info("Fixed up ORGANIZER in a REPLY from ZCO");
            }
            return true;
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
