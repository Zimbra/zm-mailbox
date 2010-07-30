/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class RevokeRight extends RightDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element eTarget = request.getElement(AdminConstants.E_TARGET);
        String targetType = eTarget.getAttribute(AdminConstants.A_TYPE);
        TargetBy targetBy = null;
        String target = null;
        if (TargetType.fromCode(targetType).needsTargetIdentity()) {
            targetBy = TargetBy.fromString(eTarget.getAttribute(AdminConstants.A_BY));
            target = eTarget.getText();
        }
            
        Element eGrantee = request.getElement(AdminConstants.E_GRANTEE);
        String granteeType = eGrantee.getAttribute(AdminConstants.A_TYPE);
        GranteeBy granteeBy = null;
        String grantee = null;
        if (GranteeType.fromCode(granteeType).needsGranteeIdentity()) {
            granteeBy = GranteeBy.fromString(eGrantee.getAttribute(AdminConstants.A_BY));
            grantee   = eGrantee.getText();
        }
        
        Element eRight = request.getElement(AdminConstants.E_RIGHT);
        String right = eRight.getText();
        
        RightModifier rightModifier = GrantRight.getRightModifier(eRight);
        
        // right checking is done in RightCommand
        
        RightCommand.revokeRight(Provisioning.getInstance(),
                                 getAuthenticatedAccount(zsc),
                                 targetType, targetBy, target,
                                 granteeType, granteeBy, grantee,
                                 right, rightModifier);
        
        Element response = zsc.createElement(AdminConstants.REVOKE_RIGHT_RESPONSE);
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Grantor must have the same or more rights on the same target or " + 
                "on a larger target set.");
    }
}
