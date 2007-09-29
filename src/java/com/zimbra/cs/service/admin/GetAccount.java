/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
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

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminService.E_ACCOUNT };
    protected String[] getProxiedAccountElementPath()  { return TARGET_ACCOUNT_PATH; }

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

	    Account account = prov.get(AccountBy.fromString(key), value);

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);

        if (!canAccessAccount(lc, account))
            throw ServiceException.PERM_DENIED("can not access account");

	    Element response = lc.createElement(AdminService.GET_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, account, applyCos);

	    return response;
	}
}
