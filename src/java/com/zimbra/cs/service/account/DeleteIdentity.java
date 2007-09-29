/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.IdentityBy;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class DeleteIdentity extends DocumentHandler {
	
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        
        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
 
        Provisioning prov = Provisioning.getInstance();

        Element eIdentity = request.getElement(AccountService.E_IDENTITY);

        // identity can be specified by name or by ID
        Identity ident = null;
        String id = eIdentity.getAttribute(AccountService.A_ID, null);
        if (id != null)
            ident = prov.get(account, IdentityBy.id, id);
        else
            ident = prov.get(account, IdentityBy.name, eIdentity.getAttribute(AccountService.A_NAME));

        if (ident != null)
            Provisioning.getInstance().deleteIdentity(account, ident.getName());

        Element response = zsc.createElement(AccountService.DELETE_IDENTITY_RESPONSE);
        return response;
    }
}