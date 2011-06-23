package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.TreeMultimap;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
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
import com.zimbra.cs.gal.GalSearchQueryCallback;
import com.zimbra.cs.gal.GalSearchResultCallback;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;

public class ContactGroup {
    
    // metadata keys for ContactGroup 
    private enum MetadataKey {
        LASTID("lid"),
        MEMBERS("m");
        
        private String key;
        MetadataKey(String key) {
            this.key = key;
        }
        
        private String getKey() {
            return key;
        }
    }
    
    public static class MemberId {
        private int id;
        
        private MemberId(int id) {
            this.id = id;
        }
        
        public static MemberId fromString(String idStr) throws ServiceException {
            try {
                Integer id = Integer.valueOf(idStr);
                return new MemberId(id.intValue());
            } catch (NumberFormatException e) {
                throw ServiceException.FAILURE("invalid member id: " + idStr, e);
            }
        }
        
        @Override
        public boolean equals(Object obj) {
            return ((obj instanceof MemberId) && (id == ((MemberId) obj).id));
        }
        
        @Override
        public int hashCode() { 
            return id; 
        }
        
        @Override
        public String toString() {
            return "" + id;
        }
        
        private String getMetaDataEncoded() {
            return toString();
        }
    }
    
    private int lastMemberId;
    
    // ordered map, order is the order in which keys were inserted into the map (insertion-order). 
    // Note that insertion order is not affected if a key is re-inserted into the map. 
    // We need to maintain the order members are added to the group (do we?), and
    // need to be able to quickly get a member by a unique key
    //
    // In members are persisted in MetadataList, which is ordered.
    private Map<MemberId, Member> members = new LinkedHashMap<MemberId, Member>();  // ordered map
    
    // never persisted
    // contains derefed members sorted by the Member.getKey() order
    private TreeMultimap<String, Member> derefedMembers = null;
    
    public static ContactGroup init(Contact contact) throws ServiceException {
        ContactGroup contactGroup = null;
        if (contact != null) {
            String encoded = contact.get(ContactConstants.A_groupMember);
            contactGroup = init(encoded);
        } else {
            contactGroup = new ContactGroup();
        }
        return contactGroup;
    }
    
    public static ContactGroup init(String encoded) throws ServiceException {
        ContactGroup contactGroup = null;
        if (encoded != null) {
            contactGroup = ContactGroup.decode(encoded);
        } else {
            contactGroup = new ContactGroup();
        }
        
        return contactGroup;
    }
    
    private ContactGroup() {
        this(0);
    }
    
    private ContactGroup(int lastMemberId) {
        this.lastMemberId = lastMemberId;
    }
    
    private MemberId getNextMemberId() {
        return new MemberId(++lastMemberId);
    }

    private void addMember(Member member) {
        members.put(member.getId(), member);
    }
    
    private void replaceMember(Member member) {
        members.put(member.getId(), member);
    }
    
    private Member createMember(MemberId reuseId, Member.Type type, String value) throws ServiceException {
        Member member = null;
        MemberId memberId = (reuseId == null) ? getNextMemberId() : reuseId;
        
        switch (type) {
            case CONTACT_REF:
                member = new ContactRefMember(memberId, value);
                break;
            case GAL_REF:  
                member = new GalRefMember(memberId, value);
                break;
            case INLINE: 
                member = new ContactGroup.InlineMember(memberId, value);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("Unrecognized member type: " + type.name(), null);
        }
        return member;
    }
    
    public Member getMemberById(MemberId id) {
        return members.get(id);
    }
    
    public void deleteAllMembers() {
        members.clear();
    }
    
    /*
     * return members in the order they were inserted
     */
    public List<Member> getMembers() {
        return Arrays.asList(members.values().toArray(new Member[members.size()]));
    }
    
    public List<Member> getMembers(boolean preferDerefed) {
        if (preferDerefed && derefedMembers != null) {
            return getDerefedMembers();
        } else {
            return getMembers();
        }
    }
    
    /*
     * return derefed members in Member.getKey() order
     */
    public List<Member> getDerefedMembers() {
        assert(derefedMembers != null);
        return Arrays.asList(derefedMembers.values().toArray(new Member[derefedMembers.size()]));
    }
    
    // create and add member at the end of the list
    public Member addMember(Member.Type type, String value) throws ServiceException {
        Member member = createMember(null, type, value);
        addMember(member);
        return member;
    }
    
    public void removeMember(MemberId memberId) {
        members.remove(memberId);
    }
    
