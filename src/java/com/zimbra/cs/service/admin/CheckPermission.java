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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.soap.ZimbraSoapContext;

public class CheckPermission extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ACCOUNT };
    protected String[] getProxiedAccountElementPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only return accounts a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element a = request.getElement(AdminConstants.E_ACCOUNT);
        String acctBy = a.getAttribute(AdminConstants.A_BY);
        String acctValue = a.getText();

        Account account = prov.get(AccountBy.fromString(acctBy), acctValue, zsc.getAuthToken());

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(acctValue);
        
        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
        Element p = request.getElement(AdminConstants.E_PRINCIPAL);
        String principalBy = p.getAttribute(AdminConstants.A_BY);
        String principalValue = p.getText();

        // look for the principal if it is not identified by name
        AccountBy by = AccountBy.fromString(principalBy);
        if (by != AccountBy.name) {
            Account principal = prov.get(by, principalValue, zsc.getAuthToken());
            if (principal == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(principalValue);
            principalValue = principal.getName();
        }
        
        Element r = request.getElement(AdminConstants.E_RIGHT);
        Right right = Right.fromCode(r.getText());
        
        if (!AccessManager.getInstance().canPerform(principalValue, account, right, false, false))
            throw ServiceException.PERM_DENIED("credential " + principalValue + " is not allowed for right " + right.getCode() + " on target " + account.getName());

        Element response = zsc.createElement(AdminConstants.CHECK_PERMISSION_RESPONSE);

        return response;
    }
}
