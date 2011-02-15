package com.zimbra.cs.gal;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.soap.ZimbraSoapContext;

public class GalGroupMembers {

    // common super interface for all the DLMembers classes
    public static interface DLMembersResult {
    }
    
    public interface DLMembers extends DLMembersResult {

        int getTotal();
        
        String getDLZimbraId();
        
        /**
         * 
         * @param beginIndex the beginning index, inclusive.
         * @param endIndex   the ending index, exclusive. 
         * @param resp
         */
        void encodeMembers(int beginIndex, int endIndex, Element resp);
    }
    
    private static class ContactDLMembers implements DLMembers {
        private Contact mContact;
        private JSONArray mMembers;
        
        private ContactDLMembers(Contact contact) {
            mContact = contact;
            
            String members = mContact.get(ContactConstants.A_member);
            
            if (members != null) {
                try {
                    mMembers = Contact.getMultiValueAttrArray(members);
                } catch (JSONException e) {
                    ZimbraLog.account.warn("unable to get members from Contact " + mContact.getId(), e);
                }
            }
        }
        
        @Override
        public int getTotal() {
            if (mMembers == null)
                return 0;
            else
                return mMembers.length();
        }
        
        @Override
        public String getDLZimbraId() {
            return mContact.get(ContactConstants.A_zimbraId);
        }
        
        @Override
        public void encodeMembers(int beginIndex, int endIndex, Element resp) {
            if (mMembers == null)
                return;
            
            if (endIndex <= getTotal()) {
                try {
                    for (int i = beginIndex; i < endIndex; i++) {
                        Element eMember = resp.addElement(AccountConstants.E_DLM).setText(mMembers.getString(i));
                    }
                } catch (JSONException e) {
                    ZimbraLog.account.warn("unable to get members from Contact " + mContact.getId(), e);
                }
            }
        }
    }
    
    private static class GalContactDLMembers implements DLMembers {
        private GalContact mGalContact;
        String[] mMembers;
        
        private GalContactDLMembers(GalContact galContact) {
            mGalContact = galContact;
            
            Object members = mGalContact.getAttrs().get(ContactConstants.A_member);
            if (members instanceof String)
                mMembers = new String[]{(String)members};
            else if (members instanceof String[])
                mMembers = (String[])members;
        }
        
        @Override
        public int getTotal() {
            if (mMembers == null)
                return 0;
            else
                return mMembers.length;
        }
        
        @Override
        public String getDLZimbraId() {
            return mGalContact.getSingleAttr(ContactConstants.A_zimbraId);
        }
        
        @Override
        public void encodeMembers(int beginIndex, int endIndex, Element resp) {
            if (mMembers == null)
                return;
            
            if (endIndex <= getTotal()) {
                for (int i = beginIndex; i < endIndex; i++) {
                    Element eMember = resp.addElement(AccountConstants.E_DLM).setText(mMembers[i]);
                }
            }        
        }

    }

    public static class ProxiedDLMembers implements DLMembersResult {
        private Element mResponse;
        
        ProxiedDLMembers(Element response) {
            mResponse = response;
            mResponse.detach();
        }
        
        public Element getResponse() {
            return mResponse;
        }
    }
    
    private static class GalGroupMembersCallback extends GalSearchResultCallback {
        private DLMembersResult mDLMembers;
        
        GalGroupMembersCallback(GalSearchParams params) {
            super(params);
        }
        
        @Override
        public boolean passThruProxiedGalAcctResponse() {
            return true;
        }
        
        DLMembersResult getDLMembers() {
            return mDLMembers;
        }
        
        @Override
        public void handleProxiedResponse(Element resp) {
            mDLMembers = new ProxiedDLMembers(resp);
        }
        
        @Override
        public Element handleContact(Contact contact) throws ServiceException {
            mDLMembers = new ContactDLMembers(contact);
            return null; 
        }
        
        @Override
        public void handleContact(GalContact galContact) throws ServiceException {
            mDLMembers = new GalContactDLMembers(galContact);
        }
        
        @Override
        public void handleElement(Element e) throws ServiceException {
            // should never be called
        }
    }

    
    public static DLMembersResult searchGal(ZimbraSoapContext zsc, Account account, String groupName, Element request) 
    throws ServiceException {
        GalSearchParams params = new GalSearchParams(account, zsc);
        params.setQuery(groupName);
        params.setType(Provisioning.GalSearchType.group);
        params.setLimit(1);
        params.setFetchGroupMembers(true);
        params.setRequest(request);
        GalGroupMembersCallback callback = new GalGroupMembersCallback(params);
        params.setResultCallback(callback);
        
        GalSearchControl gal = new GalSearchControl(params);
        gal.search();  
        return callback.getDLMembers();
    }
}