    public void modifyMember(MemberId memberId, Member.Type type, String value) throws ServiceException {
        Member member = members.get(memberId);
        if (member == null) {
            throw ServiceException.INVALID_REQUEST("no such member: " + memberId, null);
        }
        
        if (type == null || member.getType() == type) {
            member.setValue(value);
        } else {
            Member updatedMember = createMember(memberId, type, value);
            replaceMember(updatedMember);
        }
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
        derefedMembers = TreeMultimap.create();
        
        for (Member member : members.values()) {
            member.derefMember(mbox, octxt);
            if (member.derefed()) {
                String key = member.getDerefedKey();
                derefedMembers.put(key, member);
            } else {
                ZimbraLog.contact.debug("contact group member cannot be derefed: " + member.getValue());
                derefedMembers.put(member.getValue(), member);
            }
        }
    }
    
    public String encode() {
        Metadata encoded = new Metadata();
        
        MetadataList encodedMembers = new MetadataList();
        for (Member member : members.values()) {
            encodedMembers.add(member.encode());
        }
        
        encoded.put(MetadataKey.LASTID.getKey(), lastMemberId);
        encoded.put(MetadataKey.MEMBERS.getKey(), encodedMembers);
        
        return encoded.toString();
    }
    
    private static ContactGroup decode(String encodedStr) throws ServiceException {
        try {
            Metadata encoded = new Metadata(encodedStr);
            int lastMemberId = encoded.getInt(MetadataKey.LASTID.getKey(), -1);
            if (lastMemberId == -1) {
                throw ServiceException.FAILURE("missing last member id in metadata", null);
            }
            
            ContactGroup contactGroup = new ContactGroup(lastMemberId);
            
            MetadataList members = encoded.getList(MetadataKey.MEMBERS.getKey());
            if (members == null) {
                throw ServiceException.FAILURE("missing members in metadata", null);
            }
            
            List<Metadata> memberList = members.asList();
            for (Metadata encodedMember : memberList) {
                Member member = Member.decode(encodedMember);
                contactGroup.addMember(member);
            }
            return contactGroup;
            
        } catch (ServiceException e) {
            ZimbraLog.contact.warn("unabale to decode contact group", e);
            throw e;
        }
        
    }
    
    
    /*
     *======================
     * Group Member classes
     *======================
     */
    public static abstract class Member implements Comparable {
        
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
            CONTACT_REF("C", ContactConstants.GROUP_MEMBER_TYPE_CONTACT_REF),  // ContactRefMember
            GAL_REF("G", ContactConstants.GROUP_MEMBER_TYPE_GAL_REF),          // ContactRefMember
            INLINE("I", ContactConstants.GROUP_MEMBER_TYPE_INLINE);            // InlineMember
            
            private String metadataEncoded;
            private String soapEncoded;
            
            Type(String metadataEncoded, String soapEncoded) {
                this.metadataEncoded = metadataEncoded;
                this.soapEncoded = soapEncoded;
            }
            
            private String getMetaDataEncoded() {
                return metadataEncoded;
            }
            
