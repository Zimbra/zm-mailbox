/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016, 2018 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.common.collect.TreeMultimap;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchResultCallback;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.type.GalSearchType;

public class ContactGroup {
    
    // metadata keys for ContactGroup 
    private enum MetadataKey {
        MEMBERS("m");
        
        private String key;
        MetadataKey(String key) {
            this.key = key;
        }
        
        private String getKey() {
            return key;
        }
    }

    // 
    // ordered Set, order is the order in which keys were inserted into the map (insertion-order). 
    // Note that insertion order is not affected if a key is re-inserted into the map. 
    // We need to maintain the order members are added to the group (do we?), and
    // need to be able to quickly get a member by a unique key.
    //
    // In members are persisted in MetadataList, which is ordered.
    private Set<Member> members = new LinkedHashSet<Member>();  // ordered Set
    
    // never persisted
    // contains derefed members sorted by the Member.getKey() order
    private TreeMultimap<String, Member> derefedMembers = null;
    
    public static ContactGroup init(Contact contact, boolean createIfNotExist) 
    throws ServiceException {
        ContactGroup contactGroup = null;
        if (contact != null) {
            String encoded = contact.get(ContactConstants.A_groupMember);
            if (encoded != null) {
                contactGroup = init(encoded, contact.getMailbox().getAccountId() );
            }
        }
        
        if (contactGroup == null && createIfNotExist) {
            contactGroup = init();
        }
        return contactGroup;
    }
    
    public static ContactGroup init(String encoded, String ownerAcctId) throws ServiceException {
        return ContactGroup.decode(encoded, ownerAcctId );
    }
    
    public static ContactGroup init() throws ServiceException {
        return new ContactGroup();
    }
    
    private boolean isDerefed() {
        return derefedMembers != null;
    }

    public boolean hasMembers() {
        return (members.size() > 0);
    }

    public void removeAllMembers() {
        members.clear();
        derefedMembers = null;
    }
    
    /*
     * return members in the order they were inserted
     */
    public List<Member> getMembers() {
        return Collections.unmodifiableList(Arrays.asList(members.toArray(new Member[members.size()])));
    }
    
    public List<Member> getMembers(boolean preferDerefed) {
        if (preferDerefed && isDerefed()) {
            return getDerefedMembers();
        } else {
            return getMembers();
        }
    }
    
    /*
     * return derefed members in Member.getKey() order
     */
    public List<Member> getDerefedMembers() {
        assert(isDerefed());
        return Collections.unmodifiableList(Arrays.asList(derefedMembers.values().toArray(new Member[derefedMembers.size()])));
    }
    
    // create and add member at the end of the list
    public Member addMember(Member.Type type, String value) throws ServiceException {
        Member member = Member.init(type, value);
        if (members.contains(member)) {
            throw ServiceException.INVALID_REQUEST("member already exists: " + "(" + 
                    type.soapEncoded + ")" + " " + value, null);
        } 
        addMember(member);
        return member;
    }
    
    public void removeMember(Member.Type type, String value) throws ServiceException {
        Member member = Member.init(type, value);
        if (!members.contains(member)) {
            throw ServiceException.INVALID_REQUEST("no such member: " + "(" + 
                    type.soapEncoded + ")" + " " + value, null);
        }
        members.remove(member);
    }
    
    // for legacy clients
    // migrate from a dlist value to the new group format
    // if dlist is an empty string, it means to remove all members.
    public void migrateFromDlist(String dlist) throws ServiceException {
        String before = dump();
        removeAllMembers();
        MigrateContactGroup.migrate(this, dlist);

        ZimbraLog.contact.info("in-place migrated contact group from dlist: dlist=[%s], groupMember before migrate=[%s], groupMember after migrate=[%s]", 
                dlist, before, dump());
    }
    
