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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.GetContactListOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.formatter.ContactCSV;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class ExportContacts extends DocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.A_FOLDER };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        Session session = getSession(context);

        String folder = request.getAttribute(MailService.A_FOLDER, null);
        ItemId iidFolder = folder == null ? null : new ItemId(folder, lc);

        String ct = request.getAttribute(MailService.A_CONTENT_TYPE);
        if (!ct.equals("csv"))
            throw ServiceException.INVALID_REQUEST("unsupported content type: " + ct, null);

        GetContactListOperation op = new GetContactListOperation(session, octxt, mbox, Requester.SOAP, iidFolder);
        op.schedule();
        List<Contact> contacts = op.getResults();
        
        StringBuffer sb = new StringBuffer();
        if (contacts == null)
        	contacts = new ArrayList<Contact>();
        
        ContactCSV.toCSV(contacts, sb);

        Element response = lc.createElement(MailService.EXPORT_CONTACTS_RESPONSE);
        Element content = response.addElement(MailService.E_CONTENT);
        content.setText(sb.toString());

        return response;
    }
}
