/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.IdentityBy;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyIdentity extends DocumentHandler {
	
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        Provisioning prov = Provisioning.getInstance();

        Element eIdentity = request.getElement(AccountService.E_IDENTITY);
        Map<String,Object> attrs = AccountService.getAttrs(eIdentity, AccountService.A_NAME);
        
        // remove anything that doesn't start with zimbraPref. ldap will also do additional checks
        for (Iterator<String> it = attrs.keySet().iterator(); it.hasNext(); )
            if (!it.next().toLowerCase().startsWith("zimbrapref")) // if this changes, make sure we don't let them ever change objectclass
                it.remove();

        Identity ident = null;
        String key, id = eIdentity.getAttribute(AccountService.A_ID, null);
        if (id != null) {
            ident = prov.get(account, IdentityBy.id, key = id);
        } else {
            ident = prov.get(account, IdentityBy.name, key = eIdentity.getAttribute(AccountService.A_NAME));
        }
        if (ident == null)
            throw AccountServiceException.NO_SUCH_IDENTITY(key);

        prov.modifyIdentity(account, ident.getId(), attrs);
        
        Element response = zsc.createElement(AccountService.MODIFY_IDENTITY_RESPONSE);
        return response;
    }
}
