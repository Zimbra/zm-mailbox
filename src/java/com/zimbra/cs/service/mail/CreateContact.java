/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.Version;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.ContactGroup;
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
import com.zimbra.cs.mailbox.ContactGroup.Member;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedContact.FieldDelta;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.UploadDataSource;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.soap.DocumentHandler;
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
        String[] tags = TagUtil.parseTags(cn, mbox, octxt);

        Element vcard = cn.getOptionalElement(MailConstants.E_VCARD);

        List<ParsedContact> pclist;
        if (vcard != null) {
            pclist = parseAttachedVCard(zsc, octxt, mbox, vcard);
        } else {
            pclist = new ArrayList<ParsedContact>(1);
            Pair<Map<String,Object>, List<Attachment>> cdata = parseContact(cn, zsc, octxt);
            pclist.add(new ParsedContact(cdata.getFirst(), cdata.getSecond()));
        }

        if (needToMigrateDlist(zsc)) {
            for (ParsedContact pc : pclist) {
                migrateFromDlist(pc);
            }
        }
        
        List<Contact> contacts = createContacts(octxt, mbox, iidFolder, pclist, tags);
        Contact con = null;
        if (contacts.size() > 0)
            con = contacts.get(0);

        Element response = zsc.createElement(MailConstants.CREATE_CONTACT_RESPONSE);
        if (con != null) {
            if (verbose)
                ToXML.encodeContact(response, ifmt, octxt, con, true, null);
            else
                response.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, con.getId());
        }
        return response;
    }

    static Pair<Map<String,Object>, List<Attachment>> parseContact(Element cn, ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
        return parseContact(cn, zsc, octxt, null);
    }

    /*
     * for CreateContact and ModifyContact replace mode
     */
    static Pair<Map<String,Object>, List<Attachment>> parseContact(
            Element cn, ZimbraSoapContext zsc, OperationContext octxt, Contact existing)
    throws ServiceException {
        Map<String, Object> fields = new HashMap<String, Object>();
        List<Attachment> attachments = new ArrayList<Attachment>();
        boolean isContactGroup = false;
        
        for (Element elt : cn.listElements(MailConstants.E_ATTRIBUTE)) {
            String name = elt.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
            if (name.trim().equals("")) {
                throw ServiceException.INVALID_REQUEST("at least one contact field name is blank", null);
            }
            
            // do not allow specifying groupMember as attribute directly
            disallowGroupMemberAttr(name);
            
            Attachment attach = parseAttachment(elt, name, zsc, octxt, existing);
            if (attach == null) {
                disallowOperation(elt);

                String value = elt.getText();
                StringUtil.addToMultiMap(fields, name, value);
                
                if (ContactConstants.A_type.equals(name) && ContactConstants.TYPE_GROUP.equals(value)) {
                    isContactGroup = true;
                }
            } else {
                attachments.add(attach);
            }
        }
        
        // parse contact group members
        ContactGroup contactGroup = null;
        for (Element elt : cn.listElements(MailConstants.E_CONTACT_GROUP_MEMBER)) {
            if (!isContactGroup) {
                // do not check existing contact, because this is replace mode or creating
                throw ServiceException.INVALID_REQUEST(MailConstants.E_CONTACT_GROUP_MEMBER +
                        " is only allowed for contact group", null);
            }
            
            disallowOperation(elt);
            
            if (contactGroup == null) {
                contactGroup = ContactGroup.init(existing, true);
                if (existing != null) {
                    contactGroup.removeAllMembers();
                }
            }
            String memberType = elt.getAttribute(MailConstants.A_CONTACT_GROUP_MEMBER_TYPE);
            String memberValue = elt.getAttribute(MailConstants.A_CONTACT_GROUP_MEMBER_VALUE);
                
            Member.Type type = Member.Type.fromSoap(memberType);
            contactGroup.addMember(type, memberValue);
        }
        if (contactGroup != null) {
            fields.put(ContactConstants.A_groupMember, contactGroup);
        }

        return new Pair<Map<String,Object>, List<Attachment>>(fields, attachments);
    }
    
    static void disallowOperation(Element elt) throws ServiceException {
        String opStr = elt.getAttribute(MailConstants.A_OPERATION, null);
        if (opStr != null) {
            throw ServiceException.INVALID_REQUEST(MailConstants.A_OPERATION +
                    " is not allowed", null);
        }
    }
    
    static void disallowGroupMemberAttr(String attrName) throws ServiceException {
        if (attrName.trim().equals(ContactConstants.A_groupMember)) {
            throw ServiceException.INVALID_REQUEST(ContactConstants.A_groupMember + 
                    " cannot be specified as an attribute", null);
        }
    }

    static Pair<ParsedContact.FieldDeltaList, List<Attachment>> parseContactMergeMode(
            Element cn, ZimbraSoapContext zsc, OperationContext octxt, Contact existing)
    throws ServiceException {
        ParsedContact.FieldDeltaList deltaList = new ParsedContact.FieldDeltaList();
        List<Attachment> attachments = new ArrayList<Attachment>();
        boolean isContactGroup = false;
        
        for (Element elt : cn.listElements(MailConstants.E_ATTRIBUTE)) {
            String name = elt.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
            if (name.trim().equals(""))
                throw ServiceException.INVALID_REQUEST("at least one contact field name is blank", null);

            Attachment attach = parseAttachment(elt, name, zsc, octxt, existing);
            if (attach == null) {
                String opStr = elt.getAttribute(MailConstants.A_OPERATION, null);
                ParsedContact.FieldDelta.Op op = FieldDelta.Op.fromString(opStr);
                String value = elt.getText();
                deltaList.addAttrDelta(name, value, op);
                
                if (ContactConstants.A_type.equals(name) && ContactConstants.TYPE_GROUP.equals(value) &&
                        ParsedContact.FieldDelta.Op.REMOVE != op) {
                    isContactGroup = true;
                }
            } else {
                attachments.add(attach);
            }
        }
        
        for (Element elt : cn.listElements(MailConstants.E_CONTACT_GROUP_MEMBER)) {
            if (!isContactGroup && !existing.isGroup()) {
                throw ServiceException.INVALID_REQUEST(MailConstants.E_CONTACT_GROUP_MEMBER +
                        " is only allowed for contact group", null);
            }
            
            String opStr = elt.getAttribute(MailConstants.A_OPERATION);
            ParsedContact.FieldDelta.Op op = FieldDelta.Op.fromString(opStr);
            
            ContactGroup.Member.Type memberType = 
                ContactGroup.Member.Type.fromSoap(elt.getAttribute(MailConstants.A_CONTACT_GROUP_MEMBER_TYPE, null));
            String memberValue = elt.getAttribute(MailConstants.A_CONTACT_GROUP_MEMBER_VALUE, null);
            
            if (memberType == null) {
                throw ServiceException.INVALID_REQUEST("missing member type", null);
            }
            if (StringUtil.isNullOrEmpty(memberValue)) {
                throw ServiceException.INVALID_REQUEST("missing member value", null);
            }
            
            deltaList.addGroupMemberDelta(memberType, memberValue, op);
        }

        return new Pair<ParsedContact.FieldDeltaList, List<Attachment>>(deltaList, attachments);
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
            MailItem item = itemId == -1 ? existing : getRequestedMailbox(zsc).getItemById(octxt, itemId, MailItem.Type.UNKNOWN);

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
                            if (mp == null) {
                                throw MailServiceException.NO_SUCH_PART(part);
                            }
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
                    if (part != null && !part.equals("")) {
                        throw MailServiceException.NO_SUCH_PART(part);
                    }
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

    public static List<Contact> createContacts(OperationContext oc, Mailbox mbox, ItemId iidFolder, List<ParsedContact> list, String[] tags)
    throws ServiceException {

        List<Contact> toRet = new ArrayList<Contact>();

        mbox.lock.lock();
        try {
            for (ParsedContact pc : list) {
                toRet.add(mbox.createContact(oc, pc, iidFolder.getId(), tags));
            }
        } finally {
            mbox.lock.release();
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
                if (!mbox.getAccountId().equals(iid.getAccountId())) {
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(iid.getAccountId());
                }
                Message msg = mbox.getMessageById(octxt, iid.getId());
                MimePart mp = Mime.getMimePart(msg.getMimeMessage(), part);
                String ctype = new ContentType(mp.getContentType()).getContentType();
                String fname = mp.getFileName();
                if (fname != null && (MimeConstants.CT_APPLICATION_OCTET_STREAM.equals(ctype) || MimeConstants.CT_APPLICATION_TNEF.equals(ctype))) {
                    String guess = MimeDetect.getMimeDetect().detect(fname);
                    if (guess != null) {
                        ctype = guess;
                    }
                }
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
    
    static boolean needToMigrateDlist(ZimbraSoapContext zsc) throws ServiceException {
        Version zcoZcbVersion = DocumentHandler.zimbraConnectorClientVersion(zsc);
        if (zcoZcbVersion != null) {
            // ZCO/ZCB version in that the new contact group API is supported
            // TODO: change 9.0.0 to the real ZCO/ZCB version when bug 61593 is 
            // implemented/fixed.
            Version newContactGroupAPISupported = new Version("9.0.0"); 
            if (zcoZcbVersion.compareTo(newContactGroupAPISupported) < 0) {
                return true;
            }
        }
        return false;
    }
    
    static void migrateFromDlist(ParsedContact pc) throws ServiceException {
        /*
         * replace groupMember with dlist data
         * 
         * Note: if the user had also used new clients to manipulate group members 
         *       all ref members will be lost, since all dlist members will be 
         *       migrated as INLINE members.
         *       if dlist is an empty string, the group will become an empty group.
         *       
         *       This is the expected behavior.
         */
        Map<String, String> fields = pc.getFields();
        String dlist = fields.get(ContactConstants.A_dlist);
        if (dlist != null) {
            ContactGroup contactGroup = ContactGroup.init();
            contactGroup.migrateFromDlist(dlist);
            fields.put(ContactConstants.A_groupMember, contactGroup.encode());
            fields.remove(ContactConstants.A_dlist);
        }
    }
}
