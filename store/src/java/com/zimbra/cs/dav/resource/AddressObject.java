/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;

import com.google.common.collect.ListMultimap;
import com.google.common.io.Closeables;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.UUIDUtil;
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
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.cs.mailbox.ContactGroup.Member;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.VCardParamsAndValue;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.service.util.ItemId;

public class AddressObject extends MailItemResource {

    public static final String VCARD_EXTENSION = ".vcf";

    // for compatibility with Apple Address Book app.
    public static final String XABSKIND = "X-ADDRESSBOOKSERVER-KIND";
    public static final String XABSMEMBER = "X-ADDRESSBOOKSERVER-MEMBER";

    public AddressObject(DavContext ctxt, Contact item) throws ServiceException {
        super(ctxt, item);
        setProperty(DavElements.P_GETCONTENTTYPE, DavProtocol.VCARD_CONTENT_TYPE);

        // size is approximate.  it just has to be non-zero as the actual content
        // will be chunked to the client in GET response.
        setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(item.getFields().size()));
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public ResourceProperty getProperty(Element prop) {
        if (prop.getQName().equals(DavElements.CardDav.E_ADDRESS_DATA)) {
            return CardDavProperty.getAddressbookData(prop, this);
        }
        return super.getProperty(prop);
    }

    @Override
    public InputStream getContent(DavContext ctxt) throws DavException, IOException {
        try {
            return new ByteArrayInputStream(toVCard(ctxt).getBytes("UTF-8"));
        } catch (ServiceException e) {
            ZimbraLog.dav.warn("can't get content for Contact %d", mId, e);
        }
        return null;
    }

    private static void constructContactGroupFromAppleXProps(DavContext ctxt, Account ownerAccount, VCard vcard,
            Contact existingContact, int folderId) {
        ListMultimap<String, VCardParamsAndValue> xprops =
                Contact.decodeUnknownVCardProps(vcard.fields.get(ContactConstants.A_vCardXProps));
        String kind = VCardParamsAndValue.getFirstValue(XABSKIND, xprops);
        if (kind != null && kind.compareTo("group") == 0) {
            ContactGroup contactGroup;
            List<VCardParamsAndValue> xabsmembers = xprops.get(XABSMEMBER);
            try {
                if (existingContact == null) { // create
                    contactGroup = ContactGroup.init();
                } else { // modify
                    contactGroup = ContactGroup.init(existingContact, true);
                    // remove all the contacts of type CONTACT_REF that belong to the collection same as the group
                    ArrayList<Member> membersToRemove = new ArrayList<Member>();
                    for (Member member : contactGroup.getMembers()) {
                        if (Member.Type.CONTACT_REF.equals(member.getType())) {
                            ItemId itemId = new ItemId(member.getValue(), existingContact.getAccount().getId());
                            if (itemId.belongsTo(existingContact.getAccount())) {
                                // make sure member belongs to the same collection as the group.
                                Contact c = getContactByUID(ctxt, itemId.toString(), existingContact.getAccount(),
                                        folderId);
                                if (c != null) {
                                    membersToRemove.add(member);
                                }
                            }
                        }
                    }
                    for (Member member : membersToRemove) {
                        contactGroup.removeMember(member.getType(), member.getValue());
                    }
                }
                for (VCardParamsAndValue memberProp : xabsmembers) {
                    String member = memberProp.getValue();
                    if (member.startsWith("urn:uuid:")) {
                        member = member.substring(9);
                    }
                    Contact c = getContactByUID(ctxt, member, ownerAccount, folderId);
                    if (c != null) {
                        // add to the group as a CONTACT_REF
                        ItemId itemId = new ItemId(c);
                        contactGroup.addMember(Member.Type.CONTACT_REF, itemId.toString());
                    }
                }

                vcard.fields.put(ContactConstants.A_type, ContactConstants.TYPE_GROUP);
                vcard.fields.put(ContactConstants.A_groupMember, contactGroup.encode());
                // remove the Apple x-props and preserve the rest.
                xprops.removeAll(XABSKIND);
                xprops.removeAll(XABSMEMBER);
                vcard.fields.put(ContactConstants.A_vCardXProps, Contact.encodeUnknownVCardProps(xprops));
            } catch (ServiceException e) {
                ZimbraLog.dav.debug("can't parse xprop %s", xabsmembers, e);
            }
        }
    }

