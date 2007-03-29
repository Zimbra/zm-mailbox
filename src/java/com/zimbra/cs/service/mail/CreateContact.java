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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimePart;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
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
        Mailbox.OperationContext octxt = zsc.getOperationContext();
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        boolean verbose = request.getAttributeBool(MailConstants.A_VERBOSE, true);

        Element cn = request.getElement(MailConstants.E_CONTACT);
        ItemId iidFolder = new ItemId(cn.getAttribute(MailConstants.A_FOLDER, DEFAULT_FOLDER), zsc);
        String tagsStr = cn.getAttribute(MailConstants.A_TAGS, null);

        Element vcard = cn.getOptionalElement(MailConstants.E_VCARD);

        List<ParsedContact> pclist;
        if (vcard != null) {
            pclist = parseAttachedVCard(zsc, mbox, vcard);
        } else {
            pclist = new ArrayList<ParsedContact>(1);
            Pair<Map<String,String>, List<Attachment>> cdata = parseContact(zsc, cn);
            pclist.add(new ParsedContact(cdata.getFirst(), cdata.getSecond(), System.currentTimeMillis()));
        }

        List<Contact> contacts = createContacts(octxt, mbox, iidFolder, pclist, tagsStr);
        Contact con = null;
        if (contacts.size() > 0)
            con = contacts.get(0);
        
        Element response = zsc.createElement(MailConstants.CREATE_CONTACT_RESPONSE);
        if (con != null) {
            if (verbose)
                ToXML.encodeContact(response, ifmt, con, null, true, null);
            else
                response.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, con.getId());
        }
        return response;
    }

    static Pair<Map<String,String>, List<Attachment>> parseContact(ZimbraSoapContext zsc, Element cn) throws ServiceException {
        Map<String, String> fields = new HashMap<String, String>();
        List<Attachment> attachments = new ArrayList<Attachment>();

        for (Element elt : cn.listElements(MailConstants.E_ATTRIBUTE)) {
            String name = elt.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
            if (name.trim().equals(""))
                throw ServiceException.INVALID_REQUEST("at least one contact field name is blank", null);

            Attachment attach = parseAttachment(zsc, elt, name);
            if (attach == null)
                fields.put(name, elt.getText());
            else
                attachments.add(attach);
        }

        return new Pair<Map<String,String>, List<Attachment>>(fields, attachments);
    }

    private static Attachment parseAttachment(ZimbraSoapContext zsc, Element elt, String name) throws ServiceException {
        // check for uploaded attachment
        String attachId = elt.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
        if (attachId != null) {
            try {
                Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachId, zsc.getRawAuthToken());
                return new Attachment(ByteUtil.getContent(up.getInputStream(), 0), up.getContentType(), name, up.getName());
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("error reading uploaded attachment", ioe);
            }
        }

        // FIXME: support attaching messages, message parts, contact attachments, documents
        return null;
    }

    private static List<ParsedContact> parseAttachedVCard(ZimbraSoapContext zsc, Mailbox mbox, Element vcard)
    throws ServiceException {
        String text = null;
        String messageId = vcard.getAttribute(MailConstants.A_MESSAGE_ID, null);
        String attachId = vcard.getAttribute(MailConstants.A_ATTACHMENT_ID, null);

        if (attachId != null) {
            // separately-uploaded vcard attachment
            Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachId, zsc.getRawAuthToken());
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
            if (iid.isLocal()) {
                try {
                    // fetch from local store
                    if (!mbox.getAccountId().equals(iid.getAccountId()))
                        mbox = MailboxManager.getInstance().getMailboxByAccountId(iid.getAccountId());
                    Message msg = mbox.getMessageById(zsc.getOperationContext(), iid.getId());
                    MimePart mp = Mime.getMimePart(msg.getMimeMessage(), part);
                    ContentType ctype = new ContentType(mp.getContentType());
                    if (!ctype.match(Mime.CT_TEXT_PLAIN) && !ctype.match(Mime.CT_TEXT_VCARD))
                        throw MailServiceException.INVALID_CONTENT_TYPE(ctype.getBaseType());
                    text = Mime.getStringContent(mp);
                } catch (IOException e) {
                    throw ServiceException.FAILURE("error reading vCard", e);
                } catch (MessagingException e) {
                    throw ServiceException.FAILURE("error fetching message part", e);
                }
            } else {
                try {
                    // fetch from remote store
                    Map<String, String> params = new HashMap<String, String>();
                    params.put(UserServlet.QP_PART, part);
                    byte[] content = UserServlet.getRemoteContent(zsc.getRawAuthToken(), iid, params);
                    text = new String(content, "utf-8");
                } catch (IOException e) {
                    throw ServiceException.FAILURE("error reading vCard", e);
                }
            }
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
}
