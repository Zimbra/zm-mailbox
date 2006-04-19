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
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetZimlets extends AdminDocumentHandler  {

    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraContext(context);
        Account acct = getRequestedAccount(lc);
		
        Element response = lc.createElement(AdminService.GET_ZIMLETS_RESPONSE);
        Element zimlets = response.addUniqueElement(AccountService.E_ZIMLETS);
        doExtensionZimlets(zimlets, acct);
        
        return response;
    }

	private void doExtensionZimlets(Element response, Account acct) throws ServiceException {
		Iterator zimlets = Provisioning.getInstance().listAllZimlets().iterator();
		while (zimlets.hasNext()) {
			Zimlet z = (Zimlet) zimlets.next();
			if (z.isExtension()) {
				ZimletUtil.listZimlet(response, z.getName(), -1);
			}
		}
    }
}