    private static void populateContactGroupAppleXProps(DavContext ctxt, Contact contact) {
        if (contact.isContactGroup() == false) {
            return;
        }
        ContactGroup contactGroup = null;
        try {
            contactGroup = ContactGroup.init(contact.get(ContactConstants.A_groupMember), contact.getMailbox().getAccountId());
        } catch (ServiceException e) {
            ZimbraLog.dav.warn("can't get group members for Contact %d", contact.getId(), e);
        }

        ListMultimap<String, VCardParamsAndValue> xprops = contact.getUnknownVCardProps();
        xprops.put(XABSKIND, new VCardParamsAndValue("group"));
        if (contactGroup != null) {
            try {
                for (Member member : contactGroup.getMembers()) {
                    if (member.getType().equals(Member.Type.CONTACT_REF)) {
                        ItemId itemId = new ItemId(member.getValue(), contact.getAccount().getId());
                        if (itemId.belongsTo(contact.getAccount())) {
                            // make sure member belongs to the same collection as the group.
                            Contact c = getContactByUID(ctxt, itemId.toString(), contact.getAccount(),
                                    contact.getFolderId());
                            if (c != null) {
                                xprops.put(XABSMEMBER,
                                        new VCardParamsAndValue("urn:uuid:" + VCard.getUid(c)));
                            }
                        }
                    }
                }
            } catch (ServiceException e) {
                ZimbraLog.dav.warn("can't create group members xprops for Contact %d", contact.getId(), e);
            }
        }
        contact.setUnknownVCardProps(xprops);
    }

    public String toVCard(DavContext ctxt) throws ServiceException, DavException {
        Contact contact = (Contact)getMailItem(ctxt);
        populateContactGroupAppleXProps(ctxt, contact);
        return VCard.formatContact(contact, null, true, false).getFormatted();
    }
    public String toVCard(DavContext ctxt, java.util.Collection<String> attrs) throws ServiceException, DavException {
        if (attrs == null || attrs.isEmpty())
            return toVCard(ctxt);
        Contact contact = (Contact)getMailItem(ctxt);
        populateContactGroupAppleXProps(ctxt, contact);
        return VCard.formatContact(contact, attrs, true).getFormatted();
    }

    public static boolean acceptableVCardContentType(String contentType, boolean nullIsOk) {
        if (contentType == null) {
            return nullIsOk;
        }
        return (contentType.startsWith(DavProtocol.VCARD_CONTENT_TYPE) ||
                 contentType.startsWith(MimeConstants.CT_TEXT_VCARD_LEGACY) ||
                 contentType.startsWith(MimeConstants.CT_TEXT_VCARD_LEGACY2));
    }

    public static VCard uploadToVCard(DavContext ctxt)
    throws DavException, IOException {
        FileUploadServlet.Upload upload = ctxt.getUpload();
        String contentType = upload.getContentType();
        if (!acceptableVCardContentType(contentType, true)) {
            throw new DavException.InvalidData(DavElements.CardDav.E_SUPPORTED_ADDRESS_DATA,
                    String.format("Incorrect Content-Type '%s', expected '%s'",
                            contentType, DavProtocol.VCARD_CONTENT_TYPE));
        }
        long uploadSize = upload.getSize();
        if (uploadSize <= 0) {
            throw new DavException.InvalidData(DavElements.CardDav.E_VALID_ADDRESS_DATA,"empty request");
        }
        List<VCard> vcards = null;
        try (InputStream is = ctxt.getUpload().getInputStream()) {
            String buf = new String(ByteUtil.getContent(is, (int)uploadSize), MimeConstants.P_CHARSET_UTF8);
            vcards = VCard.parseVCard(buf);
        } catch (ServiceException se) {
            throw new DavException.InvalidData(DavElements.CardDav.E_VALID_ADDRESS_DATA,
                    String.format("Problem parsing %s data - %s", DavProtocol.VCARD_CONTENT_TYPE, se.getMessage()));
        }
        if (vcards == null) {
            throw new DavException.InvalidData(DavElements.CardDav.E_VALID_ADDRESS_DATA,
                    String.format("Problem parsing %s data - no cards produced.", DavProtocol.VCARD_CONTENT_TYPE));
        }
        if (vcards.size() != 1) {
            throw new DavException.InvalidData(DavElements.CardDav.E_VALID_ADDRESS_DATA,
                    String.format("Problem parsing %s data - %d cards produced, only 1 allowed.",
                            DavProtocol.VCARD_CONTENT_TYPE, vcards.size()));
        }
        VCard vcard = vcards.get(0);
        if (vcard.uid == null) {
            vcard.uid = UUIDUtil.generateUUID();
        }
        return vcard;
    }

    /**
     * @param name Preferred DAV basename for the new item - including ".vcf".
     * @param allowUpdate - PUTs are allowed to update a pre-existing item.  POSTs to the containing collection are not.
     */
    public static DavResource create(DavContext ctxt, String name, Collection where, boolean allowUpdate)
    throws DavException, IOException {
        VCard vcard = uploadToVCard(ctxt);
        return create(ctxt, name, where, vcard, allowUpdate);
    }

