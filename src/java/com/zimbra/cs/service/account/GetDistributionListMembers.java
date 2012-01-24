package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.gal.GalGroupMembers;
import com.zimbra.cs.gal.GalGroupMembers.DLMembers;
import com.zimbra.cs.gal.GalGroupMembers.DLMembersResult;
import com.zimbra.cs.gal.GalGroupMembers.ProxiedDLMembers;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.soap.ZimbraSoapContext;

public class GetDistributionListMembers extends GalDocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        
        if (!canAccessAccount(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }
        
        Element d = request.getElement(AdminConstants.E_DL);
        String dlName = d.getText();
        
        int offset = getOffset(request);
        int limit = getLimit(request);
        
        // null offset/limit and set _offset/_limit before calling searchGal().
        request.addAttribute(MailConstants.A_QUERY_OFFSET, (String)null);
        request.addAttribute(MailConstants.A_LIMIT, (String)null);
        request.addAttribute(AccountConstants.A_OFFSET_INTERNAL, offset);
        request.addAttribute(AccountConstants.A_LIMIT_INTERNAL, limit);
        
        DLMembersResult dlMembersResult = GalGroupMembers.searchGal(zsc, account, dlName, request);
        
        if (dlMembersResult == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(dlName);
        
        if (dlMembersResult instanceof ProxiedDLMembers) {
            return ((ProxiedDLMembers)dlMembersResult).getResponse();
        } else {
            return processDLMembers(zsc, dlName, account , limit, offset, (DLMembers)dlMembersResult);
        }
    }
    
    /*
     * For SOAP API cleanness purpose, we want to support offset/limit attributes.
     * 
     * But that will interfere with the GAL search if GSA is in place, 
     * because the mailbox search code would take them as the offset/limit for 
     * the contact search.
     * 
     * If we simply null the offset/limit when passing the request object to 
     * GalGroupMembers.searchGal(), the original value passed from the client 
     * will be lost if searchGal() proxies the request to the GSA's home server. 
     * 
     * We could've chosen to use different attributes for offset/limit for this 
     * SOAP request, but it is not as ideal/pretty from API point of view.
     *  
     * To fix it: 
     * Before calling GalGroupMembers.searchGal(), we read the offset/limit 
     * from the request, set them on internal attributes _limit/_offset, and 
     * null the limit/offset on the request object.
     * 
     * In the handler, we first look for _limit/_offset, if set(must have been 
     * proxied to this server by the GSA code), honor them.  
     * If not set(the request is from the client client), honor limit/offset 
     * if they are present.
     * 
     * 
     */
    private int getOffset(Element request) throws ServiceException {
        
        int offset = 0;
        
        // see if the internal attrs are set, use them if set.
        String offsetStr = request.getAttribute(AccountConstants.A_OFFSET_INTERNAL, null);
        if (offsetStr != null) {
            offset = (int) Element.parseLong(AccountConstants.A_OFFSET_INTERNAL, offsetStr);
        } else {
            // otherwise, see if it is set by the client, use them if set
            offsetStr = request.getAttribute(MailConstants.A_QUERY_OFFSET, null);
            if (offsetStr != null) {
                offset = (int) Element.parseLong(MailConstants.A_QUERY_OFFSET, offsetStr);
            }
        }
        
        if (offset < 0) {
            throw ServiceException.INVALID_REQUEST("offset" + offset + " is negative", null);
        }
        
        return offset;
    }
    
    private int getLimit(Element request) throws ServiceException {
        
        int limit = 0;
        
        // see if the internal attrs are set, use them if set.
        String limitStr = request.getAttribute(AccountConstants.A_LIMIT_INTERNAL, null);
        if (limitStr != null) {
            limit = (int) Element.parseLong(AccountConstants.A_LIMIT_INTERNAL, limitStr);
        } else {
            // otherwise, see if it is set by the client, use them if set
            limitStr = request.getAttribute(MailConstants.A_LIMIT, null);
            if (limitStr != null) {
                limit = (int) Element.parseLong(MailConstants.A_LIMIT, limitStr);
            }
        }
        
        if (limit < 0) {
            throw ServiceException.INVALID_REQUEST("limit" + limit + " is negative", null);
        }
        
        return limit;
    }

    
    protected Element processDLMembers(ZimbraSoapContext zsc, String dlName, Account account, 
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
