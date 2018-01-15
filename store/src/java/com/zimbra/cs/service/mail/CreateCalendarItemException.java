/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;

import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;


public class CreateCalendarItemException extends CalendarRequest {

    protected class CreateCalendarItemExceptionInviteParser extends ParseMimeMessage.InviteParser {
        private String mUid;
        private Invite mDefaultInvite;
        private MailSendQueue sendQueue;

        CreateCalendarItemExceptionInviteParser(String uid, Invite defaultInvite, MailSendQueue sendQueue) {
            mUid = uid;
            mDefaultInvite = defaultInvite;
            this.sendQueue = sendQueue;
        }

        @Override
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext zsc, OperationContext octxt,
                Account account, Element inviteElem) throws ServiceException {
            ParseMimeMessage.InviteParserResult toRet = CalendarUtils.parseInviteForCreateException(
                    account, getItemType(), inviteElem, (mDefaultInvite.getTimeZoneMap() != null ) ? 
                            mDefaultInvite.getTimeZoneMap().clone() : null, mUid, mDefaultInvite);

            // Send cancellations to any attendees who have been removed.
            List<ZAttendee> removedAttendees = CalendarUtils.getRemovedAttendees(
                    mDefaultInvite.getAttendees(), toRet.mInvite.getAttendees(), true, account);
            if (removedAttendees.size() > 0) {
                notifyRemovedAttendees(zsc, octxt, account,
                        mDefaultInvite.getCalendarItem().getMailbox(), mDefaultInvite.getCalendarItem(), toRet.mInvite,
                        removedAttendees, sendQueue);
            }
            return toRet;
        }
    }

    @Override
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

        MailSendQueue sendQueue = new MailSendQueue();
        Element response = getResponseElement(zsc);
        try  {
            try (final MailboxLock l = mbox.lock(true)) {
                l.lock();
                CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId());
                if (calItem == null)
                    throw MailServiceException.NO_SUCH_CALITEM(iid.getId(), " for CreateCalendarItemExceptionRequest(" + iid + "," + compNum + ")");

                // Reject the request if calendar item is under trash or is being moved to trash.
                if (calItem.inTrash())
                    throw ServiceException.INVALID_REQUEST("cannot modify a calendar item under trash", null);
                if (!isInterMboxMove && iidFolder != null) {
                    if (iidFolder.getId() != calItem.getFolderId()) {
                        Folder destFolder = mbox.getFolderById(octxt, iidFolder.getId());
                        if (destFolder.inTrash())
                            throw ServiceException.INVALID_REQUEST("cannot combine with a move to trash", null);
                    }
                }

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

                CreateCalendarItemExceptionInviteParser parser = new CreateCalendarItemExceptionInviteParser(calItem.getUid(), inv, sendQueue);
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

                boolean hasRecipients;
                try {
                    Address[] rcpts = dat.mMm.getAllRecipients();
                    hasRecipients = rcpts != null && rcpts.length > 0;
                } catch (MessagingException e) {
                    throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
                }
                // If we are sending this to other people, then we MUST be the organizer!
                if (!dat.mInvite.isOrganizer() && hasRecipients)
                    throw MailServiceException.MUST_BE_ORGANIZER("CreateCalendarItemException");

                if (!dat.mInvite.isOrganizer()) {
                    // neverSent is always false for attendee users.
                    dat.mInvite.setNeverSent(false);
                } else if (!dat.mInvite.hasOtherAttendees()) {
                    // neverSent is always false for appointments without attendees.
                    dat.mInvite.setNeverSent(false);
                } else if (hasRecipients) {
                    // neverSent is set to false when attendees are notified.
                    dat.mInvite.setNeverSent(false);
                } else {
                    // This is the case of organizer saving an invite with attendees, but without sending the notification.
                    // Set neverSent to false, but only if it isn't already set to true on the series.
                    // !series.isNeverSent() ? false : true ==> series.isNeverSent()
                    dat.mInvite.setNeverSent(inv.isNeverSent());
                }
                boolean forceSend = request.getAttributeBool(MailConstants.A_CAL_FORCESEND, true);
                sendCalendarMessage(zsc, octxt, folderId, acct, mbox, dat, response, true, forceSend, sendQueue);
                boolean echo = request.getAttributeBool(MailConstants.A_CAL_ECHO, false);
                if (echo && dat.mAddInvData != null) {
                    int maxSize = (int) request.getAttributeLong(MailConstants.A_MAX_INLINED_LENGTH, 0);
                    boolean wantHTML = request.getAttributeBool(MailConstants.A_WANT_HTML, false);
                    boolean neuter = request.getAttributeBool(MailConstants.A_NEUTER, true);
                    echoAddedInvite(response, ifmt, octxt, mbox, dat.mAddInvData, maxSize, wantHTML, neuter);
                }
            }
        } finally {
            sendQueue.send();
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
