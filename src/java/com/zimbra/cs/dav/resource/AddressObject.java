/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;
import org.json.JSONException;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.property.CardDavProperty;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.formatter.VCard;

public class AddressObject extends MailItemResource {

    public static final String VCARD_EXTENSION = ".vcf";

    // for compatibility with Apple Address Book app.
    private static final String XABSKIND = "X-ADDRESSBOOKSERVER-KIND";
    private static final String XABSMEMBER = "X-ADDRESSBOOKSERVER-MEMBER";
    
    public AddressObject(DavContext ctxt, Contact item) throws ServiceException {
        super(ctxt, item);
        setProperty(DavElements.P_GETCONTENTTYPE, DavProtocol.VCARD_CONTENT_TYPE);
        setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(mId));
    }
    
    public boolean isCollection() {
        return false;
    }
    
    public ResourceProperty getProperty(Element prop) {
        if (prop.getQName().equals(DavElements.CardDav.E_ADDRESS_DATA)) {
            return CardDavProperty.getAddressbookData(prop, this);
        }
        return super.getProperty(prop);
    }
    
    protected InputStream getRawContent(DavContext ctxt) throws DavException, IOException {
        try {
            return new ByteArrayInputStream(toVCard(ctxt).getBytes("UTF-8"));
        } catch (ServiceException e) {
            ZimbraLog.dav.warn("can't get content for Contact %d", mId, e);
        }
        return null;
    }
    
    public String toVCard(DavContext ctxt) throws ServiceException, DavException {
        Contact contact = (Contact)getMailItem(ctxt);
        return VCard.formatContact(contact, null, true).formatted;
    }
    public String toVCard(DavContext ctxt, java.util.Collection<String> attrs) throws ServiceException, DavException {
        if (attrs == null || attrs.isEmpty())
            return toVCard(ctxt);
        Contact contact = (Contact)getMailItem(ctxt);
        return VCard.formatContact(contact, attrs, true).formatted;
    }
    
    public static DavResource create(DavContext ctxt, String name, Collection where) throws DavException, IOException {
        FileUploadServlet.Upload upload = ctxt.getUpload();
        String buf = new String(ByteUtil.getContent(upload.getInputStream(), (int)upload.getSize()), MimeConstants.P_CHARSET_UTF8);
        Mailbox mbox = null;
        DavResource res = null;
        try {
            String url = ctxt.getItem();
            if (url.endsWith(".vcf"))
                url = url.substring(0, url.length()-4);
            mbox = where.getMailbox(ctxt);
            for (VCard vcard : VCard.parseVCard(buf)) {
                if (vcard.fields.isEmpty())
                    continue;
                vcard.fields.put(ContactConstants.A_vCardURL, url);
                String uid = vcard.uid;
                Account ownerAccount = Provisioning.getInstance().getAccountById(where.mOwnerId);
                // expand apple address book groups
                if (ownerAccount != null && ownerAccount.isPrefContactsExpandAppleContactGroups()) {
                    Map<String,String> xprops = Contact.decodeXProps(vcard.fields.get(ContactConstants.A_vCardXProps));
                    String kind = xprops.get(XABSKIND);
                    String memberList = xprops.get(XABSMEMBER);
                    if (kind != null && kind.compareTo("group") == 0 && memberList != null) {
                        ArrayList<String> memberEmails = new ArrayList<String>();
                        try {
                            String[] members = Contact.parseMultiValueAttr(memberList);
                            for (String uidStr : members) {
                                if (uidStr.startsWith("urn:uuid:"))
                                    uidStr = uidStr.substring(9);
                                Contact c = getContactByUID(ctxt, uidStr, ownerAccount);
                                if (c != null) {
                                    memberEmails.addAll(c.getEmailAddresses());
                                }
                                
                            }
                        } catch (JSONException e) {
                            ZimbraLog.dav.debug("can't parse xprop %s", memberList, e);
                        }
                        vcard.fields.put(ContactConstants.A_type, ContactConstants.TYPE_GROUP);
                        vcard.fields.put(ContactConstants.A_dlist, StringUtil.join(",", memberEmails));
                        vcard.fields.remove(ContactConstants.A_vCardXProps);
                    }
                }
                Contact c = null;
                // check for existing contact
                if (uid != null) {
                    vcard.fields.put(ContactConstants.A_vCardUID, uid);
                    res = UrlNamespace.getResourceAt(ctxt, ctxt.getUser(), ctxt.getPath());
                }
                if (res == null) {
                    String ifnonematch = ctxt.getRequest().getHeader(DavProtocol.HEADER_IF_NONE_MATCH);
                    if (ifnonematch == null)
                        throw new DavException("item does not exists", HttpServletResponse.SC_CONFLICT);
                    c = mbox.createContact(ctxt.getOperationContext(), vcard.asParsedContact(), where.mId, null);
                    res = new AddressObject(ctxt, c);
                    res.mNewlyCreated = true;
                } else {
                    String etag = ctxt.getRequest().getHeader(DavProtocol.HEADER_IF_MATCH);
                    String itemEtag = res.getEtag();
                    if (etag != null && !etag.equals(itemEtag))
                        throw new DavException("item etag does not match", HttpServletResponse.SC_CONFLICT);
                    mbox.modifyContact(ctxt.getOperationContext(), ((MailItemResource)res).getId(), vcard.asParsedContact());
                    res = UrlNamespace.getResourceAt(ctxt, ctxt.getUser(), ctxt.getPath());
                }
            }
        } catch (ServiceException e) {
            throw new DavException("cannot parse vcard ", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
        return res;
    }
    
    public static AddressObject getAddressObjectByUID(DavContext ctxt, String uid, Account account) throws ServiceException {
        Contact c = getContactByUID(ctxt, uid, account);
        if (c == null)
            return null;
        return new AddressObject(ctxt, c);
    }
    
    private static Contact getContactByUID(DavContext ctxt, String uid, Account account) throws ServiceException {
        Contact item = null;
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        if (uid.endsWith(AddressObject.VCARD_EXTENSION))
            uid = uid.substring(0, uid.length() - AddressObject.VCARD_EXTENSION.length());
        if (uid.indexOf('%') > 0) {
            try {
                uid = URLDecoder.decode(uid, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                ZimbraLog.dav.warn("Can't decode UID %s", uid);
            }
        }
        int index = uid.indexOf(':');
        if (index > 0) {
            item = mbox.getContactById(ctxt.getOperationContext(), Integer.parseInt(uid.substring(index+1)));
        } else {
            ZimbraQueryResults zqr = null;
            StringBuilder query = new StringBuilder();
            query.append("#").append(ContactConstants.A_vCardUID).append(":");
            query.append(uid);
            query.append(" OR ").append("#").append(ContactConstants.A_vCardURL).append(":");
            query.append(uid);
            ZimbraLog.dav.debug("query %s", query.toString());
            try {
                zqr = mbox.search(ctxt.getOperationContext(), query.toString(), new byte[] { MailItem.TYPE_CONTACT }, SortBy.NAME_ASCENDING, 10);
                if (zqr.hasNext()) {
                    ZimbraHit hit = zqr.getNext();
                    if (hit instanceof ContactHit) {
                        item = ((ContactHit)hit).getContact();
                    }
                }
            } catch (Exception e) {
                ZimbraLog.dav.error("can't search for: uid="+uid, e);
            } finally {
                if (zqr != null)
                    try {
                        zqr.doneWithSearchResults();
                    } catch (ServiceException e) {}
            }
        }
        return item;
    }
}
