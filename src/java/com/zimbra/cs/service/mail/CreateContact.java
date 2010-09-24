/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimePartDataSource;

import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.DocumentDataSource;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MessageDataSource;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.UploadDataSource;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class CreateContact extends MailDocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_CONTACT, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    @Override protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    @Override protected boolean checkMountpointProxy(Element request)  { return true; }
    @Override protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    private static final String DEFAULT_FOLDER = "" + Mailbox.ID_FOLDER_CONTACTS;

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        boolean verbose = request.getAttributeBool(MailConstants.A_VERBOSE, true);

        Element cn = request.getElement(MailConstants.E_CONTACT);
        ItemId iidFolder = new ItemId(cn.getAttribute(MailConstants.A_FOLDER, DEFAULT_FOLDER), zsc);
        String tagsStr = cn.getAttribute(MailConstants.A_TAGS, null);

        Element vcard = cn.getOptionalElement(MailConstants.E_VCARD);

        List<ParsedContact> pclist;
        if (vcard != null) {
            pclist = parseAttachedVCard(zsc, octxt, mbox, vcard);
        } else {
            pclist = new ArrayList<ParsedContact>(1);
            Pair<Map<String,String>, List<Attachment>> cdata = parseContact(cn, zsc, octxt);
            pclist.add(new ParsedContact(cdata.getFirst(), cdata.getSecond()));
        }

        List<Contact> contacts = createContacts(octxt, mbox, iidFolder, pclist, tagsStr);
        Contact con = null;
        if (contacts.size() > 0)
            con = contacts.get(0);
        
        Element response = zsc.createElement(MailConstants.CREATE_CONTACT_RESPONSE);
        if (con != null) {
            if (verbose)
                ToXML.encodeContact(response, ifmt, con, true, null);
            else
                response.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, con.getId());
        }
        return response;
    }

    static Pair<Map<String,String>, List<Attachment>> parseContact(Element cn, ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
        return parseContact(cn, zsc, octxt, null);
    }

    static Pair<Map<String,String>, List<Attachment>> parseContact(Element cn, ZimbraSoapContext zsc, OperationContext octxt, Contact existing) throws ServiceException {
        Map<String, String> fields = new HashMap<String, String>();
        List<Attachment> attachments = new ArrayList<Attachment>();

        for (Element elt : cn.listElements(MailConstants.E_ATTRIBUTE)) {
            String name = elt.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
            if (name.trim().equals(""))
                throw ServiceException.INVALID_REQUEST("at least one contact field name is blank", null);

            Attachment attach = parseAttachment(elt, name, zsc, octxt, existing);
            if (attach == null)
                fields.put(name, elt.getText());
            else
                attachments.add(attach);
        }

        return new Pair<Map<String,String>, List<Attachment>>(fields, attachments);
    }

    private static Attachment parseAttachment(Element elt, String name, ZimbraSoapContext zsc, OperationContext octxt, Contact existing) throws ServiceException {
        // check for uploaded attachment
        String attachId = elt.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
        if (attachId != null) {
            Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachId, zsc.getAuthToken());
            UploadDataSource uds = new UploadDataSource(up);
            return new Attachment(new DataHandler(uds), name, (int) up.getSize());
        }

        int itemId = (int) elt.getAttributeLong(MailConstants.A_ID, -1);
        String part = elt.getAttribute(MailConstants.A_PART, null);
        if (itemId != -1 || (part != null && existing != null)) {
            MailItem item = itemId == -1 ? existing : getRequestedMailbox(zsc).getItemById(octxt, itemId, MailItem.TYPE_UNKNOWN);

            try {
                if (item instanceof Contact) {
                    Contact contact = (Contact) item;
                    if (part != null && !part.equals("")) {
                        try {
                            int partNum = Integer.parseInt(part) - 1;
                            if (partNum >= 0 && partNum < contact.getAttachments().size()) {
                                Attachment att = contact.getAttachments().get(partNum);
                                return new Attachment(att.getDataHandler(), name, att.getSize());
                            }
                        } catch (NumberFormatException nfe) { }
                        throw ServiceException.INVALID_REQUEST("invalid contact part number: " + part, null);
                    } else {
                        VCard vcf = VCard.formatContact(contact);
                        return new Attachment(vcf.formatted.getBytes("utf-8"), "text/x-vcard; charset=utf-8", name, vcf.fn + ".vcf");
                    }
                } else if (item instanceof Message) {
                    Message msg = (Message) item;
                    if (part != null && !part.equals("")) {
                        try {
                            MimePart mp = Mime.getMimePart(msg.getMimeMessage(), part);
                            if (mp == null)
                                throw MailServiceException.NO_SUCH_PART(part);
                            DataSource ds = new MimePartDataSource(mp);
                            return new Attachment(new DataHandler(ds), name);
                        } catch (MessagingException me) {
                            throw ServiceException.FAILURE("error parsing blob", me);
                        }
                    } else {
                        DataSource ds = new MessageDataSource(msg);
                        return new Attachment(new DataHandler(ds), name, (int) msg.getSize());
                    }
                } else if (item instanceof Document) {
                    Document doc = (Document) item;
                    if (part != null && !part.equals(""))
                        throw MailServiceException.NO_SUCH_PART(part);
                    DataSource ds = new DocumentDataSource(doc);
                    return new Attachment(new DataHandler(ds), name, (int) doc.getSize());
                }
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("error attaching existing item data", ioe);
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("error attaching existing item data", e);
            }
        }

        return null;
    }

    private static List<ParsedContact> parseAttachedVCard(ZimbraSoapContext zsc, OperationContext octxt, Mailbox mbox, Element vcard)
    throws ServiceException {
        String text = null;
        String messageId = vcard.getAttribute(MailConstants.A_MESSAGE_ID, null);
        String attachId = vcard.getAttribute(MailConstants.A_ATTACHMENT_ID, null);

        if (attachId != null) {
            // separately-uploaded vcard attachment
            Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachId, zsc.getAuthToken());
            try {
                text = new String(ByteUtil.getContent(up.getInputStream(), 0));
            } catch (IOException e) {
                throw ServiceException.FAILURE("error reading vCard", e);
            }
        } else if (messageId == null) {
            // inlined content in the <vcard> element
            text = vcard.getText();
        } else {
            // part of existing message
            ItemId iid = new ItemId(messageId, zsc);
            String part = vcard.getAttribute(MailConstants.A_PART);
            String[] acceptableTypes = new String[] { MimeConstants.CT_TEXT_PLAIN, MimeConstants.CT_TEXT_VCARD, MimeConstants.CT_TEXT_VCARD_LEGACY, MimeConstants.CT_TEXT_VCARD_LEGACY2 };
            String charsetWanted = mbox.getAccount().getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, null);
            text = fetchItemPart(zsc, octxt, mbox, iid, part, acceptableTypes, charsetWanted);
        }

        List<VCard> cards = VCard.parseVCard(text);
        if (cards == null || cards.size() == 0)
            throw MailServiceException.UNABLE_TO_IMPORT_CONTACTS("no vCards present in attachment", null);
        List<ParsedContact> pclist = new ArrayList<ParsedContact>(cards.size());
        for (VCard vcf : cards)
            pclist.add(vcf.asParsedContact());
        return pclist;
    }
    
    public static List<Contact> createContacts(OperationContext oc, Mailbox mbox, 
        ItemId iidFolder, List<ParsedContact> list, String tagsStr) throws ServiceException {
        
        List<Contact> toRet = new ArrayList<Contact>();
        
        synchronized(mbox) {
            for (ParsedContact pc : list) 
                toRet.add(mbox.createContact(oc, pc, iidFolder.getId(), tagsStr));
        }
        return toRet;
    }

    static String fetchItemPart(ZimbraSoapContext zsc, OperationContext octxt, Mailbox mbox,
                                ItemId iid, String part,
                                String[] acceptableMimeTypes, String charsetWanted)
    throws ServiceException {
        String text = null;
        try {
            if (iid.isLocal()) {
                // fetch from local store
                if (!mbox.getAccountId().equals(iid.getAccountId()))
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(iid.getAccountId());
                Message msg = mbox.getMessageById(octxt, iid.getId());
                MimePart mp = Mime.getMimePart(msg.getMimeMessage(), part);
                String ctype = new ContentType(mp.getContentType()).getContentType();
                boolean typeAcceptable;
                if (acceptableMimeTypes != null) {
                    typeAcceptable = false;
                    for (String type : acceptableMimeTypes) {
                        if (type != null && type.equalsIgnoreCase(ctype)) {
                            typeAcceptable = true;
                            break;
                        }
                    }
                } else {
                    typeAcceptable = true;
                }
                if (!typeAcceptable)
                    throw MailServiceException.INVALID_CONTENT_TYPE(ctype);
                text = Mime.getStringContent(mp, charsetWanted);
            } else {
                // fetch from remote store
                Map<String, String> params = new HashMap<String, String>();
                params.put(UserServlet.QP_PART, part);
                byte[] content = UserServlet.getRemoteContent(zsc.getAuthToken(), iid, params);
                text = new String(content, MimeConstants.P_CHARSET_UTF8);
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("error fetching message part: iid=" + iid + ", part=" + part, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("error fetching message part: iid=" + iid + ", part=" + part, e);
        }
        return text;
    }
}
