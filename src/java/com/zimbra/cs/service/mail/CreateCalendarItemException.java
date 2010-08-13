/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;


public class CreateCalendarItemException extends CalendarRequest {

    protected class CreateCalendarItemExceptionInviteParser extends ParseMimeMessage.InviteParser
    {
        private String mUid;
        private Invite mDefaultInvite;

        CreateCalendarItemExceptionInviteParser(String uid, Invite defaultInvite) {
            mUid = uid;
            mDefaultInvite = defaultInvite;
        }

        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext zsc, OperationContext octxt, Account account, Element inviteElem)
        throws ServiceException {
            ParseMimeMessage.InviteParserResult toRet =
                CalendarUtils.parseInviteForCreateException(
                        account, getItemType(), inviteElem,
                        mDefaultInvite.getTimeZoneMap(), mUid, mDefaultInvite);

            // Send cancellations to any attendees who have been removed.
            List<ZAttendee> removedAttendees = CalendarUtils.getRemovedAttendees(mDefaultInvite, toRet.mInvite, true);
            if (removedAttendees.size() > 0)
                updateRemovedInvitees(zsc, octxt, account,
                                      mDefaultInvite.getCalendarItem().getMailbox(), mDefaultInvite.getCalendarItem(), toRet.mInvite, removedAttendees);

            return toRet;
        }
    };

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

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

        int compNum = (int) request.getAttributeLong(MailConstants.E_INVITE_COMPONENT);

        Element response = getResponseElement(zsc);
        synchronized(mbox) {
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId()); 
            if (calItem == null)
                throw MailServiceException.NO_SUCH_CALITEM(iid.getId(), " for CreateCalendarItemExceptionRequest(" + iid + "," + compNum + ")");

            // Conflict detection.  Do it only if requested by client.  (for backward compat)
            int modSeq = (int) request.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE, 0);
            int revision = (int) request.getAttributeLong(MailConstants.A_REVISION, 0);
            if (modSeq != 0 && revision != 0 &&
                (modSeq < calItem.getModifiedSequence() || revision < calItem.getSavedSequence()))
                throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());

            Invite inv = calItem.getInvite(iid.getSubpartId(), compNum);
            if (inv == null)
                throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
            if (inv.hasRecurId())
                throw MailServiceException.INVITE_OUT_OF_DATE("Invite id=" + ifmt.formatItemId(iid) + " comp=" + compNum + " is not the default invite");
            if (!calItem.isRecurring())
                throw ServiceException.INVALID_REQUEST("CalendarItem " + calItem.getId() + " is not a recurring calendar item", null);
            
            CreateCalendarItemExceptionInviteParser parser = new CreateCalendarItemExceptionInviteParser(calItem.getUid(), inv);
            CalSendData dat = handleMsgElement(zsc, octxt, msgElem, acct, mbox, parser);
            dat.mDontNotifyAttendees = isInterMboxMove;
            
            int folderId = calItem.getFolderId();
            if (!isInterMboxMove && iidFolder != null)
                folderId = iidFolder.getId();

            // trace logging
            if (!dat.mInvite.hasRecurId())
                ZimbraLog.calendar.info("<CreateCalendarItemException> id=%d, folderId=%d, subject=\"%s\", UID=%s",
                        iid.getId(), folderId, dat.mInvite.isPublic() ? dat.mInvite.getName() : "(private)",
                        dat.mInvite.getUid());
            else
                ZimbraLog.calendar.info("<CreateCalendarItemException> id=%d, folderId=%d, subject=\"%s\", UID=%s, recurId=%s",
                        iid.getId(), folderId, dat.mInvite.isPublic() ? dat.mInvite.getName() : "(private)",
                        dat.mInvite.getUid(), dat.mInvite.getRecurId().getDtZ());

            // If we are sending this to other people, then we MUST be the organizer!
            if (!inv.isOrganizer()) {
                try {
                    Address[] rcpts = dat.mMm.getAllRecipients();
                    if (rcpts != null && rcpts.length > 0) {
                        throw MailServiceException.MUST_BE_ORGANIZER("CreateCalendarItemException");
                    }
                } catch (MessagingException e) {
                    throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
                }
            }

            sendCalendarMessage(zsc, octxt, folderId, acct, mbox, dat, response);
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
}
