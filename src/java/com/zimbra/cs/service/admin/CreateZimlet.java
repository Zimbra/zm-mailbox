/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class CreateZimlet extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext lc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
	    String name = request.getAttribute(AdminConstants.E_NAME).toLowerCase();
	    Map<String, Object> attrs = AdminService.getAttrs(request, true);

	    checkRight(lc, context, null, Admin.R_createZimlet);
        checkSetAttrsOnCreate(lc, TargetType.zimlet, name, attrs);
        
	    Zimlet zimlet = prov.createZimlet(name, attrs);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateZimlet","name", name}, attrs));

	    Element response = lc.createElement(AdminConstants.CREATE_ZIMLET_RESPONSE);
	    GetZimlet.encodeZimlet(response, zimlet);

	    return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_createZimlet);
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyZimlet.getName(), "zimlet"));
    }
}