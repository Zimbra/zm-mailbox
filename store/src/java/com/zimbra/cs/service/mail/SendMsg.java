	/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
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

import org.dom4j.QName;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarDataSource;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeProcessor;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.mail.ParseMimeMessage.MimeMessageData;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.type.MsgContent;

/**
 * Process the {@code <SendMsg>} request from the client and send an email message.
 *
 * @since Sep 17, 2004
 */
public class SendMsg extends MailDocumentHandler {
    private static final Log LOG = LogFactory.getLog(SendMsg.class);

    private enum SendState { NEW, SENT, PENDING };

    private static final long MAX_IN_FLIGHT_DELAY_MSECS = 4 * Constants.MILLIS_PER_SECOND;
    private static final long RETRY_CHECK_PERIOD_MSECS = 500;
    protected static MimeProcessor processor = null;

    private static final ItemId NO_MESSAGE_SAVED_TO_SENT = new ItemId((String) null, -1);

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        return handle(request, context, MailConstants.SEND_MSG_RESPONSE);
    }

    public Element handle(Element request, Map<String, Object> context, QName respQname) throws ServiceException {
           try {
               ZimbraSoapContext zsc = getZimbraSoapContext(context);
               Mailbox mbox = getRequestedMailbox(zsc);
               AccountUtil.checkQuotaWhenSendMail(mbox);
               AuthToken authToken = zsc.getAuthToken();
               Account authAcct = getAuthenticatedAccount(zsc);

               OperationContext octxt = getOperationContext(zsc, context);
               ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

               // <m>
               Element msgElem = request.getElement(MailConstants.E_MSG);

               // check to see whether the entire message has been uploaded under separate cover
               String attachId = msgElem.getAttribute(MailConstants.A_ATTACHMENT_ID, null);

               boolean needCalendarSentByFixup = request.getAttributeBool(MailConstants.A_NEED_CALENDAR_SENTBY_FIXUP, false);
               boolean isCalendarForward = request.getAttributeBool(MailConstants.A_IS_CALENDAR_FORWARD, false);
               boolean noSaveToSent = request.getAttributeBool(MailConstants.A_NO_SAVE_TO_SENT, false);
               boolean fetchSavedMsg = request.getAttributeBool(MailConstants.A_FETCH_SAVED_MSG, false);

               String origId = msgElem.getAttribute(MailConstants.A_ORIG_ID, null);
               ItemId iidOrigId = origId == null ? null : new ItemId(origId, zsc);
               String replyType = msgElem.getAttribute(MailConstants.A_REPLY_TYPE, MailSender.MSGTYPE_REPLY);
               String identityId = msgElem.getAttribute(MailConstants.A_IDENTITY_ID, null);
               String dataSourceId = msgElem.getAttribute(MailConstants.A_DATASOURCE_ID, null);
               String draftId = msgElem.getAttribute(MailConstants.A_DRAFT_ID, null);
               boolean sendFromDraft = msgElem.getAttributeBool(MailConstants.A_SEND_FROM_DRAFT, false);

               Account delegatedAccount = authAcct;
               Mailbox  delegatedMailbox = null;
               if (!StringUtil.isNullOrEmpty(identityId) && !identityId.equals(authAcct.getId()) ) {

                   Account acctReq = AccountUtil.getRequestedAccount(identityId, authAcct);
                   if (acctReq != null) {
                       AccessManager accessMgr = AccessManager.getInstance();
                       if (accessMgr.canDo(authAcct, acctReq, Rights.User.R_sendAs, false) ||
                               accessMgr.canDo(authAcct, acctReq, Rights.User.R_sendOnBehalfOf, false)) {
                           delegatedAccount = acctReq;
                           delegatedMailbox = MailboxManager.getInstance().getMailboxByAccountId(delegatedAccount.getId());
                       }
                   }
               }

               ItemId iidDraft = draftId == null ? null : new ItemId(draftId, zsc);
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
                               delay -= RETRY_CHECK_PERIOD_MSECS;
                               Thread.sleep(RETRY_CHECK_PERIOD_MSECS);
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
                   MimeMessageData mimeData = new MimeMessageData();
                   try {
                       // holds return data about the MimeMessage
                       MimeMessage mm;
                       if (attachId != null) {
                           mm = parseUploadedMessage(zsc, attachId, mimeData, needCalendarSentByFixup);
                       } else if (iidDraft != null && sendFromDraft) {
                           Message msg = mbox.getMessageById(octxt, iidDraft.getId());
                           mm = msg.getMimeMessage(false);
                       } else {
                           Mailbox loggedUserMbox = MailboxManager.getInstance().getMailboxByAccountId(authAcct.getId());
                           mm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, loggedUserMbox, msgElem, null, mimeData);
                       }

                       if  (delegatedMailbox  != null) {
                           savedMsgId = doSendMessage(octxt, delegatedMailbox, mm, mimeData.uploads, iidOrigId, replyType, identityId,
                               dataSourceId, noSaveToSent, needCalendarSentByFixup, isCalendarForward);
                       } else  {
                           savedMsgId = doSendMessage(octxt, mbox, mm, mimeData.uploads, iidOrigId, replyType, identityId,
                               dataSourceId, noSaveToSent, needCalendarSentByFixup, isCalendarForward);
                       }

                       // (need to make sure that *something* gets recorded, because caching
                       //   a null ItemId makes the send appear to still be PENDING)
                       if (savedMsgId == null) {
                           savedMsgId = NO_MESSAGE_SAVED_TO_SENT;
                       }

                       // and record it in the table in case the client retries the send
                       if (sendRecord != null) {
                           sendRecord.setSecond(savedMsgId);
                       }
                   } catch (ServiceException e) {
                       clearPendingSend(mbox.getId(), sendRecord);
                       throw e;
                   } catch (RuntimeException re) {
                       clearPendingSend(mbox.getId(), sendRecord);
                       throw re;
                   } finally {
                       // purge the messages fetched from other servers.
                       if (mimeData.fetches != null) {
                           FileUploadServlet.deleteUploads(mimeData.fetches);
                       }
                   }
               }

               if (iidDraft != null) {
                   deleteDraft(iidDraft, octxt, mbox, zsc);
               }

               Element response = zsc.createElement(respQname);
               if (savedMsgId != null && savedMsgId != NO_MESSAGE_SAVED_TO_SENT && savedMsgId.getId() > 0) {
                   if (fetchSavedMsg) {
                       Message msg = GetMsg.getMsg(octxt, mbox, savedMsgId, false);
                       ToXML.encodeMessageAsMP(response, ifmt, octxt, msg, null, 0, true, true, null, false, false,
                               LC.mime_encode_missing_blob.booleanValue(), MsgContent.both);
                   } else {
                       Element respElement = response.addElement(MailConstants.E_MSG);
                       respElement.addAttribute(MailConstants.A_ID, ifmt.formatItemId(savedMsgId));
                   }
               } else {
                   response.addElement(MailConstants.E_MSG);
               }
               return response;
           } finally {
               processor = null;
           }
    }

    public static ItemId doSendMessage(OperationContext oc, Mailbox mbox, MimeMessage mm, List<Upload> uploads,
            ItemId origMsgId, String replyType, String identityId, String dataSourceId, boolean noSaveToSent,
            boolean needCalendarSentByFixup, boolean isCalendarForward)
    throws ServiceException {

        boolean isCalendarMessage = false;
        ItemId id;
        OutlookICalendarFixupMimeVisitor mv = null;
        if (needCalendarSentByFixup || isCalendarForward) {
            mv = new OutlookICalendarFixupMimeVisitor(mbox.getAccount(), mbox)
                                                                    .needFixup(needCalendarSentByFixup)
                                                                    .setIsCalendarForward(isCalendarForward);
            try {
                mv.accept(mm);
            } catch (MessagingException e) {
                throw ServiceException.PARSE_ERROR("Error while fixing up SendMsg for SENT-BY", e);
            }
            isCalendarMessage = mv.isCalendarMessage();
        }

        MailSender sender;
        if (dataSourceId == null) {
            sender = isCalendarMessage ? CalendarMailSender.getCalendarMailSender(mbox) : mbox.getMailSender();
            if (noSaveToSent) {
                id = sender.sendMimeMessage(oc, mbox, false, mm, uploads, origMsgId, replyType, null, false, processor);
            } else {
                id = sender.sendMimeMessage(oc, mbox, mm, uploads, origMsgId, replyType, identityId, false, processor);
            }
        } else {
            com.zimbra.cs.account.DataSource dataSource = mbox.getAccount().getDataSourceById(dataSourceId);
            if (dataSource == null) {
                throw ServiceException.INVALID_REQUEST("No data source with id " + dataSourceId, null);
            }
            if (!dataSource.isSmtpEnabled()) {
                throw ServiceException.INVALID_REQUEST("Data source SMTP is not enabled", null);
            }
            sender = mbox.getDataSourceMailSender(dataSource, isCalendarMessage);
            id = sender.sendDataSourceMimeMessage(oc, mbox, mm, uploads, origMsgId, replyType);
        }
        // Send Calendar Forward Invitation notification if applicable
        try {
            if (isCalendarMessage && isCalendarForward && mv.getOriginalInvite() != null) {
                ZOrganizer org = mv.getOriginalInvite().getOrganizer();
                if (org != null) {
                    String orgAddress = org.getAddress();
                    Account orgAccount = Provisioning.getInstance().getAccountByName(orgAddress);
                    if (orgAccount != null && !orgAccount.getId().equals(mbox.getAccount().getId())) {
                        MimeMessage notifyMimeMsg = CalendarMailSender.createForwardNotifyMessage(mbox.getAccount(),
                                orgAccount, orgAddress, mm.getAllRecipients(), mv.getOriginalInvite());
                        CalendarMailSender.sendPartial(oc, mbox, notifyMimeMsg, null, null, null, null, false, true);
                    }
                }
            }
        } catch (Exception e) {
            ZimbraLog.soap.warn("Ignoring error while sending Calendar Invitation Forward Notification", e);
        }

        return id;
    }

    static MimeMessage parseUploadedMessage(ZimbraSoapContext zsc, String attachId, MimeMessageData mimeData) throws ServiceException {
        return parseUploadedMessage(zsc, attachId, mimeData, false);
    }

    static MimeMessage parseUploadedMessage(ZimbraSoapContext zsc, String attachId, MimeMessageData mimeData,
                                            boolean needCalendarSentByFixup)
    throws ServiceException {
        boolean anySystemMutators = MimeVisitor.anyMutatorsRegistered();

        Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachId, zsc.getAuthToken());
        if (up == null) {
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        }
        (mimeData.uploads = new ArrayList<Upload>(1)).add(up);
        try {
            // if we may need to mutate the message, we can't use the "updateHeaders" hack...
            if (anySystemMutators || needCalendarSentByFixup) {
                MimeMessage mm = new ZMimeMessage(JMSession.getSession(), up.getInputStream());
                if (anySystemMutators) {
                    return mm;
                }

                OutlookICalendarFixupMimeVisitor.ICalendarModificationCallback callback = new OutlookICalendarFixupMimeVisitor.ICalendarModificationCallback();
                MimeVisitor mv = new OutlookICalendarFixupMimeVisitor(getRequestedAccount(zsc), getRequestedMailbox(zsc)).needFixup(true).setCallback(callback);
                try {
                    mv.accept(mm);
                } catch (MessagingException e) { }
                if (callback.wouldCauseModification()) {
                    return mm;
                }
            }

            // ... but in general, for most installs this is safe
            return new ZMimeMessage(JMSession.getSession(), up.getInputStream()) {
                @Override
                protected void updateHeaders() throws MessagingException {
                    setHeader("MIME-Version", "1.0");
                    if (getMessageID() == null)
                        updateMessageID();
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
            if (sendData == null) {
                sSentTokens.put(mailboxId, sendData = new ArrayList<Pair<String, ItemId>>(MAX_SEND_UID_CACHE));
            }

            for (Pair<String, ItemId> record : sendData) {
                if (record.getFirst().equals(sendUid)) {
                    state = record.getSecond() == null ? SendState.PENDING : SendState.SENT;
                    sendRecord = record;
                    break;
                }
            }

            if (state == SendState.NEW) {
                if (sendData.size() >= MAX_SEND_UID_CACHE) {
                    sendData.remove(0);
                }
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


    private void deleteDraft(ItemId iidDraft, OperationContext octxt, Mailbox localMbox, ZimbraSoapContext zsc) {
        try {
            if (iidDraft.belongsTo(localMbox)) {
                localMbox.delete(octxt, iidDraft.getId(), MailItem.Type.MESSAGE);
            } else if (iidDraft.isLocal()) {
                Mailbox ownerMbox = MailboxManager.getInstance().getMailboxByAccountId(iidDraft.getAccountId());
                ownerMbox.delete(octxt, iidDraft.getId(), MailItem.Type.MESSAGE);
            } else {
                Account target = Provisioning.getInstance().get(Key.AccountBy.id, iidDraft.getAccountId());
                AuthToken at = AuthToken.getCsrfUnsecuredAuthToken(zsc.getAuthToken());
                ZAuthToken zat = at.getProxyAuthToken() == null ? at.toZAuthToken() : new ZAuthToken(at.getProxyAuthToken());
                ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(target));
                zoptions.setNoSession(true);
                zoptions.setTargetAccount(target.getId()).setTargetAccountBy(Key.AccountBy.id);
                ZMailbox.getMailbox(zoptions).deleteMessage(iidDraft.toString());
            }
        } catch (ServiceException e) {
            // draft delete failure mustn't affect response to SendMsg request
            ZimbraLog.soap.info("failed to delete draft after message send: %s", iidDraft);
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
        private boolean needFixup;
        private boolean isCalendarForward;
        private boolean isCalendarMessage;
        private final Account mAccount;
        private final Mailbox mMailbox;
        private int mMsgDepth;
        private String[] mFromEmails;
        private String mSentBy;
        private final String mDefaultCharset;
        private Invite origInvite = null;

        static class ICalendarModificationCallback implements MimeVisitor.ModificationCallback {
            private boolean mWouldModify;

            public boolean wouldCauseModification() {
                return mWouldModify;
            }

            @Override
            public boolean onModification() {
                mWouldModify = true;
                return false;
            }
        }

        OutlookICalendarFixupMimeVisitor(Account acct, Mailbox mbox) {
            mAccount = acct;
            mMailbox = mbox;
            mMsgDepth = 0;
            mDefaultCharset = (acct == null ? null : acct.getPrefMailDefaultCharset());
        }

        public OutlookICalendarFixupMimeVisitor needFixup(boolean fixup) {
            this.needFixup = fixup;
            return this;
        }

        public OutlookICalendarFixupMimeVisitor setIsCalendarForward(boolean forward) {
            this.isCalendarForward = forward;
            return this;
        }

        public boolean isCalendarMessage() {
            return isCalendarMessage;
        }

        public Invite getOriginalInvite() {
            return origInvite;
        }

        @Override protected boolean visitMessage(MimeMessage mm, VisitPhase visitKind) throws MessagingException {
            boolean modified = false;
            if (VisitPhase.VISIT_BEGIN.equals(visitKind)) {
                mMsgDepth++;
                if (mMsgDepth == 1 && needFixup) {
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

        @Override protected boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) {
            return false;
        }

        @Override protected boolean visitBodyPart(MimeBodyPart bp) throws MessagingException {
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
                String ctHdr = source.getContentType();
                String methodParam = new ContentType(ctHdr).getParameter("method");
                if (methodParam == null || methodParam.trim().isEmpty()) {
                    // A text/calendar part without method parameter is just an attachment of a regular email.
                    return false;
                }
                isCalendarMessage = true;

                if (!(needFixup || isCalendarForward))
                    return false;

                String charset = mDefaultCharset;
                String cs = Mime.getCharset(ctHdr);
                if (cs != null)
                    charset = cs;
                ical = ZCalendarBuilder.build(is, charset);

                if (isCalendarForward) {
                    // Populate Invite for ical
                    List<Invite> inviteList = Invite.createFromCalendar(mAccount, null, ical, false);
                    Invite defInvite = null;
                    for (Invite cur : inviteList) {
                        if (!cur.hasRecurId()) {
                            defInvite = cur;
                            break;
                        }
                        if (defInvite == null)
                            defInvite = cur;
                    }
                    origInvite = defInvite;
                }

                if (!needFixup) {
                    return false;
                }
            } catch (Exception e) {
                throw new MessagingException("Unable to parse iCalendar part: " + e.getMessage(), e);
            } finally {
                ByteUtil.closeStream(is);
            }

            String method = ical.getPropVal(ICalTok.METHOD, ICalTok.REQUEST.toString());
            boolean isReply = method.equalsIgnoreCase(ICalTok.REPLY.toString());

            if (!isReply) {
                modified = fixupRequest(ical);
            } else {
                try {
                    modified = fixupReply(ical);
                } catch (ServiceException e) {
                    LOG.warn("Unable perform fixup of calendar reply from Outlook for mailbox " + mMailbox.getId() +
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
                bp.setDataHandler(new DataHandler(new CalendarDataSource(ical, filename)));
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
                                        ZimbraLog.calendar.debug("Fixed up %s ( %s) by adding SENT-BY=%s", token, addr, mSentBy);
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
                mMailbox.lock.lock();
                try {
                    CalendarItem calItem = mMailbox.getCalendarItemByUid(null, uid);
                    if (calItem != null) {
                        RecurId rid = replyInv.getRecurId();
                        Invite inv = calItem.getInvite(rid);
                        if (inv == null && rid != null) { // replying to a non-exception instance
                            inv = calItem.getInvite((RecurId) null);
                        }
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
                } finally {
                    mMailbox.lock.release();
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
                    } else if  (ICalTok.ATTENDEE.equals(token)) {
                        if (mFromEmails != null && mSentBy != null) {
                            String addr = prop.getValue();
                            if (addressMatchesFrom(addr)) {
                                ZParameter sentBy = prop.getParameter(ICalTok.SENT_BY);
                                if (sentBy == null) {
                                    prop.addParameter(new ZParameter(ICalTok.SENT_BY, mSentBy));
                                    ZimbraLog.calendar.debug("Fixed up %s ( %s) by adding SENT-BY=%s", token, addr, mSentBy);
                                }
                            }
                        }

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
