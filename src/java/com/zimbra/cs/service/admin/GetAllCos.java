/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetAllCos extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();
        List<Cos> coses = prov.getAllCos();
        
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        
        Element response = zsc.createElement(AdminConstants.GET_ALL_COS_RESPONSE);
        for (Cos cos : coses) {
            if (aac.hasRightsToList(cos, Admin.R_listCos, null))
                GetCos.encodeCos(response, cos, null, aac.getAttrRightChecker(cos));
        }
	    return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_listCos);
        relatedRights.add(Admin.R_getCos);
        
        notes.add(AdminRightCheckPoint.Notes.LIST_ENTRY);
    }
}
