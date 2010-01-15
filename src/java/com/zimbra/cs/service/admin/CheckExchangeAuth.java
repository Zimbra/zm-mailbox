/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.ldap.Check;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider;
import com.zimbra.cs.fb.ExchangeFreeBusyProvider.AuthScheme;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class CheckExchangeAuth extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element auth = request.getElement(AdminConstants.E_AUTH);
        ExchangeFreeBusyProvider.ServerInfo sinfo = new ExchangeFreeBusyProvider.ServerInfo();
        sinfo.url = auth.getAttribute(AdminConstants.A_URL);
        sinfo.authUsername = auth.getAttribute(AdminConstants.A_USER);
        sinfo.authPassword = auth.getAttribute(AdminConstants.A_PASS);
        String scheme = auth.getAttribute(AdminConstants.A_SCHEME);
        sinfo.scheme = AuthScheme.valueOf(scheme);
        Check.Result r = Check.checkExchangeAuth(sinfo, getAuthenticatedAccount(lc));

	    Element response = lc.createElement(AdminConstants.CHECK_EXCHANGE_AUTH_RESPONSE);
        response.addElement(AdminConstants.E_CODE).addText(r.getCode());
        String message = r.getMessage();
        if (message != null)
            response.addElement(AdminConstants.E_MESSAGE).addText(message);
	    return response;
	}
}
