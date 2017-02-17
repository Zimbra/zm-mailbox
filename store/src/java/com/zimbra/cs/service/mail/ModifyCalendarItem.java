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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteChanges;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;


public class ModifyCalendarItem extends CalendarRequest {

    // very simple: generate a new UID and send a REQUEST
    protected class ModifyCalendarItemParser extends ParseMimeMessage.InviteParser {
        private Invite mInv;
        private Invite mSeriesInv;
        private List<ZAttendee> mAttendeesAdded;
        private List<ZAttendee> mAttendeesCanceled;

        ModifyCalendarItemParser(Invite inv, Invite seriesInv) {
            mInv = inv;
            mSeriesInv = seriesInv;
            mAttendeesAdded = new ArrayList<ZAttendee>();
            mAttendeesCanceled = new ArrayList<ZAttendee>();
        }

        public List<ZAttendee> getAttendeesAdded() { return mAttendeesAdded; }
        public List<ZAttendee> getAttendeesCanceled() { return mAttendeesCanceled; }

        @Override
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext lc, OperationContext octxt,
                Account account, Element inviteElem) throws ServiceException {
            ParseMimeMessage.InviteParserResult toRet = CalendarUtils.parseInviteForModify(account, getItemType(),
                    inviteElem, mInv, mSeriesInv, mAttendeesAdded, mAttendeesCanceled, !mInv.hasRecurId());
            return toRet;
        }
    };

    @Override
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

        MailSendQueue sendQueue = new MailSendQueue();
        Element response = getResponseElement(zsc);
        int compNum = (int) request.getAttributeLong(MailConstants.A_CAL_COMP, 0);
        mbox.lock.lock();
        try {
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId());
            if (calItem == null) {
                throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
            }

            // Reject the request if calendar item is under trash or is being moved to trash.
            if (calItem.inTrash()) {
                throw ServiceException.INVALID_REQUEST("cannot modify a calendar item under trash", null);
            }
            if (!isInterMboxMove && iidFolder != null) {
                if (iidFolder.getId() != calItem.getFolderId()) {
                    Folder destFolder = mbox.getFolderById(octxt, iidFolder.getId());
                    if (destFolder.inTrash()) {
                        throw ServiceException.INVALID_REQUEST("cannot combine with a move to trash", null);
                    }
                }
            }

            // Conflict detection.  Do it only if requested by client.  (for backward compat)
            int modSeq = (int) request.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE, 0);
            int revision = (int) request.getAttributeLong(MailConstants.A_REVISION, 0);
            if (modSeq != 0 && revision != 0 &&
                    (modSeq < calItem.getModifiedSequence() || revision < calItem.getSavedSequence())) {
                throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
            }

            Invite inv = calItem.getInvite(iid.getSubpartId(), compNum);
            if (inv == null) {
                throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
            }
            Invite seriesInv = calItem.getDefaultInviteOrNull();
            int folderId = calItem.getFolderId();
            if (!isInterMboxMove && iidFolder != null) {
                folderId = iidFolder.getId();
            }
            modifyCalendarItem(zsc, octxt, request, acct, mbox, folderId, calItem, inv, seriesInv,
                               response, isInterMboxMove, sendQueue);
        } finally {
            mbox.lock.release();
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

    private Element modifyCalendarItem(
            ZimbraSoapContext zsc, OperationContext octxt, Element request,
            Account acct, Mailbox mbox, int folderId,
            CalendarItem calItem, Invite inv, Invite seriesInv, Element response, boolean isInterMboxMove,
            MailSendQueue sendQueue)
    throws ServiceException {
        // <M>
        Element msgElem = request.getElement(MailConstants.E_MSG);

        ModifyCalendarItemParser parser = new ModifyCalendarItemParser(inv, seriesInv);

        CalSendData dat = handleMsgElement(zsc, octxt, msgElem, acct, mbox, parser);
        dat.mDontNotifyAttendees = isInterMboxMove;

        if (!dat.mInvite.hasRecurId())
            ZimbraLog.calendar.info("<ModifyCalendarItem> id=%d, folderId=%d, subject=\"%s\", UID=%s",
                    calItem.getId(), folderId, dat.mInvite.isPublic() ? dat.mInvite.getName() : "(private)",
                    dat.mInvite.getUid());
        else
            ZimbraLog.calendar.info("<ModifyCalendarItem> id=%d, folderId=%d, subject=\"%s\", UID=%s, recurId=%s",
                    calItem.getId(), folderId, dat.mInvite.isPublic() ? dat.mInvite.getName() : "(private)",
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
            throw MailServiceException.MUST_BE_ORGANIZER("ModifyCalendarItem");

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
            assert(dat.mInvite.hasOtherAttendees() && !hasRecipients);
            if (!inv.hasOtherAttendees()) {
                // Special case of going from a personal appointment (no attendees) to a draft appointment with
                // attendees.  neverSent was false for being a personal appointment, so we need to explicitly set it to true.
                // This case is essentially identical to creating a new appointment with attendees without notification.
                dat.mInvite.setNeverSent(true);
            } else {
                // Set neverSent to false, but only if it wasn't already set to true before.
                // !inv.isNeverSent() ? false : true ==> inv.isNeverSent()
                dat.mInvite.setNeverSent(inv.isNeverSent());
            }
        }

        boolean echo = request.getAttributeBool(MailConstants.A_CAL_ECHO, false);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
        int maxSize = (int) request.getAttributeLong(MailConstants.A_MAX_INLINED_LENGTH, 0);
        boolean wantHTML = request.getAttributeBool(MailConstants.A_WANT_HTML, false);
        boolean neuter = request.getAttributeBool(MailConstants.A_NEUTER, true);
        boolean forceSend = request.getAttributeBool(MailConstants.A_CAL_FORCESEND, true);
        if (inv.isOrganizer()) {
            // Notify removed attendees before making any changes to the appointment.
            List<ZAttendee> atsCanceled = parser.getAttendeesCanceled();
            if (!inv.isNeverSent()) {  // No need to notify for a draft appointment.
                if (!atsCanceled.isEmpty()) {
                    notifyRemovedAttendees(zsc, octxt, acct, mbox, inv.getCalendarItem(), inv, atsCanceled, sendQueue);
                }
            }

            List<ZAttendee> atsAdded = parser.getAttendeesAdded();
            // Figure out if we're notifying all attendees.  Must do this before clearing recipients from dat.mMm.
            boolean notifyAllAttendees = isNotifyingAll(dat.mMm, atsAdded);
            // If notifying all the attendees update the last sequence number otherwise retain the existing value.
            if (notifyAllAttendees) {
                dat.mInvite.setLastFullSeqNo(dat.mInvite.getSeqNo());
            } else {
                dat.mInvite.setLastFullSeqNo(inv.getLastFullSeqNo());
            }
            if (inv.isRecurrence()) {
                // Clear to/cc/bcc from the MimeMessage, so that the sendCalendarMessage call only updates the organizer's
                // own appointment without notifying any attendees.  Notifications will be sent later,
                removeAllRecipients(dat.mMm);

                // If this is a change that removes exceptions, send cancel notification to attendees who are only
                // on the exception instances.  They will be removed from the appointment entirely as a result.
                // (unless they are added as new series attendees)
                if (!acct.isCalendarKeepExceptionsOnSeriesTimeChange()) {
                    InviteChanges ic = new InviteChanges(seriesInv, dat.mInvite);
                    if (ic.isExceptionRemovingChange()) {
                        long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
                        Invite[] invites = calItem.getInvites();
                        for (Invite except : invites) {
                            if (!except.isNeverSent() && except.hasRecurId() && !except.isCancel() && inviteIsAfterTime(except, now)) {
                                List<ZAttendee> toNotify = CalendarUtils.getRemovedAttendees(
                                        except.getAttendees(), seriesInv.getAttendees(), false, acct);
                                if (!toNotify.isEmpty()) {
                                    notifyRemovedAttendees(zsc, octxt, acct, mbox, calItem, except, toNotify, sendQueue);
                                }
                            }
                        }
                    }
                }

                // Save the change to the series as specified by the client.
                sendCalendarMessage(zsc, octxt, folderId, acct, mbox, dat, response, true, forceSend, sendQueue);

                // Echo the updated inv in the response.
                if (echo && dat.mAddInvData != null) {
                    echoAddedInvite(response, ifmt, octxt, mbox, dat.mAddInvData, maxSize, wantHTML, neuter);
                }

                boolean ignorePastExceptions = true;

                // Reflect added/removed attendees in the exception instances.
                if (!atsAdded.isEmpty() || !atsCanceled.isEmpty()) {
                    addRemoveAttendeesInExceptions(octxt, mbox, inv.getCalendarItem(), atsAdded, atsCanceled, ignorePastExceptions);
                }

                // Send notifications.
                if (hasRecipients) {
                    notifyCalendarItem(zsc, octxt, acct, mbox, inv.getCalendarItem(), notifyAllAttendees, atsAdded, ignorePastExceptions, sendQueue);
                }
            } else {
                // Modifying a one-off appointment or an exception instance.  There are no
                // complications like in the series update case.  Just update the invite with the
                // data supplied by the client, and let the built-in notification take place.
                sendCalendarMessage(zsc, octxt, folderId, acct, mbox, dat, response, true, forceSend, sendQueue);

                // Echo the updated inv in the response.
                if (echo && dat.mAddInvData != null) {
                    echoAddedInvite(response, ifmt, octxt, mbox, dat.mAddInvData, maxSize, wantHTML, neuter);
                }
            }
        } else {  // not organizer
            // Apply the change.
            sendCalendarMessage(zsc, octxt, folderId, acct, mbox, dat, response, true, forceSend, sendQueue);

            // Echo the updated inv in the response.
            if (echo && dat.mAddInvData != null) {
                echoAddedInvite(response, ifmt, octxt, mbox, dat.mAddInvData, maxSize, wantHTML, neuter);
            }
        }

        return response;
    }

    // Find out if we're notifying all attendees or only those who were added.  Let's assume we're notifying
    // all attendees if the to/cc/bcc list contains anyone other than those being added.
    private static boolean isNotifyingAll(MimeMessage mm, List<ZAttendee> atsAdded) throws ServiceException {
        Set<String> rcptsSet = new HashSet<String>();
        try {
            Address[] rcpts = mm.getAllRecipients();
            if (rcpts != null) {
                for (Address rcpt : rcpts) {
                    if (rcpt instanceof InternetAddress) {
                        String email = ((InternetAddress) rcpt).getAddress();
                        if (email != null) {
                            rcptsSet.add(email.toLowerCase());
                        }
                    }
                }
            }
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
        }
        // Subtract the added attendees from the set.
        for (ZAttendee at : atsAdded) {
            if (at != null && at.getAddress() != null) {
                rcptsSet.remove(at.getAddress().toLowerCase());
            }
        }
        return !rcptsSet.isEmpty();
    }

    private static void removeAllRecipients(MimeMessage mm) throws ServiceException {
        try {
            RecipientType rcptTypes[] = { RecipientType.TO, RecipientType.CC, RecipientType.BCC };
            for (RecipientType rcptType : rcptTypes) {
                mm.setRecipients(rcptType, (Address[]) null);
            }
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
        }
    }
}
