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
            mbox.addInvite(octxt, inv, -1, false, null);
        }

        Element response = lc.createElement(MailService.ICAL_REPLY_RESPONSE);
        return response;
    }
}
