package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

public class GrantRight extends RightDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element eTarget = request.getElement(AdminConstants.E_TARGET);
        String targetType = eTarget.getAttribute(AdminConstants.A_TYPE);
        TargetBy targetBy = null;
        String target = null;
        if (TargetType.fromString(targetType).needsTargetIdentity()) {
            targetBy = TargetBy.fromString(eTarget.getAttribute(AdminConstants.A_BY));
            target = eTarget.getText();
        }
            
        Element eGrantee = request.getElement(AdminConstants.E_GRANTEE);
        String granteeType = eGrantee.getAttribute(AdminConstants.A_TYPE);
        GranteeBy granteeBy = GranteeBy.fromString(eGrantee.getAttribute(AdminConstants.A_BY));
        String grantee = eGrantee.getText();
        
        Element eRight = request.getElement(AdminConstants.E_RIGHT);
        String right = eRight.getText();
        
        RightModifier rightModifier = getRightModifier(eRight);
        
        RightCommand.grantRight(Provisioning.getInstance(),
                                targetType, targetBy, target,
                                granteeType, granteeBy, grantee,
                                right, rightModifier);
        
        Element response = zsc.createElement(AdminConstants.GRANT_RIGHT_RESPONSE);
        return response;
    }

    static RightModifier getRightModifier(Element eRight) throws ServiceException {
        boolean deny = eRight.getAttributeBool(AdminConstants.A_DENY, false);
        boolean canDelegate = eRight.getAttributeBool(AdminConstants.A_CAN_DELEGATE, false);
        
        if (deny && canDelegate)
            throw ServiceException.INVALID_REQUEST("cannot have both deny and canDelegate right modifiers", null);
        
        RightModifier rightModifier = null;
        if (deny)
            rightModifier = RightModifier.RM_DENY;
        else if (canDelegate)
            rightModifier = RightModifier.RM_CAN_DELEGATE;
        
        return rightModifier;
    }

}
