/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.soap.ZimbraSoapContext;

public class CheckPermission extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element eTarget = request.getElement(AdminConstants.E_TARGET);
        String targetType = eTarget.getAttribute(AdminConstants.A_TYPE);
        String targetBy = eTarget.getAttribute(AdminConstants.A_BY);
        String targetValue = eTarget.getText();
        
        NamedEntry entry = null;
        if (targetType.equals(AdminConstants.A_ACCOUNT)) {
            entry = prov.get(AccountBy.fromString(targetBy), targetValue, zsc.getAuthToken());
            if (entry == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(targetValue);
                
            /*
            if (!canAccessAccount(zsc, (Account)entry))
                throw ServiceException.PERM_DENIED("can not access account");
            */
        } else if (targetType.equals(AdminConstants.A_CALENDAR_RESOURCE)) {
            entry = prov.get(CalendarResourceBy.fromString(targetBy), targetValue);
            if (entry == null)
                throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(targetValue);
        } else if (targetType.equals(AdminConstants.A_COS)) {
            entry = prov.get(CosBy.fromString(targetBy), targetValue);
            if (entry == null)
                throw AccountServiceException.NO_SUCH_COS(targetValue);
        } else if (targetType.equals(AdminConstants.A_DISTRIBUTION_LIST)) {
            entry = prov.get(DistributionListBy.fromString(targetBy), targetValue);
            if (entry == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(targetValue);
        } else if (targetType.equals(AdminConstants.A_DOMAIN)) {
            entry = prov.get(DomainBy.fromString(targetBy), targetValue);
            if (entry == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(targetValue);
        } else if (targetType.equals(AdminConstants.A_SERVER)) {
            entry = prov.get(ServerBy.fromString(targetBy), targetValue);
            if (entry == null)
                throw AccountServiceException.NO_SUCH_SERVER(targetValue);
        } else
            throw ServiceException.INVALID_REQUEST("invalid target type: " + targetType, null);

        
        Element ePrincipal = request.getElement(AdminConstants.E_PRINCIPAL);
        String principalBy = ePrincipal.getAttribute(AdminConstants.A_BY);
        String principalValue = ePrincipal.getText();

        // look for the principal if it is not identified by name
        AccountBy by = AccountBy.fromString(principalBy);
        if (by != AccountBy.name) {
            Account principal = prov.get(by, principalValue, zsc.getAuthToken());
            if (principal == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(principalValue);
            principalValue = principal.getName();
        }
        
        Element r = request.getElement(AdminConstants.E_RIGHT);
        Right right = RightManager.getInstance().getRight(r.getText());
        
        if (!AccessManager.getInstance().canPerform(principalValue, entry, right, false, false))
            throw ServiceException.PERM_DENIED("credential " + principalValue + " is not allowed for right " + right.getName() + " on target " + entry.getName());

        Element response = zsc.createElement(AdminConstants.CHECK_PERMISSION_RESPONSE);

        return response;
    }
}
