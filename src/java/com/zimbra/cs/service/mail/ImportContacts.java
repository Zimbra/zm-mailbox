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
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.formatter.ContactCSV;
import com.zimbra.cs.service.formatter.ContactCSV.ParseException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.client.ZMailbox;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.ImportContactsRequest;
import com.zimbra.soap.mail.message.ImportContactsResponse;
import com.zimbra.soap.mail.type.Content;
import com.zimbra.soap.mail.type.ImportContact;

/**
 * @author schemers
 */
public class ImportContacts extends MailDocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return true;
    }

    private String DEFAULT_FOLDER_ID = Mailbox.ID_FOLDER_CONTACTS + "";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        ImportContactsRequest req = JaxbUtil.elementToJaxb(request);
        String ct = req.getContentType();
        if (!ct.equals(ZMailbox.CONTACT_IMPORT_TYPE_CSV))
            throw ServiceException.INVALID_REQUEST("unsupported content type: " + ct, null);
        String folder = req.getFolderId();
        if (folder == null)
            folder = this.DEFAULT_FOLDER_ID;
        ItemId iidFolder = new ItemId(folder, zsc);

        String format = req.getCsvFormat();
        String locale = req.getCsvLocale();
        Content reqContent = req.getContent();
        List<Map<String, String>> contacts = null;
        List<Upload> uploads = null;
        BufferedReader reader = null;
        String attachment = reqContent.getAttachUploadId();
        try {
            if (attachment == null) {
                // Convert LF to CRLF because the XML parser normalizes element text to LF.
                String text = StringUtil.lfToCrlf(reqContent.getValue());
                reader = new BufferedReader(new StringReader(text));
            } else {
                reader = parseUploadedContent(zsc, attachment, uploads = new ArrayList<Upload>());
            }

            contacts = ContactCSV.getContacts(reader, format, locale);
            reader.close();
        } catch (IOException e) {
            throw MailServiceException.UNABLE_TO_IMPORT_CONTACTS(e.getMessage(), e);
        } catch (ParseException e) {
            throw MailServiceException.UNABLE_TO_IMPORT_CONTACTS(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException e) { }
            }
            if (attachment != null) {
                FileUploadServlet.deleteUploads(uploads);
            }
        }

        List<ItemId> idsList = ImportCsvContacts(octxt, mbox, iidFolder, contacts);


        ImportContactsResponse resp = new ImportContactsResponse();
        ImportContact impCntct = new ImportContact();

        for (ItemId iid : idsList) {
            impCntct.addCreatedId(iid.toString(ifmt));
        }
        impCntct.setNumImported(contacts.size());
        resp.setContact(impCntct);

        return zsc.jaxbToElement(resp);
    }

    private static BufferedReader parseUploadedContent(ZimbraSoapContext lc, String attachId, List<Upload> uploads)
    throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), attachId, lc.getAuthToken());
        if (up == null) {
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        }

        uploads.add(up);
        try {
            return new BufferedReader(new InputStreamReader(up.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
    }

    public static List<ItemId> ImportCsvContacts(OperationContext oc, Mailbox mbox,  ItemId iidFolder, List<Map<String, String>> csvContacts)
    throws ServiceException {
        List<ItemId> createdIds = new LinkedList<ItemId>();
        for (Map<String,String> contact : csvContacts) {
            String[] tags = TagUtil.decodeTags(ContactCSV.getTags(contact));
            Contact c = mbox.createContact(oc, new ParsedContact(contact), iidFolder.getId(), tags);
            createdIds.add(new ItemId(c));
        }
        return createdIds;
    }
}
