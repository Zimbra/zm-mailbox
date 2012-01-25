/* ***** BEGIN LICENSE BLOCK *****
/* Zimbra Collaboration Suite Server
/* Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
/* 
/* The contents of this file are subject to the Zimbra Public License
/* Version 1.3 ("License"); you may not use this file except in
/* compliance with the License.  You may obtain a copy of the License at
/* http://www.zimbra.com/license.
/* 
/* Software distributed under the License is distributed on an "AS IS"
/* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.CalendarItem.AlarmData;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

public class SnoozeCalendarItemAlarm extends DocumentHandler {

    private static final String[] sCalItemElems = {
        MailConstants.E_APPOINTMENT, MailConstants.E_TASK
    };

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        String acctId = mbox.getAccountId();
        Account authAcct = getAuthenticatedAccount(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        Element response = getResponseElement(zsc);
        for (String calItemElement : sCalItemElems) {
            for (Iterator<Element> iter = request.elementIterator(calItemElement);
                 iter.hasNext(); ) {
                Element calItemElem = iter.next();
                ItemId iid = new ItemId(calItemElem.getAttribute(MailConstants.A_ID), zsc);
                long snoozeUntil = calItemElem.getAttributeLong(MailConstants.A_CAL_ALARM_SNOOZE_UNTIL);

                // trace logging
                ZimbraLog.calendar.info("<SnoozeCalendarItemAlarm> id=%s,until=%d", iid.toString(), snoozeUntil);

                Mailbox ciMbox = null;  // mailbox for this calendar item; not necessarily same as requested mailbox
                String ciAcctId = iid.getAccountId();
                if (ciAcctId == null || ciAcctId.equals(acctId)) {
                    ciMbox = mbox;
                } else {
                    // Item ID refers to another mailbox.  (possible with ZDesktop)
                    // Let's see if the account is a Desktop account.
                    if (AccountUtil.isZDesktopLocalAccount(zsc.getAuthtokenAccountId()))
                        ciMbox = MailboxManager.getInstance().getMailboxByAccountId(ciAcctId, false);
                    if (ciMbox == null)
                        throw MailServiceException.PERM_DENIED("cannot snooze alarms of a shared calendar");
                }
                int calItemId = iid.getId();
                ItemIdFormatter ifmt = new ItemIdFormatter(authAcct.getId(), acctId, false);
                try {
                    ciMbox.snoozeCalendarItemAlarm(octxt, calItemId, snoozeUntil);
    
                    CalendarItem calItem = ciMbox.getCalendarItemById(octxt, calItemId);
                    Element calItemRespElem;
                    if (calItem instanceof Appointment)
                        calItemRespElem = response.addElement(MailConstants.E_APPOINTMENT);
                    else
                        calItemRespElem = response.addElement(MailConstants.E_TASK);
                    calItemRespElem.addAttribute(MailConstants.A_CAL_ID, iid.toString(ifmt));
    
                    boolean hidePrivate = !calItem.allowPrivateAccess(authAcct, zsc.isUsingAdminPrivileges());
                    boolean showAll = !hidePrivate || calItem.isPublic();
                    if (showAll) {
                        AlarmData alarmData = calItem.getAlarmData();
                        if (alarmData != null)
                            ToXML.encodeAlarmData(calItemRespElem, calItem, alarmData);
                    }
                } catch (NoSuchItemException nsie) {
                    //item must not exist in db anymore.
                    //this can happen if an alarm is snoozed while a big sync is happening which deletes the item (e.g. bug 48560)
                    //since item is not in db, it has effectively been snoozed; return ok and no further alarms
                    Element calItemRespElem = response.addElement(calItemElement); 
                    calItemRespElem.addAttribute(MailConstants.A_CAL_ID, iid.toString(ifmt));
                }
            }
        }
        return response;
    }
}
