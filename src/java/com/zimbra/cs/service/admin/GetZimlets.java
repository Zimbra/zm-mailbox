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

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

public class GetZimlets extends AdminDocumentHandler  {

	private static final String TYPE_EXTENSION = "extension";
	
	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraContext lc = getZimbraContext(context);
        Account acct = getRequestedAccount(lc);
		
        String type = request.getAttribute(AdminService.A_T, null);

        Element response = lc.createElement(AdminService.GET_ZIMLETS_RESPONSE);
        Element zimlets = response.addUniqueElement(AccountService.E_ZIMLETS);
        doZimlets(zimlets, acct, type);
        
        return response;
    }

	private boolean checkZimlet(Zimlet z, String type) {
		if (z == null || !z.isEnabled()) {
			return false;
		} else if (type == null) {
			return !z.isExtension();
		} else if (type.equals(TYPE_EXTENSION)) {
			return z.isExtension();
		}
		return true;
	}
	
	private void doZimlets(Element response, Account acct, String type) throws ServiceException {
    	Cos cos = acct.getCOS();
    	String[] attrList = cos.getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
    	String zimletName = null;
    	for (int attrIndex = 0; attrIndex < attrList.length; attrIndex++) {
    		try {
    			zimletName = attrList[attrIndex];
    			Zimlet z = Provisioning.getInstance().getZimlet(zimletName);
    			if (checkZimlet(z, type)) {
    				ZimletUtil.listZimlet(response, zimletName);
    			}
    		} catch (ServiceException se) {
				ZimbraLog.zimlet.error("inconsistency in installed zimlets. "+zimletName+" does not exist.");
    		}
    	}
    	
    	// load the zimlets in the dev directory and list them
    	ZimletUtil.listDevZimlets(response);
    }
}
