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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

public class CounterCalendarItem extends CalendarRequest {

    private class InviteParser extends ParseMimeMessage.InviteParser {
        private Invite mInv;
        
        InviteParser(Invite inv) {
            mInv = inv;
        }
        
        public ParseMimeMessage.InviteParserResult parseInviteElement(
                ZimbraSoapContext lc, OperationContext octxt, Account account, Element inviteElem)
        throws ServiceException {
            return CalendarUtils.parseInviteForCounter(account, mInv, getItemType(), inviteElem);
        }
    };

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        Invite oldInv = null;

        // proxy handling
        String idStr = request.getAttribute(MailConstants.A_ID, null);
        if (idStr != null) {
            ItemId iid = new ItemId(idStr, zsc);
            if (!iid.belongsTo(acct))
                return proxyRequest(request, context, iid.getAccountId());
        
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId());
            if (calItem == null) {
                throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
            }

            // Conflict detection.  Do it only if requested by client.  (for backward compat)
            int modSeq = (int) request.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE, 0);
            int revision = (int) request.getAttributeLong(MailConstants.A_REVISION, 0);
            if (modSeq != 0 && revision != 0 &&
                    (modSeq < calItem.getModifiedSequence() || revision < calItem.getSavedSequence()))
                throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
        
            int compNum = (int) request.getAttributeLong(MailConstants.A_CAL_COMP, 0);
            if (compNum != 0) {
                oldInv = calItem.getInvite(iid.getSubpartId(), compNum);
                if (oldInv == null)
                    throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
            }
        }

        Element msgElem = request.getElement(MailConstants.E_MSG);
        InviteParser parser = new InviteParser(oldInv);
        CalSendData dat = handleMsgElement(zsc, octxt, msgElem, acct, mbox, parser);

        mbox.getMailSender().sendMimeMessage(octxt, mbox, dat.mMm);
        Element response = getResponseElement(zsc);
        return response;
    }
}
