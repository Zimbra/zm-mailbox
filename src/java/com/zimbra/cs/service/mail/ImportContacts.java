/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.formatter.ContactCSV;
import com.zimbra.cs.service.formatter.ContactCSV.ParseException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class ImportContacts extends MailDocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }

    String DEFAULT_FOLDER_ID = Mailbox.ID_FOLDER_CONTACTS + "";

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        String folder = request.getAttribute(MailConstants.A_FOLDER, DEFAULT_FOLDER_ID);
        ItemId iidFolder = new ItemId(folder, zsc);

        String ct = request.getAttribute(MailConstants.A_CONTENT_TYPE);
        if (!ct.equals("csv"))
            throw ServiceException.INVALID_REQUEST("unsupported content type: " + ct, null);
        
        String format = request.getAttribute(MailConstants.A_CSVFORMAT, null);
        String locale = request.getAttribute(MailConstants.A_CSVLOCALE, null);
        Element content = request.getElement(MailConstants.E_CONTENT);
        List<Map<String, String>> contacts = null;
        List<Upload> uploads = null;
        BufferedReader reader = null;
        String attachment = content.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
        try {
            if (attachment == null)
                reader = new BufferedReader(new StringReader(content.getText()));
            else
                reader = parseUploadedContent(zsc, attachment, uploads = new ArrayList<Upload>());
            
            contacts = ContactCSV.getContacts(reader, format, locale);
            reader.close();
        } catch (IOException e) {
            throw MailServiceException.UNABLE_TO_IMPORT_CONTACTS(e.getMessage(), e);
        } catch (ParseException e) {
            throw MailServiceException.UNABLE_TO_IMPORT_CONTACTS(e.getMessage(), e);
        } finally {
            if (reader != null)
                try { reader.close(); } catch (IOException e) { }
            if (attachment != null)
                FileUploadServlet.deleteUploads(uploads);
        }

        List<ItemId> idsList = ImportCsvContacts(octxt, mbox, iidFolder, contacts);
        

        StringBuilder ids = new StringBuilder();
        
        for (ItemId iid : idsList) {
            if (ids.length() > 0)
                ids.append(",");
            ids.append(iid.toString(ifmt));
        }

        Element response = zsc.createElement(MailConstants.IMPORT_CONTACTS_RESPONSE);
        Element cn = response.addElement(MailConstants.E_CONTACT);
        cn.addAttribute(MailConstants.A_IDS, ids.toString());
        cn.addAttribute(MailConstants.A_NUM, contacts.size());
        return response;
    }
    
    private static BufferedReader parseUploadedContent(ZimbraSoapContext lc, String attachId, List<Upload> uploads)
    throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), attachId, lc.getAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        uploads.add(up);
        try {
            return new BufferedReader(new InputStreamReader(up.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
    }
    
    public static List<ItemId> ImportCsvContacts(OperationContext oc, Mailbox mbox, 
        ItemId iidFolder, List<Map<String, String>> csvContacts) throws ServiceException {
        
        List<ItemId> createdIds = new LinkedList<ItemId>();
        for (Map<String,String> contact : csvContacts) {
        	String tags = getTags(oc, mbox, contact);
        	Contact c = mbox.createContact(oc, new ParsedContact(contact), iidFolder.getId(), tags);
        	createdIds.add(new ItemId(c));
        }
        
        return createdIds;
    }
    
    public static String getTags(OperationContext octxt, Mailbox mbox, Map<String,String> contact) {
		String tags = ContactCSV.getTags(contact);
		if (tags == null)
			return null;
		StringBuilder tagIds = null;
		for (String t : tags.split(",")) {
			Tag tag = null;
			try {
				tag = mbox.getTagByName(t);
			} catch (ServiceException se) {
				try {
					tag = mbox.createTag(octxt, t, MailItem.DEFAULT_COLOR);
				} catch (ServiceException e) {
				}
			}
			if (tag != null) {
				if (tagIds == null)
					tagIds = new StringBuilder(Integer.toString(tag.getId()));
				else
					tagIds.append(",").append(Integer.toString(tag.getId()));
			}
		}
		return tagIds.toString();
    }
    
}
