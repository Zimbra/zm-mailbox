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
package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;


public class CreateCalendarItemException extends CreateCalendarItem {
    
    private static Log sLog = LogFactory.getLog(CreateCalendarItemException.class);

    private static final String[] TARGET_PATH = new String[] { MailService.A_ID };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return false; }

    protected class CreateCalendarItemExceptionInviteParser extends ParseMimeMessage.InviteParser
    {
        private String mUid;
        private Invite mDefaultInvite;

        CreateCalendarItemExceptionInviteParser(String uid, Invite defaultInvite) {
            mUid = uid;
            mDefaultInvite = defaultInvite;
        }

        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext lc, Account account, Element inviteElem)
        throws ServiceException {
            ParseMimeMessage.InviteParserResult toRet =
                CalendarUtils.parseInviteForCreate(
                        account, getItemType(), inviteElem,
                        mDefaultInvite.getTimeZoneMap(), mUid, true,
                        CalendarUtils.RECUR_NOT_ALLOWED);

            // Send cancellations to any attendees who have been removed.
            List<ZAttendee> removedAttendees =
                CalendarUtils.getRemovedAttendees(mDefaultInvite, toRet.mInvite);
            if (removedAttendees.size() > 0)
                updateRemovedInvitees(lc, account, mDefaultInvite.getCalendarItem().getMailbox(),
                                      mDefaultInvite.getCalendarItem(), toRet.mInvite, removedAttendees);

            return toRet;
        }
    };

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        
        ItemId iid = new ItemId(request.getAttribute(MailService.A_ID), lc);
        int compNum = (int) request.getAttributeLong(MailService.E_INVITE_COMPONENT);
        sLog.info("<CreateCalendarItemException id=" + lc.formatItemId(iid) + " comp=" + compNum + "> " + lc.toString());
        
        // <M>
        Element msgElem = request.getElement(MailService.E_MSG);
        
        if (msgElem.getAttribute(MailService.A_FOLDER, null) != null) {
            throw ServiceException.FAILURE("You may not specify a target Folder when creating an Exception for an existing calendar item", null);
        }
        
        Element response = getResponseElement(lc);
        synchronized(mbox) {
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId()); 
            Invite inv = calItem.getInvite(iid.getSubpartId(), compNum);
            
            if (inv.hasRecurId()) {
                throw MailServiceException.INVITE_OUT_OF_DATE("Invite id=" + lc.formatItemId(iid) + " comp=" + compNum + " is not the a default invite");
            }
            
            if (calItem == null)
                throw MailServiceException.NO_SUCH_CALITEM(inv.getUid(), " for CreateCalendarItemExceptionRequest(" + iid + "," + compNum + ")");
            else if (!calItem.isRecurring())
                throw ServiceException.INVALID_REQUEST("CalendarItem " + calItem.getId() + " is not a recurring calendar item", null);
            
            CreateCalendarItemExceptionInviteParser parser = new CreateCalendarItemExceptionInviteParser(calItem.getUid(), inv);
            CalSendData dat = handleMsgElement(lc, msgElem, acct, mbox, parser);
            
            return sendCalendarMessage(lc, calItem.getFolderId(), acct, mbox, dat, response, false);
        }
    }
}
