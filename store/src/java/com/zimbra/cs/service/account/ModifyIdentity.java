/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.IdentityBy;
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
            identity = prov.get(account, Key.IdentityBy.id, key = id);
        } else {
            identity = prov.get(account, Key.IdentityBy.name, key = eIdentity.getAttribute(AccountConstants.A_NAME));
        }

        if (identity == null) {
            String[] childIds = account.getChildAccount();
            for (String childId : childIds) {
                Account childAccount = prov.get(AccountBy.id, childId, zsc.getAuthToken());
                if (childAccount != null) {
                    Identity childIdentity;

                    if (id != null) {
                        childIdentity = prov.get(childAccount, Key.IdentityBy.id, key = id);
                    } else {
                        childIdentity = prov.get(childAccount, Key.IdentityBy.name, key = eIdentity.getAttribute(AccountConstants.A_NAME));
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
