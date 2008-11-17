package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.TargetType;

public abstract class RightDocumentHandler extends AdminDocumentHandler {
    
    Entry getTargetEntry(Provisioning prov, Element eTarget, TargetType targetType) throws ServiceException {
        TargetBy targetBy = TargetBy.fromString(eTarget.getAttribute(AdminConstants.A_BY));
        String target = eTarget.getText();
         
        return TargetType.lookupTarget(prov, targetType, targetBy, target);
    }
    
    NamedEntry getGranteeEntry(Provisioning prov, Element eGrantee, GranteeType granteeType) throws ServiceException {
        if (!granteeType.allowedForAdminRights())
            throw ServiceException.INVALID_REQUEST("unsupported grantee type: " + granteeType.getCode(), null);
        
        GranteeBy granteeBy = GranteeBy.fromString(eGrantee.getAttribute(AdminConstants.A_BY));
        String grantee = eGrantee.getText();
        
        return GranteeType.lookupGrantee(prov, granteeType, granteeBy, grantee);
   }
    
}
