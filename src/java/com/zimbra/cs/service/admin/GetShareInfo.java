package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.account.GetShareInfo.ShareInfoVisitor;
import com.zimbra.soap.ZimbraSoapContext;

public class GetShareInfo extends ShareInfoHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        Provisioning prov = Provisioning.getInstance();
        
        // entry to get the share info for 
        NamedEntry taregtEntry = getTargetEntry(zsc, request, prov);
        
        Element eGrantee = request.getElement(AdminConstants.E_GRANTEE);
        boolean directOnly = eGrantee.getAttributeBool(AdminConstants.A_DIRECT_ONLY);
        
        Account ownerAcct = getOwner(zsc, request, prov, false);
        
        // TODO, check permission
        
        Element response = zsc.createElement(AdminConstants.MODIFY_ACCOUNT_RESPONSE);
        ShareInfoVisitor visitor = new com.zimbra.cs.service.account.GetShareInfo.ShareInfoVisitor(prov, response);
        
        if (taregtEntry instanceof Account)
            prov.getShareInfo((Account)taregtEntry, directOnly, ownerAcct, visitor);
        else if (taregtEntry instanceof DistributionList)
            prov.getShareInfo((DistributionList)taregtEntry, directOnly, ownerAcct, visitor);
        else
            throw ServiceException.INVALID_REQUEST("unsupported target type", null);
        
        return response;
    }
    
}