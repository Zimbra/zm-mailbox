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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SignatureBy;
import com.zimbra.cs.account.Signature;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifySignature extends DocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        
        canModifyOptions(zsc, account);
        
        Provisioning prov = Provisioning.getInstance();

        Element eSignature = request.getElement(AccountConstants.E_SIGNATURE);
        Map<String,Object> attrs = AccountService.getAttrs(eSignature, AccountConstants.A_NAME);
        
        // remove anything that doesn't start with zimbraPref. ldap will also do additional checks
        for (Iterator<String> it = attrs.keySet().iterator(); it.hasNext(); )
            if (!it.next().toLowerCase().startsWith("zimbrapref")) // if this changes, make sure we don't let them ever change objectclass
                it.remove();

        Signature signature = null;
        String key, id = eSignature.getAttribute(AccountConstants.A_ID, null);
        if (id != null) {
            signature = prov.get(account, SignatureBy.id, key = id);
        } else {
            signature = prov.get(account, SignatureBy.name, key = eSignature.getAttribute(AccountConstants.A_NAME));
        }
        if (signature == null)
            throw AccountServiceException.NO_SUCH_SIGNATURE(key);

        prov.modifySignature(account, signature.getName(), attrs);
        
        Element response = zsc.createElement(AccountConstants.MODIFY_SIGNATURE_RESPONSE);
        return response;
    }
}
