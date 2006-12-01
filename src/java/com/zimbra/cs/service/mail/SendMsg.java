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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 17, 2004
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.operation.SendMsgOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.mail.ParseMimeMessage.MimeMessageData;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.Element;
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
        OperationContext octxt = zsc.getOperationContext();
        Session session = getSession(context);

        sLog.info("<SendMsg> " + zsc.toString());

        // <m>
        Element msgElem = request.getElement(MailService.E_MSG);

        // check to see whether the entire message has been uploaded under separate cover
        String attachId = msgElem.getAttribute(MailService.A_ATTACHMENT_ID, null);

        boolean needCalendarSentByFixup = request.getAttributeBool(MailService.A_NEED_CALENDAR_SENTBY_FIXUP, false);
        boolean noSaveToSent = request.getAttributeBool(MailService.A_NO_SAVE_TO_SENT, false);

        int origId = (int) msgElem.getAttributeLong(MailService.A_ORIG_ID, 0);
        String replyType = msgElem.getAttribute(MailService.A_REPLY_TYPE, MailSender.MSGTYPE_REPLY);
        String identityId = msgElem.getAttribute(MailService.A_IDENTITY_ID, null);


        SendState state = SendState.NEW;
        int savedMsgId = -1;
        Pair<String, Integer> sendRecord = null;

        // get the "send uid" and check that this isn't a retry of a pending send
        String sendUid = request.getAttribute(MailService.A_SEND_UID, null);
        if (sendUid != null) {
            long delay = MAX_IN_FLIGHT_DELAY_MSECS;
            do {
                if (state == SendState.PENDING) {
                    try {
                        delay -= RETRY_CHECK_PERIOD_MSECS;  Thread.sleep(RETRY_CHECK_PERIOD_MSECS);
                    } catch (InterruptedException ie) { }
                }

                Pair<SendState, Pair<String, Integer>> result = findPendingSend(mbox.getId(), sendUid);
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
                    mm = ParseMimeMessage.parseMimeMsgSoap(zsc, mbox, msgElem, null, mimeData);
                }

                // send the message ...
                SendMsgOperation op = new SendMsgOperation(session, octxt, mbox, Requester.SOAP,
                        mm, mimeData.newContacts, mimeData.uploads, origId, replyType,
                        identityId, noSaveToSent, false, needCalendarSentByFixup);
                op.schedule();
                savedMsgId = op.getMsgId();

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

        Element response = zsc.createElement(MailService.SEND_MSG_RESPONSE);
        Element respElement = response.addElement(MailService.E_MSG);
        if (savedMsgId > 0)
            respElement.addAttribute(MailService.A_ID, zsc.formatItemId(savedMsgId));
        return response;
    }

    static MimeMessage parseUploadedMessage(ZimbraSoapContext zsc, String attachId, MimeMessageData mimeData)
    throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachId, zsc.getRawAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        (mimeData.uploads = new ArrayList<Upload>(1)).add(up);
        try {
            return new Mime.FixedMimeMessage(JMSession.getSession(), up.getInputStream());
        } catch (MessagingException e) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException when parsing upload", e);
        }
    }


    private static final Map<Integer, List<Pair<String, Integer>>> sSentTokens = new HashMap<Integer, List<Pair<String, Integer>>>(100);
    private static final int MAX_SEND_UID_CACHE = 5;

    private Pair<SendState, Pair<String, Integer>> findPendingSend(Integer mailboxId, String sendUid) {
        SendState state = SendState.NEW;
        Pair<String, Integer> sendRecord = null;

        synchronized (sSentTokens) {
            List<Pair<String, Integer>> sendData = sSentTokens.get(mailboxId);
            if (sendData == null)
                sSentTokens.put(mailboxId, sendData = new ArrayList<Pair<String, Integer>>(MAX_SEND_UID_CACHE));

            for (Pair<String, Integer> record : sendData) {
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
                sendRecord = new Pair<String, Integer>(sendUid, null);
                sendData.add(sendRecord);
            }
        }

        return new Pair<SendState, Pair<String, Integer>>(state, sendRecord);
    }

    private void clearPendingSend(Integer mailboxId, Pair<String, Integer> sendRecord) {
        if (sendRecord != null) {
            synchronized (sSentTokens) {
                sSentTokens.get(mailboxId).remove(sendRecord);
            }
        }
    }
}
