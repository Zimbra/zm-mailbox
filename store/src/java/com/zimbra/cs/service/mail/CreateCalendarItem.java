/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Feb 22, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;

import com.zimbra.common.util.ZimbraLog;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.mailbox.EmailToSMS;

/**
 * @author tim
 */
public class CreateCalendarItem extends CalendarRequest {
    
    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_MSG, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    // very simple: generate a new UID and send a REQUEST
    protected class CreateCalendarItemInviteParser extends ParseMimeMessage.InviteParser { 
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext lc, OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            return CalendarUtils.parseInviteForCreate(account, getItemType(), inviteElem, null, null, false, CalendarUtils.RECUR_ALLOWED);
        }
    };

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        // <M>
        Element msgElem = request.getElement(MailConstants.E_MSG);

        CreateCalendarItemInviteParser parser = new CreateCalendarItemInviteParser();
        CalSendData dat = handleMsgElement(zsc, octxt, msgElem, acct, mbox, parser);

        int defaultFolder = dat.mInvite.isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
        String defaultFolderStr = Integer.toString(defaultFolder);
        String folderIdStr = msgElem.getAttribute(MailConstants.A_FOLDER, defaultFolderStr);
        ItemId iidFolder = new ItemId(folderIdStr, zsc);

        // Don't allow creating in Trash folder/subfolder.  We don't want to invite attendees to an appointment in trash.
        Folder folder = mbox.getFolderById(octxt, iidFolder.getId());
        if (folder.inTrash())
            throw ServiceException.INVALID_REQUEST("cannot create a calendar item under trash", null);

        // trace logging
        if (!dat.mInvite.hasRecurId())
            ZimbraLog.calendar.info("<CreateCalendarItem> folderId=%d, subject=\"%s\", UID=%s",
                    iidFolder.getId(), dat.mInvite.isPublic() ? dat.mInvite.getName() : "(private)",
                    dat.mInvite.getUid());
        else
            ZimbraLog.calendar.info("<CreateCalendarItem> folderId=%d, subject=\"%s\", UID=%s, recurId=%s",
                    iidFolder.getId(), dat.mInvite.isPublic() ? dat.mInvite.getName() : "(private)",
                    dat.mInvite.getUid(), dat.mInvite.getRecurId().getDtZ());

        Element response = getResponseElement(zsc);

        boolean hasRecipients;
        try {
            Address[] rcpts = dat.mMm.getAllRecipients();
            hasRecipients = rcpts != null && rcpts.length > 0;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
        }
        // If we are sending this to other people, then we MUST be the organizer!
        if (!dat.mInvite.isOrganizer() && hasRecipients)
            throw MailServiceException.MUST_BE_ORGANIZER("CreateCalendarItem");

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
            dat.mInvite.setNeverSent(true);
        }
        boolean forceSend = request.getAttributeBool(MailConstants.A_CAL_FORCESEND, true);
        MailSendQueue sendQueue = new MailSendQueue();
        try {
            sendCalendarMessage(zsc, octxt, iidFolder.getId(), acct, mbox, dat, response, true, forceSend, sendQueue);
			// send sms to calendar creator
			try {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							EmailToSMS.getInstance().sendCalendarSMS(acct, dat.mMm);
						} catch (ServiceException e) {
							ZimbraLog.calendar.error("Error while sending calendar sms", e);
						} catch (OutOfMemoryError e) {
							ZimbraLog.calendar.error("OutOfMemoryError while sending calendar sms", e);
						}
					}
				};
				Thread smsThread = new Thread(r, "CalendarSMS");
				smsThread.setDaemon(true);
				smsThread.start();
			} catch (Exception e) {
				ZimbraLog.calendar.error("ServiceException while sending calendar sms", e);
			}
		} finally {
			sendQueue.send();
		}
        boolean echo = request.getAttributeBool(MailConstants.A_CAL_ECHO, false);
        if (echo && dat.mAddInvData != null) {
            ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
            int maxSize = (int) request.getAttributeLong(MailConstants.A_MAX_INLINED_LENGTH, 0);
            boolean wantHTML = request.getAttributeBool(MailConstants.A_WANT_HTML, false);
            boolean neuter = request.getAttributeBool(MailConstants.A_NEUTER, true);
            echoAddedInvite(response, ifmt, octxt, mbox, dat.mAddInvData, maxSize, wantHTML, neuter);
        }
        return response;
    }
}
