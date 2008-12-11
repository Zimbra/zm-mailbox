/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;


public class ModifyCalendarItem extends CalendarRequest {

    // very simple: generate a new UID and send a REQUEST
    protected class ModifyCalendarItemParser extends ParseMimeMessage.InviteParser {
        protected Mailbox mmbox;
        protected Invite mInv;
        protected Invite mSeriesInv;
        
        ModifyCalendarItemParser(Mailbox mbox, Invite inv, Invite seriesInv) {
            mmbox = mbox;
            mInv = inv;
            mSeriesInv = seriesInv;
        }
        
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext lc, OperationContext octxt, Account account, Element inviteElem)
        throws ServiceException {
            List<ZAttendee> atsToCancel = new ArrayList<ZAttendee>();

            ParseMimeMessage.InviteParserResult toRet =
                CalendarUtils.parseInviteForModify(account, getItemType(), inviteElem, mInv, mSeriesInv, atsToCancel, !mInv.hasRecurId());

            // send cancellations to any invitees who have been removed...
            if (atsToCancel.size() > 0)
                updateRemovedInvitees(lc, octxt, account, mmbox, mInv.getCalendarItem(), mInv, atsToCancel);
            
            return toRet;
        }
    };

    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        // proxy handling

        Element msgElem = request.getElement(MailConstants.E_MSG);
        String folderStr = msgElem.getAttribute(MailConstants.A_FOLDER, null);
        ItemId iid = new ItemId(request.getAttribute(MailConstants.A_ID), zsc);
        if (!iid.belongsTo(acct)) {
            // Proxy it.
            if (folderStr != null) {
                // make sure that the folder ID is fully qualified
                ItemId folderFQ = new ItemId(folderStr, zsc);
                msgElem.addAttribute(MailConstants.A_FOLDER, folderFQ.toString());
            }
            return proxyRequest(request, context, iid.getAccountId());
        }

        // Check if moving to a different mailbox.
        boolean isInterMboxMove = false;
        ItemId iidFolder = null;
        if (folderStr != null) {
            iidFolder = new ItemId(folderStr, zsc);
            isInterMboxMove = !iidFolder.belongsTo(mbox);
        }
        
        Element response = getResponseElement(zsc);
        int compNum = (int) request.getAttributeLong(MailConstants.A_CAL_COMP, 0);
        synchronized(mbox) {
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId());
            if (calItem == null) {
                throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
            }
            Invite inv = calItem.getInvite(iid.getSubpartId(), compNum);
            if (inv == null) {
                throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
            }
            Invite seriesInv = calItem.getDefaultInviteOrNull();
            int folderId = calItem.getFolderId();
            if (!isInterMboxMove && iidFolder != null)
                folderId = iidFolder.getId();
            modifyCalendarItem(zsc, octxt, request, acct, mbox, folderId, calItem, inv, seriesInv,
                               response, isInterMboxMove);
        }

        // Inter-mailbox move if necessary.
        if (isInterMboxMove) {
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId());
            List<Integer> ids = new ArrayList<Integer>(1);
            ids.add(calItem.getId());
            ItemActionHelper.MOVE(octxt, mbox, zsc.getResponseProtocol(), ids, calItem.getType(), null, iidFolder);
        }

        return response;
    }

    private Element modifyCalendarItem(
            ZimbraSoapContext zsc, OperationContext octxt, Element request,
            Account acct, Mailbox mbox, int folderId,
            CalendarItem calItem, Invite inv, Invite seriesInv, Element response, boolean isInterMboxMove)
    throws ServiceException {
        // <M>
        Element msgElem = request.getElement(MailConstants.E_MSG);
        
        ModifyCalendarItemParser parser = new ModifyCalendarItemParser(mbox, inv, seriesInv);
        
        CalSendData dat = handleMsgElement(zsc, octxt, msgElem, acct, mbox, parser);
        dat.mDontNotifyAttendees = isInterMboxMove;

        // If we are sending this update to other people, then we MUST be the organizer!
        if (!inv.isOrganizer()) {
            try {
                Address[] rcpts = dat.mMm.getAllRecipients();
                if (rcpts != null && rcpts.length > 0) {
                    throw MailServiceException.MUST_BE_ORGANIZER("ModifyCalendarItem");
                }
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
            }
        }

        sendCalendarMessage(zsc, octxt, folderId, acct, mbox, dat, response, false);

        return response;
    }
}
