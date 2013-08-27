/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
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
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.type.TargetBy;

public class CheckRight extends RightDocumentHandler {

    @Override
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
        GranteeType granteeType = GranteeType.fromCode(
                eGrantee.getAttribute(AdminConstants.A_TYPE, GranteeType.GT_EMAIL.getCode()));
        if ((granteeType != GranteeType.GT_USER) && (granteeType != GranteeType.GT_EMAIL)) {
            throw ServiceException.INVALID_REQUEST("invalid grantee type " + granteeType, null);
        }
        GranteeBy granteeBy = GranteeBy.fromString(eGrantee.getAttribute(AdminConstants.A_BY));
        String granteeVal = eGrantee.getText();

        Element eRight = request.getElement(AdminConstants.E_RIGHT);
        String right = eRight.getText();

        Element eAttrs = request.getOptionalElement(AdminConstants.E_ATTRS);
        Map<String, Object> attrs = (eAttrs==null)? null : AdminService.getAttrs(request);

        MailTarget grantee = null;
        NamedEntry ne = GranteeType.lookupGrantee(Provisioning.getInstance(), granteeType, granteeBy, granteeVal);
        if (ne instanceof MailTarget) {
            grantee = (MailTarget) ne;
        } else {
            grantee = new GuestAccount(granteeVal, null);
        }

        if (!granteeVal.equals(zsc.getAuthtokenAccountId())) {
            /* Make sure authenticated account has the right to check rights for this grantee.
             */
            checkCheckRightRight(zsc, (grantee instanceof Account) ? GranteeType.GT_USER : GranteeType.GT_GROUP,
                    granteeBy, granteeVal, true);
        }
        ViaGrant via = new ViaGrant();
        boolean result = RightCommand.checkRight(Provisioning.getInstance(), targetType, targetBy, target,
                            grantee, right, attrs, via);

        Element resp = zsc.createElement(AdminConstants.CHECK_RIGHT_RESPONSE);

        resp.addAttribute(AdminConstants.A_ALLOW, result);
        if (via.available()) {
            Element eVia = resp.addElement(AdminConstants.E_VIA);

            Element eViaTarget = eVia.addElement(AdminConstants.E_TARGET);
            eViaTarget.addAttribute(AdminConstants.A_TYPE, via.getTargetType());
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
