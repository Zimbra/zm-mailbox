/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Message.CalendarItemInfo;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

public class ForwardAppointmentInvite extends ForwardAppointment {

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
        MimeMessage mmFwdWrapper =
            ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, mbox, msgElem,
                null, ParseMimeMessage.NO_INV_ALLOWED_PARSER, parsedMessageData);

        Message msg = mbox.getMessageById(octxt, iid.getId());
        if (msg == null) {
            throw MailServiceException.NO_SUCH_MSG(iid.getId());
        }

        Pair<MimeMessage, MimeMessage> msgPair = getMessagePair(mbox, senderAcct, msg, mmFwdWrapper);
        forwardMessages(mbox, octxt, msgPair);

        Element response = getResponseElement(zsc);
        return response;
    }

    public static void forwardMessages(Mailbox mbox,  OperationContext octxt, Pair<MimeMessage,MimeMessage> msgPair) throws ServiceException {
        if (msgPair.getFirst() != null) {
            sendFwdMsg(octxt, mbox, msgPair.getFirst());
        }
        if (msgPair.getSecond() != null) {
            sendFwdNotifyMsg(octxt, mbox, msgPair.getSecond());
        }
    }

    public static Pair<MimeMessage, MimeMessage> getMessagePair(Mailbox mbox, Account senderAcct, Message msg, MimeMessage mmFwdWrapper ) throws ServiceException {
        Pair<MimeMessage, MimeMessage> msgPair;
        try (final MailboxLock l = mbox.lock(true)) {
            l.lock();
            MimeMessage mmInv = msg.getMimeMessage();
            List<Invite> invs = new ArrayList<Invite>();
            for (Iterator<CalendarItemInfo> iter = msg.getCalendarItemInfoIterator(); iter.hasNext(); ) {
                CalendarItemInfo cii = iter.next();
                Invite inv = cii.getInvite();
                if (inv != null) {
                    invs.add(inv);
                }
            }
            ZVCalendar cal = null;
            Invite firstInv = null;
            if (!invs.isEmpty()) {
                // Recreate the VCALENDAR from Invites.
                boolean first = true;
                for (Invite inv : invs) {
                    if (first) {
                        first = false;
                        firstInv = inv;
                        cal = inv.newToICalendar(true);
                    } else {
                        ZComponent comp = inv.newToVComponent(true, true);
                        cal.addComponent(comp);
                    }
                }
            } else {
                // If no invites found in metadata, parse from text/calendar MIME part.
                try {
                    CalPartDetectVisitor visitor = new CalPartDetectVisitor();
                    visitor.accept(mmInv);
                    MimeBodyPart calPart = visitor.getCalendarPart();
                    if (calPart != null) {
                        String ctHdr = calPart.getContentType();
                        ContentType ct = new ContentType(ctHdr);
                        String charset = ct.getParameter(MimeConstants.P_CHARSET);
                        if (charset == null || charset.length() == 0)
                            charset = MimeConstants.P_CHARSET_UTF8;
                        InputStream is = calPart.getInputStream();
                        try {
                            cal = ZCalendarBuilder.build(is, charset);
                        } finally {
                            ByteUtil.closeStream(is);
                        }
                        List<Invite> invList = Invite.createFromCalendar(senderAcct, msg.getFragment(), cal, false);
                        if (invList != null && !invList.isEmpty())
                            firstInv = invList.get(0);
                        if (firstInv == null)
                            throw ServiceException.FAILURE("Error building Invite for calendar part in message " + msg.getId(), null);
                    }
                } catch (MessagingException e) {
                    throw ServiceException.FAILURE("Error getting calendar part in message " + msg.getId(), null);
                } catch (IOException e) {
                    throw ServiceException.FAILURE("Error getting calendar part in message " +  msg.getId(), null);
                }
            }

            msgPair = getInstanceFwdMsg(senderAcct, firstInv, cal, mmInv, mmFwdWrapper);

        }
        return msgPair;
    }

    // MimeVisitor that finds text/calendar part.
    private static class CalPartDetectVisitor extends MimeVisitor {
        private MimeBodyPart mCalPart;

        public CalPartDetectVisitor() {
        }

        public MimeBodyPart getCalendarPart() { return mCalPart; }

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
            if (mCalPart == null && matchingType(bp, MimeConstants.CT_TEXT_CALENDAR))
                mCalPart = bp;
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
}
