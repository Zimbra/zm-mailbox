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

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zimlet.ZimletException;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

public class ModifyZimlet extends AdminDocumentHandler {

	@Override
	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraContext lc = getZimbraContext(context);
        
		Element z = request.getElement(AdminService.E_ZIMLET);

		doAcl(z);
		doStatus(z);
		doPriority(z);

	    Element response = lc.createElement(AdminService.MODIFY_ZIMLET_RESPONSE);
		return response;
	}
	
	static void doAcl(Element z) throws ServiceException {
	    String name = z.getAttribute(AdminService.A_NAME);
        Element a = z.getElement(AdminService.E_ACL);
        String cos = a.getAttribute(AdminService.A_COS, null);
        if (cos == null) return;
        String acl = a.getAttribute(AdminService.A_ACL, null);
		acl = acl.toLowerCase();
		try {
			if (acl.equals("grant")) {
				ZimletUtil.activateZimlet(name, cos);
			} else if (acl.equals("deny")) {
				ZimletUtil.deactivateZimlet(name, cos);
			}
		} catch (ZimletException ze) {
			throw ServiceException.FAILURE("cannot modify acl", ze);
		}
	}

	static void doStatus(Element z) throws ServiceException {
	    String name = z.getAttribute(AdminService.A_NAME);
        Element s = z.getElement(AdminService.E_STATUS);
        String val = s.getAttribute(AdminService.A_VALUE, null);
        if (val == null) return;
	    boolean status = val.equalsIgnoreCase("enabled");

		try {
			ZimletUtil.setZimletEnable(name, status);
		} catch (ZimletException ze) {
			throw ServiceException.FAILURE("cannot modify status", ze);
		}
	}

	static void doPriority(Element z) throws ServiceException {
	    String name = z.getAttribute(AdminService.A_NAME);
        Element p = z.getElement(AdminService.E_PRIORITY);
        int val = (int)p.getAttributeLong(AdminService.A_VALUE, -1);
        if (val == -1) return;

		ZimletUtil.setPriority(name, val);
	}
}