    /**
     * @param name Preferred DAV basename for the new item - including ".vcf".
     * @param allowUpdate - PUTs are allowed to update a pre-existing item.  POSTs to the containing collection are not.
     */
    public static DavResource create(DavContext ctxt, String name, Collection where, VCard vcard, boolean allowUpdate)
    throws DavException, IOException {
        if (vcard.fields.isEmpty()) {
            throw new DavException.InvalidData(DavElements.CardDav.E_VALID_ADDRESS_DATA,
                    String.format("Problem parsing %s data - no fields found.", DavProtocol.VCARD_CONTENT_TYPE));
        }
        String baseName = HttpUtil.urlUnescape(name);
        DavResource res = null;
        boolean useEtag = allowUpdate;
        StringBuilder pathSB = new StringBuilder(where.getUri());
        if (pathSB.charAt(pathSB.length() -1) != '/') {
            pathSB.append('/');
        }
        pathSB.append(baseName);
        String path = pathSB.toString();
        try {
            if (name.endsWith(AddressObject.VCARD_EXTENSION)) {
                name = name.substring(0, name.length()-4);
                name = HttpUtil.urlUnescape(name);
            }
            Mailbox mbox = where.getMailbox(ctxt);
            vcard.fields.put(ContactConstants.A_vCardURL, name);
            String uid = vcard.uid;
            Account ownerAccount = Provisioning.getInstance().getAccountById(where.mOwnerId);
            Contact c = null;
            // check for existing contact
            if (uid != null) {
                vcard.fields.put(ContactConstants.A_vCardUID, uid);
                AddressObject ao = getAddressObjectByUID(ctxt, uid, ctxt.getTargetMailbox().getAccount(),
                            ctxt.getTargetMailbox().getFolderById(ctxt.getOperationContext(), where.mId), baseName);
                if (ao != null) {
                    if (!allowUpdate) {
                        throw new DavException.CardDavUidConflict(
                                // "An item with the same UID already exists in the address book", ao.getUri());
                                "An item with the same UID already exists in the address book",
                                ao.getHref());
                    }
                    if (path.equals(ao.getUri())) {
                        res = ao;
                    } else {
                        throw new DavException.CardDavUidConflict(
                                // "An item with the same UID already exists in the address book", ao.getUri());
                                "An item with the same UID already exists in the address book",
                                ao.getHref());
                    }
                } else {
                    res = UrlNamespace.getResourceAt(ctxt, ctxt.getUser(), path);
                }
            }
            if (res == null) {
                // Convert Apple contact group to Zimbra contact group.
                constructContactGroupFromAppleXProps(ctxt, ownerAccount, vcard, null, where.getId());
                c = mbox.createContact(ctxt.getOperationContext(), vcard.asParsedContact(), where.mId, null);
                res = new AddressObject(ctxt, c);
                res.mNewlyCreated = true;
            } else {
                String etag = null;
                if (useEtag) {
                    etag = ctxt.getRequest().getHeader(DavProtocol.HEADER_IF_MATCH);
                    useEtag = (etag != null);
                }

                String itemEtag = res.getEtag();
                if (useEtag && (etag != null) && !etag.equals(itemEtag)) {
                    throw new DavException("item etag does not match", HttpServletResponse.SC_PRECONDITION_FAILED);
                }
                String ifnonematch = ctxt.getRequest().getHeader(DavProtocol.HEADER_IF_NONE_MATCH);
                if ((ifnonematch != null) && ifnonematch.equals("*")) {
                    throw new DavException("item already exists", HttpServletResponse.SC_PRECONDITION_FAILED);
                }
                MailItemResource mir = (MailItemResource) res;
                constructContactGroupFromAppleXProps(
                        ctxt, ownerAccount, vcard, (Contact) mir.getMailItem(ctxt), where.getId());
                vcard.merge((Contact) mir.getMailItem(ctxt));
                mbox.modifyContact(ctxt.getOperationContext(), mir.getId(), vcard.asParsedContact());
                res = UrlNamespace.getResourceAt(ctxt, ctxt.getUser(), path);
            }
        } catch (ServiceException e) {
            ZimbraLog.dav.info("Problem parsing VCARD", e);
            throw new DavException.InvalidData(DavElements.CardDav.E_VALID_ADDRESS_DATA,
                    String.format("cannot parse vcard - %s", e.getMessage()), e);
        }
        if (res == null) {
            throw new DavException.InvalidData(DavElements.CardDav.E_VALID_ADDRESS_DATA, "cannot parse vcard");
        }
        return res;
    }

