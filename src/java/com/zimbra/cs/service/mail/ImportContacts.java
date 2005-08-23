/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.ContactCSV;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.ContactCSV.ParseException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class ImportContacts extends DocumentHandler  {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        String ct = request.getAttribute(MailService.A_CONTENT_TYPE);
        if (!ct.equals("csv"))
            throw ServiceException.INVALID_REQUEST("unsupported content type: " + ct, null);
        
        Element content = request.getElement(MailService.E_CONTENT);
        List contacts = null;
        BufferedReader reader = null;
        String attachment = content.getAttribute(MailService.A_ATTACHMENT_ID, null);
        try {
            if (attachment == null) reader = new BufferedReader(new StringReader(content.getText()));
            else reader = parseUploadedContent(mbox, attachment);
            contacts = ContactCSV.getContacts(reader);
        } catch (ParseException e) {
            throw MailServiceException.UNABLE_TO_IMPORT_CONTACTS(e.getMessage(), e);
        } finally {
            if (reader != null) try { reader.close(); } catch (IOException e) { }
            if (attachment != null) FileUploadServlet.deleteUploads(mbox.getAccountId(), attachment);
        }

        StringBuffer ids = new StringBuffer();
        for (Iterator it = contacts.iterator(); it.hasNext(); ) {
            Map cmap = (Map) it.next();
            Contact contact = mbox.createContact(octxt, cmap, Mailbox.ID_FOLDER_CONTACTS, null);
            if (ids.length() > 0) ids.append(",");
            ids.append(contact.getId());
        }

        Element response = lc.createElement(MailService.IMPORT_CONTACTS_RESPONSE);
        Element cn = response.addElement(MailService.E_CONTACT);
        cn.addAttribute(MailService.A_IDS, ids.toString());
        cn.addAttribute(MailService.A_NUM, contacts.size());
        return response;
    }
    
    private static BufferedReader parseUploadedContent(Mailbox mbox, String attachId) throws ServiceException {
        List uploads = FileUploadServlet.fetchUploads(mbox.getAccountId(), attachId);
        if (uploads == null || uploads.size() == 0)
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        else if (uploads.size() > 1)
            throw MailServiceException.TOO_MANY_UPLOADS(attachId);

        FileItem fi = (FileItem) uploads.get(0);
        try {
            return new BufferedReader(new InputStreamReader(fi.getInputStream()));
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
    }

}