            public String getSoapEncoded() {
                return soapEncoded;
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
        
        private MemberId id;  // unique id of the member within this group
        protected String value;
        private String derefedKey; // key for sorting in the expanded group
        private Object derefedObject;
        
        public abstract Type getType();
        protected abstract String genDerefedKey(Object obj) throws ServiceException ;
        protected abstract Object deref(Mailbox mbox, OperationContext octxt) 
        throws ServiceException ;  // load the actual entry

        
        protected Member(MemberId id, String value) throws ServiceException {
            this.id = id;
            setValue(value);
        }
        
        /*
         * called from TreeMultimap (ContactGroup.derefAllMembers) when two 
         * derefed keys are the same.  return id order in this case.
         * 
         * Note: must not return 0 here, if we do, the member will not be 
         *       inserted into the TreeMultimap.
         */
        @Override
        public int compareTo(Object other) {
            if (!(other instanceof Member)) {
                return 0;
            }
            Member otherMember = (Member) other;
            return getId().toString().compareTo(otherMember.getId().toString());
        }
        
        public MemberId getId() {
            return id;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDerefedKey() {
            assert(derefed());
            return derefedKey;
        }
        
        public Object getDerefedObj() {
            return derefedObject;
        }
        
        protected boolean derefed() {
            return getDerefedObj() != null;
        }
        
        private void derefMember(Mailbox mbox, OperationContext octxt) {
            if (!derefed()) {
                try {
                    Object obj = deref(mbox, octxt);
                    setDerefedObject(obj);
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
        
        private void setDerefedObject(Object obj) throws ServiceException {
            if (obj != null) {
                setDerefedKey(genDerefedKey(obj));
            }
            this.derefedObject = obj;
        }
        
        private Metadata encode() {
            Metadata encoded = new Metadata();
            encoded.put(MetadataKey.ID.getKey(), id.getMetaDataEncoded());
            encoded.put(MetadataKey.TYPE.getKey(), getType().getMetaDataEncoded());
            encoded.put(MetadataKey.VALUE.getKey(), value);
            return encoded;
        }
        
        private static Member decode(Metadata encoded) throws ServiceException {
            String idStr = encoded.get(MetadataKey.ID.getKey());
            String encodedType = encoded.get(MetadataKey.TYPE.getKey());
            String value = encoded.get(MetadataKey.VALUE.getKey());
            
            MemberId id = MemberId.fromString(idStr);
            Type type = Type.fromMetadata(encodedType);
            switch (type) {
                case CONTACT_REF:
                    return new ContactRefMember(id, value);
                case GAL_REF:  
                    return new GalRefMember(id, value);
                case INLINE: 
                    return new InlineMember(id, value);
            }
            throw ServiceException.FAILURE("Unrecognized member type: " + encodedType, null);
        }
    }
    
    public static class ContactRefMember extends Member {
        public ContactRefMember(MemberId id, String value) throws ServiceException {
            super(id, value);
        }

        @Override
        public Type getType() {
            return Type.CONTACT_REF;
        }

        @Override 
        protected String genDerefedKey(Object obj) throws ServiceException {
            String key = null;
            
            if (obj instanceof Contact) {
                Contact contact = (Contact) obj;
                key = contact.getFileAsString();
            } else if (obj instanceof Element) {
                // a proxied contact, must have a fileAs string
                Element element = (Element) obj;
                key = element.getAttribute(MailConstants.A_FILE_AS_STR, null);
                // should we fallback to other fileds if key is null?
            }
            return key;
        }

        @Override
        protected Object deref(Mailbox requestedMbox, OperationContext octxt) 
        throws ServiceException {
            Object obj = null;
            
            ItemId itemId = new ItemId(value, requestedMbox.getAccountId());
            if (itemId.isLocal()) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(itemId.getAccountId(), false);
                Contact contact = mbox.getContactById(octxt, itemId.getId());
                obj = contact;
            } else {
                obj = fetchRemoteContact(octxt.getAuthToken(), itemId);
            }
            
            return obj;
        }
        
        private Element fetchRemoteContact(AuthToken authToken, ItemId contactId)
        throws ServiceException {
            Provisioning prov = Provisioning.getInstance();
            
            String ownerAcctId = contactId.getAccountId();
            Account ownerAcct = prov.get(AccountBy.id, ownerAcctId);
            String serverUrl = URLUtil.getAdminURL(prov.getServerByName(ownerAcct.getMailHost()));
            SoapHttpTransport transport = new SoapHttpTransport(serverUrl);
            transport.setAuthToken(authToken.toZAuthToken());
            transport.setTargetAcctId(ownerAcctId);
            
            Element request = Element.create(SoapProtocol.Soap12, MailConstants.GET_CONTACTS_REQUEST);
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
        public GalRefMember(MemberId id, String value) throws ServiceException {
            super(id, value);
        }

        @Override
        public Type getType() {
            return Type.GAL_REF;
        }

        @Override
        protected String genDerefedKey(Object obj) throws ServiceException {
            String key = null;
            if (obj instanceof Contact) {
                Contact contact = (Contact) obj;
                key = contact.getFileAsString();
                // should we fallback to other fileds if key is null?
            } else if (obj instanceof GalContact) {
                GalContact galContact = (GalContact) obj;
                key = galContact.getSingleAttr(ContactConstants.A_email);
                if (key == null) {
                    key = galContact.getSingleAttr(ContactConstants.A_fullName);
                }
                if (key == null) {
                    key = galContact.getSingleAttr(ContactConstants.A_firstName) + " " +
                        galContact.getSingleAttr(ContactConstants.A_lastName);
                }
            } else if (obj instanceof Element) {
                // a proxied GAL sync account entry, must have a fileAs string
                Element element = (Element) obj;
                key = element.getAttribute(MailConstants.A_FILE_AS_STR, null);
                // should we fallback to other fileds if key is null?
            }
            return key;
        }

        @Override
        protected Object deref(Mailbox mbox, OperationContext octxt) 
        throws ServiceException {
            // search GAL by DN
            GalSearchParams params = new GalSearchParams(mbox.getAccount(), null);
            params.setSearchEntryByDn(value);
            params.setType(Provisioning.GalSearchType.all);
            params.setLimit(1);
            
            // params.setExtraQueryCallback(new ContactGroupExtraQueryCallback(value));
            ContactGroupResultCallback callback = new ContactGroupResultCallback(params);
            params.setResultCallback(callback);
            
            GalSearchControl gal = new GalSearchControl(params);
            gal.search();  
            Object obj = callback.getResult(); 
            return obj;
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
        public InlineMember(MemberId id, String value) throws ServiceException {
            super(id, value);
        }

        @Override
        public Type getType() {
            return Type.INLINE;
        }

        @Override
        protected String genDerefedKey(Object obj) throws ServiceException {
            return value;
        }

        @Override
        protected Object deref(Mailbox mbox, OperationContext octxt) {
            return value;
        }
    }
}
