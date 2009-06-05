/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
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
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class CreateCos extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
	    String name = request.getAttribute(AdminConstants.E_NAME).toLowerCase();
	    Map<String, Object> attrs = AdminService.getAttrs(request, true);
	    
	    checkRight(zsc, context, null, Admin.R_createCos);
	    checkSetAttrsOnCreate(zsc, TargetType.cos, name, attrs);
	        
	    Cos cos = prov.createCos(name, attrs);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateCos","name", name}, attrs));         

	    Element response = zsc.createElement(AdminConstants.CREATE_COS_RESPONSE);
	    GetCos.encodeCos(response, cos);

	    return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_createCos);
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyCos.getName(), "cos"));
    }
}