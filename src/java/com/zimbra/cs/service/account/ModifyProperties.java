/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.zimlet.ZimletUserProperties;

/**
 * @author jylee
 */
public class ModifyProperties extends AccountDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
        
        if (!canAccessAccount(lc, acct))
            throw ServiceException.PERM_DENIED("can not access account");

        ZimletUserProperties props = ZimletUserProperties.getProperties(acct);

        for (Element e : request.listElements(AccountService.E_PROPERTY)) {
            props.setProperty(e.getAttribute(AccountService.A_ZIMLET),
            					e.getAttribute(AccountService.A_NAME),
            					e.getText());
        }
        props.saveProperties(acct);
        Element response = lc.createElement(AccountService.MODIFY_PROPERTIES_RESPONSE);
        return response;
	}
}
