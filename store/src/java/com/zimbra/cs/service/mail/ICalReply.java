/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class ICalReply extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        Element icalElem = request.getElement(MailConstants.E_CAL_ICAL);
        String icalStr = icalElem.getText();
        String sender = icalElem.getAttribute(MailConstants.E_CAL_ATTENDEE, null);
        ZVCalendar cal = ZCalendarBuilder.build(icalStr);

        List<Invite> invites =
            Invite.createFromCalendar(mbox.getAccount(), null, cal, false);
        for (Invite inv : invites) {
            String method = inv.getMethod();
            if (!ICalTok.REPLY.toString().equals(method)) {
                throw ServiceException.INVALID_REQUEST(
                        "iCalendar method must be REPLY (was " + method + ")", null);
            }
        }
        for (Invite inv : invites) {
            mbox.processICalReply(octxt, inv, sender);
        }

        Element response = getResponseElement(zsc);
        return response;
    }
}
