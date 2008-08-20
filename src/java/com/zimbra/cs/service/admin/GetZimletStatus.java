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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class GetZimletStatus extends AdminDocumentHandler {

	@Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
		Provisioning prov = Provisioning.getInstance();
		
        Element response = lc.createElement(AdminConstants.GET_ZIMLET_STATUS_RESPONSE);
        Element elem = response.addElement(AccountConstants.E_ZIMLETS);
		@SuppressWarnings({"unchecked"})
		List<Zimlet> zimlets = prov.listAllZimlets();
    	zimlets = ZimletUtil.orderZimletsByPriority(zimlets);
    	int priority = 0;
		for (Zimlet z : zimlets) {
			doZimlet(z, elem, priority++);
		}
        
		Iterator<Cos> cos = prov.getAllCos().iterator();
		while (cos.hasNext()) {
			Cos c = (Cos) cos.next();
			elem = response.addElement(AdminConstants.E_COS);
			elem.addAttribute(AdminConstants.E_NAME, c.getName());
			String[] z = ZimletUtil.getZimlets(c);
			for (int i = 0; i < z.length; i++) {
				doZimlet(prov.getZimlet(z[i]), elem, -1);
			}
		}
        return response;
    }

	private void doZimlet(Zimlet z, Element elem, int priority) {
		if (z == null)
			return;
        Element zim = elem.addElement(AccountConstants.E_ZIMLET);
		zim.addAttribute(AdminConstants.A_NAME, z.getName());
		zim.addAttribute(AdminConstants.A_STATUS, (z.isEnabled() ? "enabled" : "disabled"));
		zim.addAttribute(AdminConstants.A_EXTENSION, (z.isExtension() ? "true" : "false"));
		if (priority >= 0) {
			zim.addAttribute(AdminConstants.A_PRIORITY, priority);
		}
    }
}
