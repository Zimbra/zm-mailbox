/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Message.CalendarItemInfo;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class ForwardAppointmentInvite extends ForwardAppointment {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account senderAcct = getAuthenticatedAccount(zsc);
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

        MimeMessage mmFwd;
        synchronized(mbox) {
            Message msg = mbox.getMessageById(octxt, iid.getId());
            if (msg == null)
                throw MailServiceException.NO_SUCH_MSG(iid.getId());

            MimeMessage mmInv = msg.getMimeMessage();
            List<Invite> invs = new ArrayList<Invite>();
            for (Iterator<CalendarItemInfo> iter = msg.getCalendarItemInfoIterator(); iter.hasNext(); ) {
                CalendarItemInfo cii = iter.next();
                Invite inv = cii.getInvite();
                if (inv != null)
                    invs.add(inv);
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
                throw ServiceException.FAILURE("No invite data found in message " + iid.getId(), null);
                // TODO: If no invites in metadata, parse from text/calendar MIME part.
            }

            // Set SENT-BY to sender's email address.  Required by Outlook.
            String sentByAddr = AccountUtil.getFriendlyEmailAddress(senderAcct).getAddress();
            Address[] rcpts = null;
            try {
                rcpts = mmFwdWrapper.getAllRecipients();
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("error getting recipients", e);
            }
            setSentByAndAttendees(cal, sentByAddr, rcpts);

            mmFwd = getInstanceFwdMsg(senderAcct, firstInv, mmInv, mmFwdWrapper);
        }
        sendFwdMsg(octxt, mbox, mmFwd);

        Element response = getResponseElement(zsc);
        return response;
    }
}
