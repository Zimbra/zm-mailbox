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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
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

public class CheckPermission extends MailDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element eTarget = request.getElement(MailConstants.E_TARGET);
        String targetType = eTarget.getAttribute(MailConstants.A_TARGET_TYPE);
        String targetBy = eTarget.getAttribute(MailConstants.A_TARGET_BY);
        String targetValue = eTarget.getText();
        
        NamedEntry entry = null;
        if (targetType.equals(MailConstants.A_ACCOUNT)) {
            entry = prov.get(AccountBy.fromString(targetBy), targetValue, zsc.getAuthToken());
            if (entry == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(targetValue);
        } else if (targetType.equals(MailConstants.A_CALENDAR_RESOURCE)) {
            entry = prov.get(CalendarResourceBy.fromString(targetBy), targetValue);
            if (entry == null)
                throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(targetValue);
        } else if (targetType.equals(MailConstants.A_DISTRIBUTION_LIST)) {
            entry = prov.get(DistributionListBy.fromString(targetBy), targetValue);
            if (entry == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(targetValue);
        } else
            throw ServiceException.INVALID_REQUEST("invalid target type: " + targetType, null);
        
        Element r = request.getElement(MailConstants.E_RIGHT);
        Right right = RightManager.getInstance().getUserRight(r.getText());
        
        if (!AccessManager.getInstance().canPerform(zsc.getAuthToken(), entry, right, false, false))
            throw ServiceException.PERM_DENIED("credential " + zsc.getAuthtokenAccountId() + " is not allowed for right " + right.getName() + " on target " + entry.getName());

        Element response = zsc.createElement(MailConstants.CHECK_PERMISSION_RESPONSE);

        return response;
    }
}
