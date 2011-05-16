/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.ExportContactsRequest;
import com.zimbra.soap.mail.message.ExportContactsResponse;

/**
 * @author schemers
 */
public final class ExportContacts extends MailDocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH =
                new String[] { MailConstants.A_FOLDER };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        ExportContactsRequest req = JaxbUtil.elementToJaxb(request);
        String folder = req.getFolderId();
        ItemId iidFolder = folder == null ? null : new ItemId(folder, zsc);

        String ct = req.getContentType();
        if (ct == null)
            throw ServiceException.INVALID_REQUEST(
                    "content type missing", null);
        if (!ct.equals("csv"))
            throw ServiceException.INVALID_REQUEST(
                    "unsupported content type: " + ct, null);

        String format = req.getCsvFormat();
        String locale = req.getCsvLocale();
        String separator = req.getCsvDelimiter();
        Character sepChar = null;
        if ((separator != null) && (separator.length() > 0))
            sepChar = separator.charAt(0);

        List<Contact> contacts = mbox.getContactList(
                octxt, iidFolder != null ? iidFolder.getId() : -1);

        StringBuilder sb = new StringBuilder();
        if (contacts == null)
            contacts = new ArrayList<Contact>();

        try {
            ContactCSV contactCSV = new ContactCSV();
            contactCSV.toCSV(format, locale, sepChar, contacts.iterator(), sb);
        } catch (ContactCSV.ParseException e) {
            throw MailServiceException.UNABLE_TO_EXPORT_CONTACTS(
                        e.getMessage(), e);
        }

        ExportContactsResponse resp = new ExportContactsResponse(sb.toString());
        return zsc.jaxbToElement(resp);
    }
}
