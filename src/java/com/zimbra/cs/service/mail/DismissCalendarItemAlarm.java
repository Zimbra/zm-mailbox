/* ***** BEGIN LICENSE BLOCK *****
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
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.CalendarItem.AlarmData;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

public class DismissCalendarItemAlarm extends DocumentHandler {

    /*
     * <DismissCalendarItemAlarmRequest>
     *   <appt|task id="cal item id" lastAlarm="alarm time in millis"/>+
     * </DismissCalendarItemAlarmRequest>
     *
     * <DismissCalendarItemAlarmResponse>
     *   <appt|task id="cal item id">
     *     <alarmData ...>  // so the client knows when to trigger the next alarm
     *   </appt|task>+
     * </DismissCalendarItemAlarmResponse>
     */

    private static final String[] sCalItemElems = {
        MailConstants.E_APPOINTMENT, MailConstants.E_TASK
    };

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        Element response = getResponseElement(zsc);
        for (String calItemElement : sCalItemElems) {
            for (Iterator<Element> iter = request.elementIterator(calItemElement);
                 iter.hasNext(); ) {
                Element calItemElem = iter.next();
                ItemId iid = new ItemId(calItemElem.getAttribute(MailConstants.A_ID), zsc);
                long dismissedAt = calItemElem.getAttributeLong(MailConstants.A_CAL_ALARM_DISMISSED_AT);
                int calItemId = iid.getId();
                mbox.dismissCalendarItemAlarm(octxt, calItemId, dismissedAt);

                CalendarItem calItem = mbox.getCalendarItemById(octxt, calItemId);
                AlarmData alarmData = calItem.getAlarmData();
                Element calItemRespElem;
                if (calItem instanceof Appointment)
                    calItemRespElem = response.addElement(MailConstants.E_APPOINTMENT);
                else
                    calItemRespElem = response.addElement(MailConstants.E_TASK);
                calItemRespElem.addAttribute(MailConstants.A_CAL_ID, calItem.getId());
                if (alarmData != null)
                    ToXML.encodeAlarmData(calItemRespElem, calItem, alarmData);
            }
        }
        return response;
    }
}
