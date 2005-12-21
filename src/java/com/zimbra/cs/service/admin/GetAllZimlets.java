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

import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

public class GetAllZimlets extends AdminDocumentHandler {

	@Override
    public Element handle(Element request, Map context) throws ServiceException {
		ZimbraContext lc = getZimbraContext(context);
		Provisioning prov = Provisioning.getInstance();
		
        Element response = lc.createElement(AdminService.GET_ALL_ZIMLETS_RESPONSE);
        Element elem = response.addElement(AccountService.E_ZIMLETS);
		Iterator zimlets = prov.listAllZimlets().iterator();
		while (zimlets.hasNext()) {
			Zimlet z = (Zimlet) zimlets.next();
			doZimlet(z, elem);
		}
        
		Iterator cos = prov.getAllCos().iterator();
		while (cos.hasNext()) {
			Cos c = (Cos) cos.next();
			elem = response.addElement(AdminService.E_COS);
			elem.addAttribute(AdminService.E_NAME, c.getName());
			String[] z = c.getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
			for (int i = 0; i < z.length; i++) {
				doZimlet(prov.getZimlet(z[i]), elem);
			}
		}
        return response;
    }

	private void doZimlet(Zimlet z, Element elem) throws ServiceException {
	        Element zim = elem.addElement(AccountService.E_ZIMLET);
			zim.addAttribute(AdminService.A_NAME, z.getName());
			zim.addAttribute(AdminService.A_STATUS, (z.isEnabled() ? "enabled" : "disabled"));
			zim.addAttribute(AdminService.A_EXTENSION, (z.isExtension() ? "true" : "false"));
			zim.addAttribute(AdminService.A_PRIORITY, z.getPriority());
    }
}
