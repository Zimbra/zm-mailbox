/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.formatter.ContactCSV;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class ExportContacts extends MailDocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        String folder = request.getAttribute(MailConstants.A_FOLDER, null);
        ItemId iidFolder = folder == null ? null : new ItemId(folder, zsc);

        String ct = request.getAttribute(MailConstants.A_CONTENT_TYPE);
        if (!ct.equals("csv"))
            throw ServiceException.INVALID_REQUEST("unsupported content type: " + ct, null);
        
        String format = request.getAttribute(MailConstants.A_CSVFORMAT, null);
        
        List<Contact> contacts = mbox.getContactList(octxt, iidFolder != null ? iidFolder.getId() : -1);
        
        StringBuffer sb = new StringBuffer();
        if (contacts == null)
        	contacts = new ArrayList<Contact>();
        
        try {
            ContactCSV contactCSV = new ContactCSV();
            contactCSV.toCSV(format, contacts.iterator(), sb);
        } catch (ContactCSV.ParseException e) {
            throw MailServiceException.UNABLE_TO_EXPORT_CONTACTS(e.getMessage(), e);
        }

        Element response = zsc.createElement(MailConstants.EXPORT_CONTACTS_RESPONSE);
        Element content = response.addElement(MailConstants.E_CONTENT);
        content.setText(sb.toString());

        return response;
    }
}
