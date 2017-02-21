/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.ldap.Check;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class CheckAuthConfig extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

	    String name = request.getAttribute(AdminConstants.E_NAME).toLowerCase();
	    String password = request.getAttribute(AdminConstants.E_PASSWORD);
	    Map attrs = AdminService.getAttrs(request, true);
        
        Element response = zsc.createElement(AdminConstants.CHECK_AUTH_CONFIG_RESPONSE);
        Provisioning.Result r = Provisioning.getInstance().checkAuthConfig(attrs, name, password);
        
        response.addElement(AdminConstants.E_CODE).addText(r.getCode());
        String message = r.getMessage();
        if (message != null)
            response.addElement(AdminConstants.E_MESSAGE).addText(message);
        response.addElement(AdminConstants.E_BINDDN).addText(r.getComputedDn());

	    return response;
	}
	
	@Override
	public void docRights(List<AdminRight> relatedRights, List<String> notes) {
	    notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }
}