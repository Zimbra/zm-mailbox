/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GrantRightRequest;
import com.zimbra.soap.admin.type.RightModifierInfo;
import com.zimbra.soap.type.ZmBoolean;

public class GrantRight extends RightDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        GrantRightRequest grReq = JaxbUtil.elementToJaxb(request);
        RightModifierInfo modifierInfo = grReq.getRight();
        if (modifierInfo == null) {
            throw ServiceException.INVALID_REQUEST("No information specified on what right to assign", null);
        }
        RightModifier rightModifier = getRightModifier(modifierInfo);

        // right checking is done in RightCommand

        RightCommand.grantRight(Provisioning.getInstance(), getAuthenticatedAccount(zsc), grReq.getTarget(),
                                grReq.getGrantee(), modifierInfo.getValue(), rightModifier);

        Element response = zsc.createElement(AdminConstants.GRANT_RIGHT_RESPONSE);
        return response;
    }

    static RightModifier getRightModifier(RightModifierInfo eRight) throws ServiceException {
        boolean deny = ZmBoolean.toBool(eRight.getDeny(), false);
        boolean canDelegate = ZmBoolean.toBool(eRight.getCanDelegate(), false);
        boolean disinheritSubGroups = ZmBoolean.toBool(eRight.getDisinheritSubGroups(), false);
        boolean subDomain = ZmBoolean.toBool(eRight.getSubDomain(), false);

        int numModifiers = 0;
        if (deny) {
            numModifiers++;
        }
        if (canDelegate) {
            numModifiers++;
        }
        if (disinheritSubGroups) {
            numModifiers++;
        }
        if (subDomain) {
            numModifiers++;
        }

        if (numModifiers > 1) {
            throw ServiceException.INVALID_REQUEST("can only have one modifier", null);
        }

        RightModifier rightModifier = null;
        if (deny) {
            rightModifier = RightModifier.RM_DENY;
        } else if (canDelegate) {
            rightModifier = RightModifier.RM_CAN_DELEGATE;
        } else if (disinheritSubGroups) {
            rightModifier = RightModifier.RM_DISINHERIT_SUB_GROUPS;
        } else if (subDomain) {
            rightModifier = RightModifier.RM_SUBDOMAIN;
        }
        return rightModifier;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Grantor must have the same or more rights on the same target or " +
                "on a larger target set.");
    }

}
