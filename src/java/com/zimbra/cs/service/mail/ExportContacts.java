package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.ContactCSV;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class ExportContacts extends DocumentHandler  {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        String ct = request.getAttribute(MailService.A_CONTENT_TYPE);
        if (!ct.equals("csv"))
            throw ServiceException.INVALID_REQUEST("unsupported content type: "+ct, null);

        List contacts = mbox.getContactList(-1);
        StringBuffer sb = new StringBuffer();
        if (contacts == null)
            contacts = new ArrayList();
        ContactCSV.toCSV(contacts, sb);

        Element response = lc.createElement(MailService.EXPORT_CONTACTS_RESPONSE);
        Element content = response.addElement(MailService.E_CONTENT);
        content.setText(sb.toString());

        return response;
    }
}
