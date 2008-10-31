package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

public class RevokePermission extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Element eTarget = request.getElement(AdminConstants.E_TARGET);
        TargetType targetType = TargetType.fromString(eTarget.getAttribute(AdminConstants.A_TYPE));
        NamedEntry targetEntry = null;
        if (targetType.needsTargetIdentity())
            targetEntry = GrantPermission.getTargetEntry(prov, eTarget, targetType);
            
        Element eGrantee = request.getElement(AdminConstants.E_GRANTEE);
        GranteeType granteeType = GranteeType.fromCode(eGrantee.getAttribute(AdminConstants.A_TYPE));
        NamedEntry granteeEntry = GrantPermission.getGranteeEntry(prov, eGrantee, granteeType);
        
        Element eRight = request.getElement(AdminConstants.E_RIGHT);
        Right right = RightManager.getInstance().getRight(eRight.getText());
        boolean deny = eRight.getAttributeBool(MailConstants.A_DENY, false);
        
        prov.revokePermission(targetType, targetEntry, granteeType, granteeEntry, right, deny);
        
        Element response = zsc.createElement(AdminConstants.GRANT_PERMISSION_RESPONSE);
        return response;
    }

}
