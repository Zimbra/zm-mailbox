package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.account.GetShareInfo.ShareInfoVisitor;
import com.zimbra.soap.ZimbraSoapContext;

public class GetPublishedShareInfo extends ShareInfoHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        // entry to get the share info for 
        NamedEntry taregtEntry = getPublishableTargetEntry(zsc, request, prov);
        
        checkDistributionListRight(zsc, (DistributionList)taregtEntry, Admin.R_getDistributionListShareInfo);
        
        Element response = zsc.createElement(AdminConstants.GET_PUBLISHED_SHARE_INFO_RESPONSE);

        Account owner = null;
        Element eOwner = request.getOptionalElement(AccountConstants.E_OWNER);
        if (eOwner != null) {
            AccountBy acctBy = AccountBy.fromString(eOwner.getAttribute(AccountConstants.A_BY));
            String key = eOwner.getText();
            owner = prov.get(acctBy, key);
                
            // in the account namespace GetShareInfo
            // to defend against harvest attacks return "no shares" instead of error 
            // when an invalid user name/id is used.
            //
            // this is the admin namespace GetShareInfo, we want to let the admin know if 
            // the owner name is bad
            if (owner == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(key);
        }
            
        ShareInfoVisitor visitor = new ShareInfoVisitor(prov, response, null, null);
        
        // get shares published on the entry
        ShareInfo.Published.getPublished(prov, (DistributionList)taregtEntry, true, owner, visitor);
        visitor.finish();
        
        return response;
    }
    
    @Override
    protected void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getDistributionListShareInfo);
    }

}
