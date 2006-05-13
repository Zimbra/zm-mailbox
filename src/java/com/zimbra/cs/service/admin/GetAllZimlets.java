/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.2
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimlets
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllZimlets extends AdminDocumentHandler {

	@Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
		Provisioning prov = Provisioning.getInstance();
		
        Element response = lc.createElement(AdminService.GET_ALL_ZIMLETS_RESPONSE);
        Element elem = response.addElement(AccountService.E_ZIMLETS);
		@SuppressWarnings({"unchecked"})
		List<Zimlet> zimlets = prov.listAllZimlets();
    	zimlets = ZimletUtil.orderZimletsByPriority(zimlets);
    	int priority = 0;
		for (Zimlet z : zimlets) {
			doZimlet(z, elem, priority++);
		}
        
		Iterator cos = prov.getAllCos().iterator();
		while (cos.hasNext()) {
			Cos c = (Cos) cos.next();
			elem = response.addElement(AdminService.E_COS);
			elem.addAttribute(AdminService.E_NAME, c.getName());
			String[] z = c.getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
			for (int i = 0; i < z.length; i++) {
				doZimlet(prov.getZimlet(z[i]), elem, -1);
			}
		}
        return response;
    }

	private void doZimlet(Zimlet z, Element elem, int priority) {
        Element zim = elem.addElement(AccountService.E_ZIMLET);
		zim.addAttribute(AdminService.A_NAME, z.getName());
		zim.addAttribute(AdminService.A_STATUS, (z.isEnabled() ? "enabled" : "disabled"));
		zim.addAttribute(AdminService.A_EXTENSION, (z.isExtension() ? "true" : "false"));
		if (priority >= 0) {
			zim.addAttribute(AdminService.A_PRIORITY, priority);
		}
    }
}
