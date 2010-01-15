/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllZimlets extends AdminDocumentHandler {

    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

		String exclude = request.getAttribute(AdminConstants.A_EXCLUDE, AdminConstants.A_NONE);
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

		List<Zimlet> zimlets = prov.listAllZimlets();
		
		AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);

	    Element response = zsc.createElement(AdminConstants.GET_ALL_ZIMLETS_RESPONSE);
    	if(AdminConstants.A_EXTENSION.equalsIgnoreCase(exclude)) {
		    for (Zimlet zimlet : zimlets) {
		        if (!zimlet.isExtension()) {
    		        if (aac.hasRightsToList(zimlet, Admin.R_listZimlet, null))
    		            GetZimlet.encodeZimlet(response, zimlet, null, aac.getAttrRightChecker(zimlet));
    		    }
		    }
    	} else if(AdminConstants.A_MAIL.equalsIgnoreCase(exclude)) {
		    for (Zimlet zimlet : zimlets) {
		        if (zimlet.isExtension()) {
		    	    if (aac.hasRightsToList(zimlet, Admin.R_listZimlet, null))
		    		    GetZimlet.encodeZimlet(response, zimlet, null, aac.getAttrRightChecker(zimlet));
		    	}
		    }
    	} else {
		    for (Zimlet zimlet : zimlets) {
		        if (aac.hasRightsToList(zimlet, Admin.R_listZimlet, null))
	    		    GetZimlet.encodeZimlet(response, zimlet, null, aac.getAttrRightChecker(zimlet));
    	    }
    		
    	}
	    return response;
	}
    	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_listZimlet);
        relatedRights.add(Admin.R_getZimlet);
        
        notes.add(AdminRightCheckPoint.Notes.LIST_ENTRY);
    }
}
