/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.soap.ZimbraSoapContext;

public class AddCalendarItemInvite extends CalendarRequest {

    protected class AddInviteParser extends ParseMimeMessage.InviteParser { 
        public ParseMimeMessage.InviteParserResult parseInviteElement(
                ZimbraSoapContext lc, OperationContext octxt, Account account, Element inviteElem)
        throws ServiceException {
            return CalendarUtils.parseInviteForAddInvite(account, getItemType(), inviteElem, null);
        }
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        AddInviteParser parser = new AddInviteParser();
        SetCalendarItemData scid = SetCalendarItem.getSetCalendarItemData(zsc, octxt, acct, mbox, request, parser);

        Invite inv = scid.mInv;
        CalendarItem calItem = mbox.getCalendarItemByUid(octxt, inv.getUid());
        int folderId = inv.isTodo() ? Mailbox.ID_FOLDER_TASKS : Mailbox.ID_FOLDER_CALENDAR;
        if (calItem != null) {
            int f = calItem.getFolderId();
            if (f != Mailbox.ID_FOLDER_TRASH && f != Mailbox.ID_FOLDER_SPAM)
                folderId = f;
        }
        
        int[] ids = mbox.addInvite(octxt, inv, folderId, scid.mPm, false, false, true);

        Element response = getResponseElement(zsc);
        if (ids != null && ids.length >= 2) {
            calItem = mbox.getCalendarItemById(octxt, ids[0]);
            if (calItem != null) {
                Invite[] invs = calItem.getInvites(ids[1]);
                if (invs != null && invs.length > 0) {
                    response.addAttribute(MailConstants.A_CAL_ID, ids[0]);
                    response.addAttribute(MailConstants.A_CAL_INV_ID, ids[1]);
                    response.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, invs[0].getComponentNum());
                }
            }
        }
        return response;
    }

}
