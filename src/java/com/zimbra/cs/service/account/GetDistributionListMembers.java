package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.gal.GalGroupMembers;
import com.zimbra.cs.gal.GalGroupMembers.DLMembers;
import com.zimbra.cs.gal.GalGroupMembers.DLMembersResult;
import com.zimbra.cs.gal.GalGroupMembers.ProxiedDLMembers;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.soap.ZimbraSoapContext;

public class GetDistributionListMembers extends GalDocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(getZimbraSoapContext(context));
        
        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
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
        
        DLMembersResult dlMembersResult = GalGroupMembers.searchGal(zsc, account, dlName, request);
        
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


}
