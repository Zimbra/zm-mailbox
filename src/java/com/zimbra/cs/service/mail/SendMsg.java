/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 17, 2004
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
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
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.mail.ParseMimeMessage.MimeMessageData;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;


/**
 * @author tim
 *
 * Process the <SendMsg> request from the client and send an email message.
 */
public class SendMsg extends MailDocumentHandler {

    private static Log sLog = LogFactory.getLog(SendMsg.class);

    private enum SendState { NEW, SENT, PENDING };

    private static final long MAX_IN_FLIGHT_DELAY_MSECS = 4 * Constants.MILLIS_PER_SECOND;
    private static final long RETRY_CHECK_PERIOD_MSECS = 500;

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        sLog.info("<SendMsg> " + zsc.toString());

        // <m>
        Element msgElem = request.getElement(MailConstants.E_MSG);

        // check to see whether the entire message has been uploaded under separate cover
        String attachId = msgElem.getAttribute(MailConstants.A_ATTACHMENT_ID, null);

        boolean needCalendarSentByFixup = request.getAttributeBool(MailConstants.A_NEED_CALENDAR_SENTBY_FIXUP, false);
        boolean noSaveToSent = request.getAttributeBool(MailConstants.A_NO_SAVE_TO_SENT, false);

        int origId = (int) msgElem.getAttributeLong(MailConstants.A_ORIG_ID, 0);
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
                    mm = parseUploadedMessage(zsc, attachId, mimeData);
                } else {
                    mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, mbox, msgElem, null, mimeData);
                }

                savedMsgId = doSendMessage(octxt, mbox, mm, mimeData.newContacts, mimeData.uploads,origId,
                    replyType, identityId, noSaveToSent, false, needCalendarSentByFixup);

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
        if (savedMsgId != null)
            respElement.addAttribute(MailConstants.A_ID, ifmt.formatItemId(savedMsgId));
        return response;
    }
    
    public static ItemId doSendMessage(OperationContext oc, Mailbox mbox,
        MimeMessage mm, List<InternetAddress> newContacts,  List<Upload> uploads,
        int origMsgId, String replyType, String identityId, boolean noSaveToSent,
        boolean ignoreFailedAddresses, boolean needCalendarSentByFixup) throws ServiceException {
        
        if (needCalendarSentByFixup)
            fixupICalendarFromOutlook(mbox, mm);

        if (noSaveToSent)
            return mbox.getMailSender().sendMimeMessage(oc, mbox, false, mm, newContacts, uploads,
                                                          origMsgId, replyType, null,
                                                          ignoreFailedAddresses, false);
        else
            return mbox.getMailSender().sendMimeMessage(oc, mbox, mm, newContacts, uploads,
                origMsgId, replyType, identityId, ignoreFailedAddresses, false);
    }

    static MimeMessage parseUploadedMessage(ZimbraSoapContext zsc, String attachId, MimeMessageData mimeData)
    throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachId, zsc.getRawAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        (mimeData.uploads = new ArrayList<Upload>(1)).add(up);
        try {
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


    private static final Map<Integer, List<Pair<String, ItemId>>> sSentTokens = new HashMap<Integer, List<Pair<String, ItemId>>>(100);
    private static final int MAX_SEND_UID_CACHE = 5;

    private static Pair<SendState, Pair<String, ItemId>> findPendingSend(Integer mailboxId, String sendUid) {
        SendState state = SendState.NEW;
        Pair<String, ItemId> sendRecord = null;

        synchronized (sSentTokens) {
            List<Pair<String, ItemId>> sendData = sSentTokens.get(mailboxId);
            if (sendData == null)
                sSentTokens.put(mailboxId, sendData = new ArrayList<Pair<String, ItemId>>(MAX_SEND_UID_CACHE));

            for (Pair<String, ItemId> record : sendData) {
                if (record.getFirst().equals(sendUid)) {
                    if (record.getSecond() == null) {
                        state = SendState.PENDING;
                    } else {
                        state = SendState.SENT;
                    }
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

    private static void clearPendingSend(Integer mailboxId, Pair<String, ItemId> sendRecord) {
        if (sendRecord != null) {
            synchronized (sSentTokens) {
                sSentTokens.get(mailboxId).remove(sendRecord);
            }
        }
    }
    
    private static void fixupICalendarFromOutlook(Mailbox ownerMbox, MimeMessage mm)
    throws ServiceException {
        MimeVisitor mv = new OutlookICalendarFixupMimeVisitor(ownerMbox.getAccount());
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
        private boolean mNeedFixup;
        private int mMsgDepth;
        private String[] mFromEmails;
        private String mSentBy;
        private String mDefaultCharset;

        OutlookICalendarFixupMimeVisitor(Account acct) {
            mNeedFixup = false;
            mMsgDepth = 0;
            mDefaultCharset = (acct == null ? null : acct.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, null));
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
            InputStream is = null;
            try {
                DataSource source = bp.getDataHandler().getDataSource();
                Reader reader = Mime.getTextReader(is = source.getInputStream(), source.getContentType(), mDefaultCharset);
                ical = ZCalendarBuilder.build(reader);
            } catch (Exception e) {
                throw new MessagingException("Unable to parse iCalendar part: " + e.getMessage(), e);
            } finally {
                ByteUtil.closeStream(is);
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
