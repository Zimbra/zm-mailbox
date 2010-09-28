package com.zimbra.cs.service.account;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchResultCallback;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.soap.ZimbraSoapContext;

public class GetDistributionListMembers extends AccountDocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        int limit = (int) request.getAttributeLong(AdminConstants.A_LIMIT, 0);
        if (limit < 0) {
            throw ServiceException.INVALID_REQUEST("limit" + limit + " is negative", null);
        }
        
        int offset = (int) request.getAttributeLong(AdminConstants.A_OFFSET, 0);
        if (offset < 0) {
            throw ServiceException.INVALID_REQUEST("offset" + offset + " is negative", null);
        }
        
        Element d = request.getElement(AdminConstants.E_DL);
        String dlName = d.getText();
        
        Account account = getAuthenticatedAccount(getZimbraSoapContext(context));
        
        DLMembersResult dlMembersResult = searchGal(zsc, account, dlName, request);
        
        if (dlMembersResult == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(dlName);
        
        if (dlMembersResult instanceof ProxiedDLMembers) {
            return ((ProxiedDLMembers)dlMembersResult).getResponse();
        } else {
            return processDLMembers(zsc, dlName, account , limit, offset, (DLMembers)dlMembersResult);
        }
    }
    
    private Element processDLMembers(ZimbraSoapContext zsc, String dlName, Account account, 
            int limit, int offset, DLMembers dlMembers) throws ServiceException {
          
        if (!GalSearchControl.canExpandGalGroup(dlName, dlMembers.getDLZimbraId(), account))
            throw ServiceException.PERM_DENIED("can not access dl members: " + dlName);
       
        
        Element response = zsc.createElement(AccountConstants.GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE);
        if (dlMembers != null) {
            int numMembers = dlMembers.getTotal();
            
            if (offset > 0 && offset >= numMembers) {
                throw ServiceException.INVALID_REQUEST("offset " + offset + " greater than size " + numMembers, null);
            }
            
            int endIndex = offset + limit;
            if (limit == 0) {
                endIndex = numMembers;
            }
            if (endIndex > numMembers) {
                endIndex = numMembers;
            }
            
            dlMembers.encodeMembers(offset, endIndex, response);
            
            response.addAttribute(AccountConstants.A_MORE, endIndex < numMembers);
            response.addAttribute(AccountConstants.A_TOTAL, numMembers);
        }
        
        return response;
    }
    
    // common super interface for all the DLMembers classes
    private interface DLMembersResult {
    }
    
    private interface DLMembers extends DLMembersResult {

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
        
        public int getTotal() {
            if (mMembers == null)
                return 0;
            else
                return mMembers.length();
        }
        
        public String getDLZimbraId() {
            return mContact.get(ContactConstants.A_zimbraId);
        }
        
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
        
        public int getTotal() {
            if (mMembers == null)
                return 0;
            else
                return mMembers.length;
        }
        
        public String getDLZimbraId() {
            return mGalContact.getSingleAttr(ContactConstants.A_zimbraId);
        }
        
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

    private static class ProxiedDLMembers implements DLMembersResult {
        private Element mResponse;
        
        ProxiedDLMembers(Element response) {
            mResponse = response;
            mResponse.detach();
        }
        
        Element getResponse() {
            return mResponse;
        }
    }
    
    private static class GalGroupMembersCallback extends GalSearchResultCallback {
        private DLMembersResult mDLMembers;
        
        GalGroupMembersCallback(GalSearchParams params) {
            super(params);
        }
        
        public boolean passThruProxiedGalAcctResponse() {
            return true;
        }
        
        DLMembersResult getDLMembers() {
            return mDLMembers;
        }
        
        public void handleProxiedResponse(Element resp) {
            mDLMembers = new ProxiedDLMembers(resp);
        }
        
        public Element handleContact(Contact contact) throws ServiceException {
            mDLMembers = new ContactDLMembers(contact);
            return null; 
        }
        
        public void handleContact(GalContact galContact) throws ServiceException {
            mDLMembers = new GalContactDLMembers(galContact);
        }
        
        public void handleElement(Element e) throws ServiceException {
            // TODO: for perf reason proxy this request to the GAL sync account host 
            //       if GAL sync account is configured but not on this host
            
        }
    }
    
    private static DLMembersResult searchGal(ZimbraSoapContext zsc, Account account, String groupName, Element request) throws ServiceException {
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
