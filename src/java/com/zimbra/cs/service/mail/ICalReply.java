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

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class ICalReply extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException, SoapFaultException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element icalElem = request.getElement(MailService.E_APPT_ICAL);
        String icalStr = icalElem.getText();
        ZVCalendar cal;
        StringReader sr = null;
        try {
            sr = new StringReader(icalStr);
            cal = ZCalendarBuilder.build(sr);
        } finally {
            if (sr != null)
                sr.close();
        }

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
            mbox.processICalReply(octxt, inv);
        }

        Element response = lc.createElement(MailService.ICAL_REPLY_RESPONSE);
        return response;
    }
}
