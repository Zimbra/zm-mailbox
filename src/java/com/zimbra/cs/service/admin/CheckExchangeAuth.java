/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.ldap.Check;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class CheckExchangeAuth extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        // R_checkExchangeAuthConfig is a domain right.
        //
        // This SOAP does not take a domain, pass null to checkRight,
        // it'll check right on global grant.  If this SOAP supports a 
        // domain parameter and the admin has a R_checkExchangeAuthConfig 
        // grant on the domain, the domain grant will be effective.
        checkRight(zsc, context, null, Admin.R_checkExchangeAuthConfig);

        
        Check.Result r = Check.checkExchangeAuth(getAuthenticatedAccount(zsc));

	    Element response = zsc.createElement(AdminConstants.CHECK_EXCHANGE_AUTH_RESPONSE);
        response.addElement(AdminConstants.E_CODE).addText(r.getCode());
        String message = r.getMessage();
        if (message != null)
            response.addElement(AdminConstants.E_MESSAGE).addText(message);
	    return response;
	}
	
	@Override
    protected void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_checkExchangeAuthConfig);
        notes.add(Admin.R_checkExchangeAuthConfig.getName() + 
                " is a domain right.  However CheckExchangeAuth does not take a " + 
                "domain, thus the right has to be granted on the global grant " +
                "to be effective.");
    }
}