/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.account;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.IdentityBy;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyIdentity extends DocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        Identity identity = null;
        
        if (!canModifyOptions(zsc, account))
            throw ServiceException.PERM_DENIED("can not modify options");
        
        Provisioning prov = Provisioning.getInstance();

        Element eIdentity = request.getElement(AccountConstants.E_IDENTITY);
        Map<String,Object> attrs = AccountService.getAttrs(eIdentity, AccountConstants.A_NAME);
        
        // remove anything that doesn't start with zimbraPref. ldap will also do additional checks
        for (Iterator<String> it = attrs.keySet().iterator(); it.hasNext(); )
            if (!it.next().toLowerCase().startsWith("zimbrapref")) // if this changes, make sure we don't let them ever change objectclass
                it.remove();

        String key, id = eIdentity.getAttribute(AccountConstants.A_ID, null);
        if (id != null) {
            identity = prov.get(account, IdentityBy.id, key = id);
        } else {
            identity = prov.get(account, IdentityBy.name, key = eIdentity.getAttribute(AccountConstants.A_NAME));
        }

        if (identity == null) {
            String[] childIds = account.getChildAccount();
            for (String childId : childIds) {
                Account childAccount = prov.get(AccountBy.id, childId, zsc.getAuthToken());
                if (childAccount != null) {
                    Identity childIdentity;

                    if (id != null) {
                        childIdentity = prov.get(childAccount, IdentityBy.id, key = id);
                    } else {
                        childIdentity = prov.get(childAccount, IdentityBy.name, key = eIdentity.getAttribute(AccountConstants.A_NAME));
                    }

                    if (childIdentity != null) {
                        identity = childIdentity;
                        account = childAccount;
                        break;
                    }
                }
            }
        }

        if (identity == null)
            throw AccountServiceException.NO_SUCH_IDENTITY(key);

        prov.modifyIdentity(account, identity.getName(), attrs);
        
        Element response = zsc.createElement(AccountConstants.MODIFY_IDENTITY_RESPONSE);
        return response;
    }
}
