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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author schemers
 */
public class ModifyPrefs extends WriteOpDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraContext(context);
        Account acct = getRequestedAccount(lc);

        HashMap prefs = new HashMap();
        HashMap specialPrefs = new HashMap();
        
        for (Iterator it = request.elementIterator(AccountService.E_PREF); it.hasNext(); ) {
            Element e = (Element) it.next();
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
        acct.modifyAttrs(prefs, true);
        Element response = lc.createElement(AccountService.MODIFY_PREFS_RESPONSE);
        return response;
	}
}