    public static AddressObject getAddressObjectByUID(DavContext ctxt, String uid, Account account, Folder f)
    throws ServiceException {
        return getAddressObjectByUID(ctxt, uid, account, f, null /* preferredBaseName */);
    }

    /**
     * @param preferredBaseName If more than one item matches the UID, prefer one matching this name - can be null
     */
    public static AddressObject getAddressObjectByUID(DavContext ctxt, String uid, Account account, Folder f,
            String preferredBaseName)
    throws ServiceException {
        Contact c = getContactByUID(ctxt, uid, account, (f != null) ? f.getId() : -1, preferredBaseName);
        if (c == null)
            return null;
        return new AddressObject(ctxt, c);
    }

    public static AddressObject getAddressObjectByUID(DavContext ctxt, String uid, Account account)
    throws ServiceException {
        return getAddressObjectByUID(ctxt, uid, account, null);
    }

    private static Contact getContactByUID(DavContext ctxt, String uid, Account account, int folderId)
    throws ServiceException {
        return getContactByUID(ctxt, uid, account, folderId, null /* preferredBaseName */);
    }

    /**
     * @param preferredBaseName If more than one item matches the UID, prefer one matching this name - can be null
     */
    private static Contact getContactByUID(DavContext ctxt, String uid, Account account, int folderId,
            String preferredBaseName)
    throws ServiceException {
        Contact item = null;
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        if (uid.endsWith(AddressObject.VCARD_EXTENSION)) {
            uid = uid.substring(0, uid.length() - AddressObject.VCARD_EXTENSION.length());
            // Unescape the name (It was encoded in DavContext intentionally)
            uid = HttpUtil.urlUnescape(uid);
        }
        // first check whether the UID is an encoding of the Contact ID
        int id = 0;
        int index = uid.indexOf(':');
        if (index > 0) {
            String accountId = uid.substring(0, index);
            try {
                if (accountId.equals(account.getId()))
                    id = Integer.parseInt(uid.substring(index+1));
            } catch (NumberFormatException e) {
            }
        }
        if (id > 0) {
            item = mbox.getContactById(ctxt.getOperationContext(), Integer.parseInt(uid.substring(index+1)));
        } else {
            StringBuilder query = new StringBuilder();
            query.append("#").append(ContactConstants.A_vCardUID).append(":");
            // escape the double quotes in uid and surround with double quotes
            query.append("\"").append(uid.replace("\"", "\\\"")).append("\"");
            query.append(" OR ").append("#").append(ContactConstants.A_vCardURL).append(":");
            query.append("\"").append(uid.replace("\"", "\\\"")).append("\"");
            ZimbraLog.dav.debug("query %s", query.toString());
            try (ZimbraQueryResults zqr = mbox.index.search(ctxt.getOperationContext(), query.toString(),
                EnumSet.of(MailItem.Type.CONTACT), SortBy.NAME_ASC, 10)){
                // There could be multiple contacts with the same UID from different collections.
                item = getMatchingHit(zqr, folderId, preferredBaseName);
            } catch (Exception e) {
                ZimbraLog.dav.error("can't search for: uid=%s", uid, e);
            }
        }

        if ((item != null) && (folderId >= 0) && (item.getFolderId() != folderId))
            item = null;

        return item;
    }

    /**
     * There could be multiple contacts with the same UID from different collections.
     * Due to an old CardDAV bug, it is also possible there may be more than one contact with the same UID in
     * the same collection.  Using "preferredBaseName" ensures that CardDAV clients can still do an
     * update in that case.
     *
     * @param zqr
     * @param folderId
     * @param preferredBaseName If more than one item matches, prefer one matching this name - can be null
     *        Ignored if folderId < 0
     * @return
     * @throws ServiceException
     */
    private static Contact getMatchingHit(ZimbraQueryResults zqr, int folderId, String preferredBaseName)
    throws ServiceException {
        Contact item = null;
        Contact firstMatchingItem = null;
        while (zqr.hasNext()) {
            ZimbraHit hit = zqr.getNext();
            if (hit instanceof ContactHit) {
                item = ((ContactHit)hit).getContact();
                if (folderId < 0) {
                    break;
                }
                if (item.getFolderId() == folderId) {
                    if (firstMatchingItem == null) {
                        firstMatchingItem = item;
                    }
                    if (preferredBaseName != null) {
                        String contactBaseName = VCard.getUrl(item) + AddressObject.VCARD_EXTENSION;
                        if (!preferredBaseName.equals(contactBaseName)) {
                            item = null;
                            continue;
                        }
                    }
                    break;
                }
                item = null;
            }
        }
        return item != null ? item : firstMatchingItem;
    }

}