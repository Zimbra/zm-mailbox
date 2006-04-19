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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.formatter.ContactCSV;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class ExportContacts extends DocumentHandler  {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        String ct = request.getAttribute(MailService.A_CONTENT_TYPE);
        if (!ct.equals("csv"))
            throw ServiceException.INVALID_REQUEST("unsupported content type: "+ct, null);

        List contacts = mbox.getContactList(lc.getOperationContext(), -1);
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
