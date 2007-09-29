/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

import com.zimbra.cs.service.account.ToXML;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class CreateAccount extends AdminDocumentHandler {

    /**
     * must be careful and only create accounts for the domain admin!
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String name = request.getAttribute(AdminService.E_NAME).toLowerCase();
	    String password = request.getAttribute(AdminService.E_PASSWORD, null);
	    Map<String, Object> attrs = AdminService.getAttrs(request, true);

        if (!canAccessEmail(lc, name))
            throw ServiceException.PERM_DENIED("can not access account:"+name);

	    Account account = prov.createAccount(name, password, attrs);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateAccount","name", name}, attrs));         

	    Element response = lc.createElement(AdminService.CREATE_ACCOUNT_RESPONSE);

        ToXML.encodeAccount(response, account);

	    return response;
	}
}