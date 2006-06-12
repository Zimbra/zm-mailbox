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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.CreateContactOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.formatter.ContactCSV;
import com.zimbra.cs.service.formatter.ContactCSV.ParseException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class ImportContacts extends DocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.A_FOLDER };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }

    String DEFAULT_FOLDER_ID = Mailbox.ID_FOLDER_CONTACTS + "";

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        Session session = getSession(context);

        String folder = request.getAttribute(MailService.A_FOLDER, DEFAULT_FOLDER_ID);
        ItemId iidFolder = new ItemId(folder, lc);

        String ct = request.getAttribute(MailService.A_CONTENT_TYPE);
        if (!ct.equals("csv"))
            throw ServiceException.INVALID_REQUEST("unsupported content type: " + ct, null);
        
        Element content = request.getElement(MailService.E_CONTENT);
        List<Map<String, String>> contacts = null;
        List<Upload> uploads = null;
        BufferedReader reader = null;
        String attachment = content.getAttribute(MailService.A_ATTACHMENT_ID, null);
        try {
            if (attachment == null)
                reader = new BufferedReader(new StringReader(content.getText()));
            else
                reader = parseUploadedContent(lc, attachment, uploads = new ArrayList<Upload>());
            contacts = ContactCSV.getContacts(reader);
        } catch (ParseException e) {
            throw MailServiceException.UNABLE_TO_IMPORT_CONTACTS(e.getMessage(), e);
        } finally {
            if (reader != null)
                try { reader.close(); } catch (IOException e) { }
            if (attachment != null)
                FileUploadServlet.deleteUploads(uploads);
        }

        CreateContactOperation op = new CreateContactOperation(session, octxt, mbox, Requester.SOAP, iidFolder, contacts, null);
        op.schedule();
        List<Contact> results = op.getContacts();

        StringBuffer ids = new StringBuffer();
        for (Contact c : results) {
        	if (ids.length() > 0)
        		ids.append(",");
        	ids.append(c.getId());
        }

        Element response = lc.createElement(MailService.IMPORT_CONTACTS_RESPONSE);
        Element cn = response.addElement(MailService.E_CONTACT);
        cn.addAttribute(MailService.A_IDS, ids.toString());
        cn.addAttribute(MailService.A_NUM, contacts.size());
        return response;
    }
    
    private static BufferedReader parseUploadedContent(ZimbraSoapContext lc, String attachId, List<Upload> uploads)
    throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), attachId, lc.getRawAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        uploads.add(up);
        try {
            return new BufferedReader(new InputStreamReader(up.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
    }
}
