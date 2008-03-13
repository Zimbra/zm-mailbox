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
 * Created on Sep 3, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class ChangePassword extends AccountDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        String name = request.getAttribute(AccountConstants.E_ACCOUNT);
        
        Element virtualHostEl = request.getOptionalElement(AccountConstants.E_VIRTUAL_HOST);
        String virtualHost = virtualHostEl == null ? null : virtualHostEl.getText().toLowerCase();
        
        if (virtualHost != null && name.indexOf('@') == -1) {
            Domain d = prov.get(DomainBy.virtualHostname, virtualHost);
            if (d != null)
                name = name + "@" + d.getName();
        }
        
        Account acct = prov.get(AccountBy.name, name, zsc.getAuthToken());
        if (acct == null)
            throw AuthFailedServiceException.AUTH_FAILED(name, "account not found");
		String oldPassword = request.getAttribute(AccountConstants.E_OLD_PASSWORD);
		String newPassword = request.getAttribute(AccountConstants.E_PASSWORD);
		prov.changePassword(acct, oldPassword, newPassword);

        Element response = zsc.createElement(AccountConstants.CHANGE_PASSWORD_RESPONSE);
        return response;
	}

    public boolean needsAuth(Map<String, Object> context) {
        // This command can be sent before authenticating, so this method
        // should return false.  The Account.changePassword() method called
        // from handle() will internally make sure the old password provided
        // matches the current password of the account.
        //
        // The user identity in the auth token, if any, is ignored.
        return false;
    }
}
