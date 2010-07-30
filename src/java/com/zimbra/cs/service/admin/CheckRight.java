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

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GuestAccount;
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
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

public class CheckRight extends RightDocumentHandler {
    
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
        String granteeType = eGrantee.getAttribute(AdminConstants.A_TYPE, GranteeType.GT_USER.getCode());
        if (GranteeType.fromCode(granteeType) != GranteeType.GT_USER)
            throw ServiceException.INVALID_REQUEST("invalid grantee type " + granteeType, null);
        GranteeBy granteeBy = GranteeBy.fromString(eGrantee.getAttribute(AdminConstants.A_BY));
        String grantee = eGrantee.getText();

        Element eRight = request.getElement(AdminConstants.E_RIGHT);
        String right = eRight.getText();
        boolean deny = eRight.getAttributeBool(MailConstants.A_DENY, false);
        
        Element eAttrs = request.getOptionalElement(AdminConstants.E_ATTRS);
        Map<String, Object> attrs = (eAttrs==null)? null : AdminService.getAttrs(request);
        
        GuestAccount guest = null;
        if (!grantee.equals(zsc.getAuthtokenAccountId())) {
            boolean checked = checkCheckRightRight(zsc, GranteeType.GT_USER, granteeBy, grantee, true);
            if (!checked)
                guest = new GuestAccount(grantee, null);
        }
        
        ViaGrant via = new ViaGrant();
        boolean result = RightCommand.checkRight(Provisioning.getInstance(),
                                                 targetType, targetBy, target,
                                                 granteeBy, grantee, guest,
                                                 right, attrs,
                                                 via);
        
        Element resp = zsc.createElement(AdminConstants.CHECK_RIGHT_RESPONSE);
        
        resp.addAttribute(AdminConstants.A_ALLOW, result);
        if (via.available()) {
            Element eVia = resp.addElement(AdminConstants.E_VIA);
            
            Element eViaTarget = eVia.addElement(AdminConstants.E_TARGET);
            eViaTarget.addAttribute(AdminConstants.A_TYPE, via.getGranteeType());
            eViaTarget.setText(via.getTargetName());
            
            Element eViaGrantee = eVia.addElement(AdminConstants.E_GRANTEE);
            eViaGrantee.addAttribute(AdminConstants.A_TYPE, via.getGranteeType());
            eViaGrantee.setText(via.getGranteeName());
            
            Element eViaRight = eVia.addElement(AdminConstants.E_RIGHT);
            eViaRight.addAttribute(AdminConstants.A_DENY, via.isNegativeGrant());
            eViaRight.setText(via.getRight());
        }
        
        return resp;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_checkRightUsr);
    }

}
