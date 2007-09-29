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
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class ModifyPrefs extends AccountDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
        
        if (!canAccessAccount(lc, acct))
            throw ServiceException.PERM_DENIED("can not access account");

        HashMap<String, String> prefs = new HashMap<String, String>();
//        HashMap specialPrefs = new HashMap();

        for (Element e : request.listElements(AccountService.E_PREF)) {
            String name = e.getAttribute(AccountService.A_NAME);
            String value = e.getText();
		    if (!name.startsWith("zimbraPref")) {
		        throw ServiceException.INVALID_REQUEST("pref name must start with zimbraPref", null);
		    }
		    prefs.put(name, value);
        }
        if (prefs.containsKey(Provisioning.A_zimbraPrefMailForwardingAddress)) {
            if (!acct.getBooleanAttr(Provisioning.A_zimbraFeatureMailForwardingEnabled, false)) {
                throw ServiceException.PERM_DENIED("forwarding not enabled");
            }
        }
        // call modifyAttrs and pass true to checkImmutable
        
        Provisioning.getInstance().modifyAttrs(acct, prefs, true);
        Element response = lc.createElement(AccountService.MODIFY_PREFS_RESPONSE);
        return response;
	}
}
