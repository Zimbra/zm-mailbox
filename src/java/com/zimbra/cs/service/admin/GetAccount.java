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

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetAccount extends AdminDocumentHandler {

    /**
     * must be careful and only return accounts a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        boolean applyCos = request.getAttributeBool(AdminService.A_APPLY_COS, true);        
        Element a = request.getElement(AdminService.E_ACCOUNT);
	    String key = a.getAttribute(AdminService.A_BY);
        String value = a.getText();

	    Account account = null;

        if (key.equals(AdminService.BY_NAME)) {
            account = prov.get(AccountBy.NAME, value);
        } else if (key.equals(AdminService.BY_ID)) {
            account = prov.get(AccountBy.ID, value);
        } else if (key.equals(AdminService.BY_ADMIN_NAME)) {
            account = prov.get(AccountBy.ADMIN_NAME, value);
        } else if (key.equals(AdminService.BY_FOREIGN_PRINCIPAL)) {
            account = prov.get(AccountBy.FOREIGN_PRINCIPAL, value);
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);

        if (!canAccessAccount(lc, account))
            throw ServiceException.PERM_DENIED("can not access account");

	    Element response = lc.createElement(AdminService.GET_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, account, applyCos);

	    return response;
	}
}