    // for legacy clients
    public String migrateToDlist(Mailbox mbox, OperationContext octxt) 
    throws ServiceException {
        List<String> addrs = getEmailAddresses(false, mbox, octxt, false);
        if (addrs.isEmpty()) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String addr : addrs) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(addr);
            }
            
            if (ZimbraLog.contact.isDebugEnabled()) {
                ZimbraLog.contact.debug("returned contact group as dlist: dlist=[%s], groupMember=[%s]", 
                        sb.toString(), dump());
            }
            
            return sb.toString();
        }
        
        
    }
    
    private void addMember(Member member) {
        members.add(member);
    }

    
    /*
     * Note: deref each time when called, result is not cached
     * 
     * Return all members expanded in key order.  
     * Key is:
     *  for CONTACT_REF: the fileAs field of the Contact
     *  for GAL_REF: email address of the GAL entry
     *  for CONTACT_REF: the value
     */
    public void derefAllMembers(Mailbox mbox, OperationContext octxt) {
        derefAllMembers(mbox, octxt, SoapProtocol.Soap12);
    }
    
    public void derefAllMembers(Mailbox mbox, OperationContext octxt, SoapProtocol proxyProtocol) {
        derefedMembers = TreeMultimap.create();
        
        for (Member member : members) {
            member.derefMember(mbox, octxt, proxyProtocol);
            if (member.derefed()) {
                String key = member.getDerefedKey();
                derefedMembers.put(key, member);
            } else {
                ZimbraLog.contact.debug("contact group member cannot be derefed: " + member.getValue());
                derefedMembers.put(member.getValue(), member);
            }
        }
    }
    
    public List<String> getInlineEmailAddresses() {
        return getEmailAddresses(false, null, null, true);
    }

    public List<String> getEmailAddresses(boolean refresh, Mailbox mbox, 
            OperationContext octxt, boolean inlineMembersOnly) {
        List<String> result = new ArrayList<String>();
        
        if (inlineMembersOnly) {
            for (Member member : members) {
                if (Member.Type.INLINE == member.getType()) {
                    result.add(member.getValue());
                }
            }
        } else {
            if (refresh || !isDerefed()) {
                derefAllMembers(mbox, octxt);
            }
            
            for (Member member : members) {
                member.getDerefedEmailAddresses(result);
            }
        }
        return result;
    }
    
    public String encode() {
        Metadata encoded = new Metadata();
        
        MetadataList encodedMembers = new MetadataList();
        for (Member member : members) {
            encodedMembers.add(member.encode());
        }
        
        encoded.put(MetadataKey.MEMBERS.getKey(), encodedMembers);
        
        return encoded.toString();
    }
    
    private static ContactGroup decode(String encodedStr, String ownerAcctId) throws ServiceException {
        try {
            Metadata encoded = new Metadata(encodedStr);

            ContactGroup contactGroup = new ContactGroup();
            
            MetadataList members = encoded.getList(MetadataKey.MEMBERS.getKey());
            if (members == null) {
                throw ServiceException.FAILURE("missing members in metadata", null);
            }
            
            List<Metadata> memberList = members.asList();
            for (Metadata encodedMember : memberList) {
                Member member = Member.decode(encodedMember, ownerAcctId);
                contactGroup.addMember(member);
            }
            return contactGroup;
            
        } catch (ServiceException e) {
            ZimbraLog.contact.warn("unabale to decode contact group", e);
            throw e;
        }
        
    }
    
    private String dump() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Member member : members) {
            if (!first) {
                sb.append(", ");
            } else {
                first = false;
            }
            sb.append(member.getType().getMetaDataEncoded() + ":" + member.getValue());
        }
        return sb.toString();
    }
    
    /*
     *======================
     * Group Member classes
     *======================
     */
    public static abstract class Member implements Comparable<Member> {
        
        // metadata keys for member data
        private enum MetadataKey {
            ID("id"),
            TYPE("t"),
            VALUE("v");
            
            private String key;
            MetadataKey(String key) {
                this.key = key;
            }
            
            private String getKey() {
                return key;
            }
        }
        
        // type encoded/stored in metadata - do not change the encoded value
        public static enum Type {
            CONTACT_REF("C", ContactConstants.GROUP_MEMBER_TYPE_CONTACT_REF, "memberC"),  // ContactRefMember
            GAL_REF("G", ContactConstants.GROUP_MEMBER_TYPE_GAL_REF, "memberG"),          // ContactRefMember
            INLINE("I", ContactConstants.GROUP_MEMBER_TYPE_INLINE, "memberI");            // InlineMember
            
            private String metadataEncoded;
            private String soapEncoded;
            private String delimittedFieldsEncoded;
            
            Type(String metadataEncoded, String soapEncoded, String delimittedFieldsEncoded) {
                this.metadataEncoded = metadataEncoded;
                this.soapEncoded = soapEncoded;
                this.delimittedFieldsEncoded = delimittedFieldsEncoded;
            }
            
            private String getMetaDataEncoded() {
                return metadataEncoded;
            }
            
            public String getSoapEncoded() {
                return soapEncoded;
            }
            
            public String getDelimittedFieldsEncoded() {
                return delimittedFieldsEncoded;
            }
            
            private static Type fromMetadata(String metadataEncoded) throws ServiceException {
                if (CONTACT_REF.getMetaDataEncoded().equals(metadataEncoded)) {
                    return CONTACT_REF;
                } else if (GAL_REF.getMetaDataEncoded().equals(metadataEncoded)) {
                    return GAL_REF;
                } else if (INLINE.getMetaDataEncoded().equals(metadataEncoded)) {
                    return INLINE;
                }
                
                throw ServiceException.FAILURE("Unrecognized member type: " + metadataEncoded, null);
            }
            
            public static Type fromSoap(String soapEncoded) throws ServiceException {
                if (soapEncoded == null) {
                    return null;
                } else if (CONTACT_REF.getSoapEncoded().equals(soapEncoded)) {
                    return CONTACT_REF;
                } else if (GAL_REF.getSoapEncoded().equals(soapEncoded)) {
                    return GAL_REF;
                } else if (INLINE.getSoapEncoded().equals(soapEncoded)) {
                    return INLINE;
                }
                
                throw ServiceException.INVALID_REQUEST("Unrecognized member type: " + soapEncoded, null);
            }
        }
        
        private static Member init(Member.Type type, String value) throws ServiceException {
            Member member = null;
            switch (type) {
                case CONTACT_REF:
                    member = new ContactRefMember(value);
                    break;
                case GAL_REF:  
                    member = new GalRefMember(value);
                    break;
                case INLINE: 
                    member = new ContactGroup.InlineMember(value);
                    break;
                default:
                    throw ServiceException.INVALID_REQUEST("Unrecognized member type: " + type.name(), null);
            }
            return member;
        }
        
        protected String value;
        private String derefedKey; // key for sorting in the expanded group
        private List<String> derefedEmailAddrs; // derefed email addresses of the member
        private Object derefedObject;
        
        public abstract Type getType();

        
        // load the actual entry
        protected abstract void deref(Mailbox mbox, OperationContext octxt, 
                SoapProtocol proxyProtocol) throws ServiceException;  
        
        protected Member(String value) throws ServiceException {
            setValue(value);
        }
        
        private String getKey() {
            return getType().getMetaDataEncoded() + value;
        }
        
        /*
         * Needed for determining equality in our members Set.
         */
        @Override
        public int hashCode() { 
            return getKey().hashCode(); 
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Member) {
                Member otherMember = (Member) other;
                return getKey().equals( otherMember.getKey() );
            } else {
                return false;
            }
        }
        
        
        /*
         * called from TreeMultimap (ContactGroup.derefAllMembers) when two 
         * derefed keys are the same.  return key order in this case.
         * 
         * Note: must not return 0 here, if we do, the member will not be 
         *       inserted into the TreeMultimap.
         */
        @Override
        public int compareTo(Member other) {
            return getKey().compareTo(other.getKey());
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDerefedKey() {
            assert(derefed());
            return derefedKey;
        }
        
        // if result is not null, append email addresses of the member into result
        // if result is null, create a new List filled with email addresses of the member
        // return the List into which email addresses are added
        private List<String> getDerefedEmailAddresses(List<String> result) {
            assert(derefed());
            if (result == null) {
                result = new ArrayList<String>();
            }
            
            if (derefedEmailAddrs != null) {
                result.addAll(derefedEmailAddrs);
            }
            return result;
        }
        
        public Object getDerefedObj() {
            return derefedObject;
        }
        
        protected boolean derefed() {
            return derefedObject != null;
        }
        
        private void derefMember(Mailbox mbox, OperationContext octxt, SoapProtocol proxyProtocol) {
            if (!derefed()) {
                try {
                    deref(mbox, octxt, proxyProtocol);
                } catch (ServiceException e) {
                    // log and continue
                    ZimbraLog.contact.warn("unable to deref contact group member: " + value, e);
                }
            }
        }
        
        private void setValue(String value) throws ServiceException {
            if (StringUtil.isNullOrEmpty(value)) {
                throw ServiceException.INVALID_REQUEST("missing value", null);
            }
            this.value = value;
        }
        
        private void setDerefedKey(String key) {
            if (key == null) {
                key = "";
            }
            this.derefedKey = key;
        }
        
        private void setDerefedEmailAddrs(List<String> emaiLAddrs) {
            this.derefedEmailAddrs = emaiLAddrs;
        }
        
        protected void setDerefedObject(Object obj, String key, List<String> emailAddrs) {
            if (obj != null) {
                setDerefedKey(key);
                setDerefedEmailAddrs(emailAddrs);
            }
            this.derefedObject = obj;
        }
        
        private Metadata encode() {
            Metadata encoded = new Metadata();
            encoded.put(MetadataKey.TYPE.getKey(), getType().getMetaDataEncoded());
            encoded.put(MetadataKey.VALUE.getKey(), value);
            return encoded;
        }
        
        private static Member decode(Metadata encoded, String ownerAcctId) throws ServiceException {
            String encodedType = encoded.get(MetadataKey.TYPE.getKey());
            String value = encoded.get(MetadataKey.VALUE.getKey());

            Type type = Type.fromMetadata(encodedType);

            if( type.equals( Type.CONTACT_REF ))
            {
                ItemId id = new ItemId( value, ownerAcctId );
                if( id.getAccountId().equalsIgnoreCase( ownerAcctId ) ) {
                    value = String.valueOf( id.getId() );
                }
            }
            
            return Member.init(type, value);
        }
    }
    
    public static class ContactRefMember extends Member {
        public ContactRefMember(String value) throws ServiceException {
            super(value);
        }

        @Override
        public Type getType() {
            return Type.CONTACT_REF;
        }
        
        private String genDerefedKey(Contact contact) throws ServiceException {
            return contact.getFileAsString();
        }
        
        private String genDerefedKey(Element eContact) throws ServiceException {
            return eContact.getAttribute(MailConstants.A_FILE_AS_STR, null);
        }
        
        private List<String> genDerefedEmailAddrs(Account ownerAcct, Contact contact) {
            String emailFields[] = Contact.getEmailFields(ownerAcct);
            Map<String, String> fieldMap = contact.getAllFields(); 
            
            List<String> result = new ArrayList<String>();
            for (String field : emailFields) {
                String addr = fieldMap.get(field);
                if (addr != null && !addr.trim().isEmpty()) {
                    result.add(addr);
                }
            }
            return result;
        }
        
        private List<String> genDerefedEmailAddrs(Account ownerAcct, Element eContact) {
            String emailFields[] = Contact.getEmailFields(ownerAcct);
            Set<String> emailFieldsSet = new HashSet<String>(Arrays.asList(emailFields));

            List<String> result = new ArrayList<String>();
            for (Element eAttr : eContact.listElements(MailConstants.E_ATTRIBUTE)) {
                String field = eAttr.getAttribute(MailConstants.A_ATTRIBUTE_NAME, null);
                if (field != null && emailFieldsSet.contains(field)) {
                    String content = eAttr.getText();
                    if (!Strings.isNullOrEmpty(content)) {
                        result.add(content);
                    }
                }
            }
            
            return result;
        }

        @Override
        protected void deref(Mailbox requestedMbox, OperationContext octxt, SoapProtocol proxyProtocol) 
        throws ServiceException {
            Object obj = null;
            String key = null;
            List<String> emailAddrs = null;
            
            ItemId itemId = new ItemId(value, requestedMbox.getAccountId());
            String ownerAcctId = itemId.getAccountId();
            Account ownerAcct = Provisioning.getInstance().get(AccountBy.id, ownerAcctId);
            if (ownerAcct == null) {
                ZimbraLog.contact.debug("no such account for contact group member: " + itemId.toString());
                return;
            }
            
            if (itemId.isLocal()) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(itemId.getAccountId(), false);
                Contact contact = mbox.getContactById(octxt, itemId.getId());
                if (contact != null) {
                    obj = contact;
                    key = genDerefedKey(contact);
                    emailAddrs = genDerefedEmailAddrs(ownerAcct, contact);
                }
            } else {
                AuthToken authToken = AuthToken.getCsrfUnsecuredAuthToken(octxt.getAuthToken());
                Element eContact = fetchRemoteContact(authToken, ownerAcct, itemId, proxyProtocol);
                if (eContact != null) {
                    obj = eContact;
                    key = genDerefedKey(eContact);
                    emailAddrs = genDerefedEmailAddrs(ownerAcct, eContact);
                }
            }
            
            setDerefedObject(obj, key, emailAddrs);
        }
        
        private Element fetchRemoteContact(AuthToken authToken, Account ownerAcct, 
                ItemId contactId, SoapProtocol proxyProtocol)
        throws ServiceException {
            String serverUrl = URLUtil.getAdminURL(Provisioning.affinityServer(ownerAcct));
            SoapHttpTransport transport = new SoapHttpTransport(serverUrl);
            transport.setAuthToken(authToken.toZAuthToken());
            transport.setTargetAcctId(ownerAcct.getId());
            
            Element request = Element.create(proxyProtocol, MailConstants.GET_CONTACTS_REQUEST);
            Element eContact = request.addElement(MailConstants.E_CONTACT);
            eContact.addAttribute(MailConstants.A_ID, contactId.toString());
            
            Element response;
            try {
                response = transport.invokeWithoutSession(request);
            } catch (IOException e) {
                ZimbraLog.contact.debug("unable to fetch remote member ", e);
                throw ServiceException.PROXY_ERROR("unable to fetch remote member " + contactId.toString(), serverUrl);
            }
            Element eGotContact = response.getOptionalElement(MailConstants.E_CONTACT);
            if (eGotContact != null) {
                eGotContact.detach();
            }
            return eGotContact;
        }
    }
    
    public static class GalRefMember extends Member {
        private static final String PRIMARY_EMAIL_FIELD = "email";
        private static final Set<String> GAL_EMAIL_FIELDS = new HashSet<String>(Arrays.asList(
                new String[] {
                        PRIMARY_EMAIL_FIELD, "email2", "email3", "email4", "email5", "email6", 
                        "email7", "email8", "email9", "email10", "email11", "email12", "email13", 
                        "email14", "email15", "email16"
                }));
        
        public GalRefMember(String value) throws ServiceException {
            super(value);
        }

        @Override
        public Type getType() {
            return Type.GAL_REF;
        }
        
        private String genDerefedKey(Contact contact) throws ServiceException {
            String key = contact.get(ContactConstants.A_email);
            if (key == null) {
                key = contact.getFileAsString();
            }
            return key;
        }
        
        private String genDerefedKey(GalContact galContact) throws ServiceException {
            String key = galContact.getSingleAttr(ContactConstants.A_email);
            if (key == null) {
                key = galContact.getSingleAttr(ContactConstants.A_fullName);
            }
            if (key == null) {
                key = galContact.getSingleAttr(ContactConstants.A_firstName) + " " +
                    galContact.getSingleAttr(ContactConstants.A_lastName);
            }
            return key;
        }
        
        private String genDerefedKey(Element eContact) throws ServiceException {
            // a proxied GAL sync account entry, must have a fileAs string
            return eContact.getAttribute(MailConstants.A_FILE_AS_STR, null);
        }
        
        private List<String> genDerefedEmailAddrs(Contact contact) {
            Map<String, String> fieldMap = contact.getAllFields(); 
            
            List<String> result = new ArrayList<String>();
            for (String field : GAL_EMAIL_FIELDS) {
                String addr = fieldMap.get(field);
                if (addr != null && !addr.trim().isEmpty()) {
                    result.add(addr);
                }
            }
            return result;
        }
        
        private List<String> genDerefedEmailAddrs(GalContact galContact) {
            Map<String, Object> fieldMap = galContact.getAttrs();
            
            List<String> result = new ArrayList<String>();
            for (String field : GAL_EMAIL_FIELDS) {
                Object value = fieldMap.get(field);
                if (value instanceof String) {
                    result.add((String) value);
                } else if (value instanceof String[]) {
                    String[] addrs = (String[]) value;
                    for (String addr : addrs) {
                        result.add(addr);
                    }
                }
            }
            return result;
        }
        
        private List<String> genDerefedEmailAddrs(Element eContact) {

            List<String> result = new ArrayList<String>();
            for (Element eAttr : eContact.listElements(MailConstants.E_ATTRIBUTE)) {
                String field = eAttr.getAttribute(MailConstants.A_ATTRIBUTE_NAME, null);
                if (field != null && GAL_EMAIL_FIELDS.contains(field)) {
                    String content = eAttr.getText();
                    if (!Strings.isNullOrEmpty(content)) {
                        result.add(content);
                    }
                }
            }
            
            return result;
        }

        @Override
        protected void deref(Mailbox mbox, OperationContext octxt, SoapProtocol proxyProtocol) 
        throws ServiceException {
            // search GAL by DN
            GalSearchParams params = new GalSearchParams(mbox.getAccount(), null);
            params.setSearchEntryByDn(value);
            params.setType(GalSearchType.all);
            params.setLimit(1);
            params.setProxyProtocol(proxyProtocol);
            
            // params.setExtraQueryCallback(new ContactGroupExtraQueryCallback(value));
            ContactGroupResultCallback callback = new ContactGroupResultCallback(params);
            params.setResultCallback(callback);
            
            GalSearchControl gal = new GalSearchControl(params);
            gal.search(); 
            
            Object obj = callback.getResult(); 
            String key = null;
            List<String> emailAddrs = null;
            
            if (obj != null) {
                if (obj instanceof Contact) {
                    Contact contact = (Contact) obj;
                    key = genDerefedKey(contact);
                    emailAddrs = genDerefedEmailAddrs(contact);
                } else if (obj instanceof GalContact) {
                    GalContact galContact = (GalContact) obj;
                    key = genDerefedKey(galContact);
                    emailAddrs = genDerefedEmailAddrs(galContact);
                } else if (obj instanceof Element) {
                    Element eContact = (Element) obj;
                    key = genDerefedKey(eContact);
                    emailAddrs = genDerefedEmailAddrs(eContact);
                }
            }
            
            setDerefedObject(obj, key, emailAddrs);
        }

        private static class ContactGroupResultCallback extends GalSearchResultCallback {
            Object result;
            
            public ContactGroupResultCallback(GalSearchParams params) {
                super(params);
            }
            
            private Object getResult() {
                return result;
            }
            
            @Override
            public Element handleContact(Contact contact) throws ServiceException {
                result = contact;
                return null; 
            }
            
            @Override
            public void handleContact(GalContact galContact) throws ServiceException {
                result = galContact;
            }
            
            @Override
            public void handleElement(Element element) throws ServiceException {
                element.detach();
                result = element; // will be attached directly to the outut element in ToXML.
            }
        }

    }
    
    public static class InlineMember extends Member {
        public InlineMember(String value) throws ServiceException {
            super(value);
        }

        @Override
        public Type getType() {
            return Type.INLINE;
        }

        @Override
        protected void deref(Mailbox mbox, OperationContext octxt, SoapProtocol proxyProtocol) {
            // value is the derefed obj, the key, and the email
            List<String> emailAddrs = new ArrayList<String>();
            emailAddrs.add(value);
            
            setDerefedObject(value, value, emailAddrs);
        }
    }
    
    
    /*
     * =============================
     * Migrate dlist to groupMember
     * =============================
     */
    public static class MigrateContactGroup {
        /*
         * dlist is a String of comma-seperated email address with optional display part.
         * There could be comma in the display part.
         * e.g
         * "Ballard, Martha" <martha34@aol.com>, "Davidson, Ross" <rossd@example.zimbra.com>, user1@test.com
         * 
         * This should be split to:
         * "Ballard, Martha" <martha34@aol.com>
         * "Davidson, Ross" <rossd@example.zimbra.com>
         * user1@test.com
         */
        private static final Pattern PATTERN = Pattern.compile("(([\\s]*)(\"[^\"]*\")*[^,]*[,]*)");
        
        private Mailbox mbox;
        private OperationContext octxt;
        
        public MigrateContactGroup(Mailbox mbox) throws ServiceException {
            this.mbox = mbox;
            octxt = new OperationContext(mbox);
        }
        
        public void handle() throws ServiceException {
            for (MailItem item : mbox.getItemList(octxt, MailItem.Type.CONTACT, -1)) {
                Contact contact = (Contact) item;
                try {
                    migrate(contact);
                } catch (Exception e) {
                    if (contact.isGroup()) {
                        ZimbraLog.contact.info("skipped migrating contact group %d", contact.getId(), e);
                    }
                }
            }
        }
        
        public void migrate(Contact contact) throws ServiceException {
            if (!contact.isGroup()) {
                return;
            }

            String dlist = contact.get(ContactConstants.A_dlist);
            if (Strings.isNullOrEmpty(dlist)) {
                ZimbraLog.contact.info("skipped migrating contact group %d as dlist is empty", contact.getId());
                return;
            }

            ContactGroup contactGroup = ContactGroup.init();

            migrate(contactGroup, dlist);

            if (contactGroup.hasMembers()) {
                ParsedContact pc = new ParsedContact(contact);

                pc.modifyField(ContactConstants.A_groupMember, contactGroup.encode());

                // remove dlist.  
                // TODO: should we do this? or should we keep dlist and hide it
                // using zimbraContactHiddenAttributes? 
                pc.modifyField(ContactConstants.A_dlist, null);

                mbox.modifyContact(octxt, contact.getId(), pc);

                ZimbraLog.contact.info("migrated contact group %s: dlist=[%s], groupMember=[%s]",
                        contact.getId(), dlist, contactGroup.dump());
            } else {
                ZimbraLog.contact.info("aborted migrating contact group %d: dlist=[%s]", contact.getId(), dlist);
            }
        }

        // add each dlist member as an inlined member in groupMember
        static void migrate(ContactGroup contactGroup, String dlist) throws ServiceException {
            Matcher matcher = PATTERN.matcher(dlist);
            while (matcher.find()) {
                String token = matcher.group();
                int len = token.length();
                if (len > 0) {
                    if (token.charAt(len-1) == ',') {
                        token = token.substring(0, len-1);
                    }
                    String addr = token.trim();
                    if (!addr.isEmpty()) {
                        try {
                            contactGroup.addMember(Member.Type.INLINE, addr);
                        } catch (ServiceException e) {
                            ZimbraLog.contact.info("skipped contact group member %s", addr);
                        }
                    }
                }
            }
        }
        
    }
    
}
