package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.PermUtil;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

public class GrantPermission extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Element eTarget = request.getElement(AdminConstants.E_TARGET);
        TargetType targetType = TargetType.fromString(eTarget.getAttribute(AdminConstants.A_TYPE));
        NamedEntry targetEntry = null;
        if (targetType.needsTargetIdentity())
            targetEntry = getTargetEntry(prov, eTarget, targetType);
            
        Element eGrantee = request.getElement(AdminConstants.E_GRANTEE);
        GranteeType granteeType = GranteeType.fromCode(eGrantee.getAttribute(AdminConstants.A_TYPE));
        NamedEntry granteeEntry = getGranteeEntry(prov, eGrantee, granteeType);
        
        Element eRight = request.getElement(AdminConstants.E_RIGHT);
        Right right = RightManager.getInstance().getRight(eRight.getText());
        boolean deny = eRight.getAttributeBool(MailConstants.A_DENY, false);
        
        prov.grantPermission(targetType, targetEntry, granteeType, granteeEntry, right, deny);
        
        Element response = zsc.createElement(AdminConstants.GRANT_PERMISSION_RESPONSE);
        return response;
    }
    
    static NamedEntry getTargetEntry(Provisioning prov, Element eTarget, TargetType targetType) throws ServiceException {
        TargetBy targetBy = TargetBy.fromString(eTarget.getAttribute(AdminConstants.A_BY));
        String target = eTarget.getText();
         
        return PermUtil.lookupTarget(prov, targetType, targetBy, target);
    }
    
    static NamedEntry getGranteeEntry(Provisioning prov, Element eGrantee, GranteeType granteeType) throws ServiceException {
        if (!granteeType.allowedForAdminRights())
            throw ServiceException.INVALID_REQUEST("unsupported grantee type: " + granteeType.getCode(), null);
        
        GranteeBy granteeBy = GranteeBy.fromString(eGrantee.getAttribute(AdminConstants.A_BY));
        String grantee = eGrantee.getText();
        
        return PermUtil.lookupGrantee(prov, granteeType, granteeBy, grantee);
   }

}
