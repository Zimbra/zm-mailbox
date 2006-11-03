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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Account;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyIdentity extends DocumentHandler {
	
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        
        Element identityEl = request.getElement(AccountService.E_IDENTITY);
        String name = identityEl.getAttribute(AccountService.A_NAME);
        Map<String,Object> attrs = AccountService.getAttrs(identityEl, AccountService.A_NAME);

        // remove anything that doesn't start with zimbraPref. ldap will also do additional checks
        List<String> toRemove = new ArrayList<String>();
        for (String key: attrs.keySet())
            if (!key.startsWith("zimbraPref")) // if this changes, make sure we don't let them ever change objectclass
                toRemove.add(key);
        
        for (String key : toRemove)
            attrs.remove(key);
        
        Provisioning.getInstance().modifyIdentity(account, name, attrs);
        
        Element response = zsc.createElement(AccountService.MODIFY_IDENTITY_RESPONSE);
        return response;
    }
}
