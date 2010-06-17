/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
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
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllFreeBusyProviders extends AdminDocumentHandler {
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        // allow only system admin for now
        checkRight(zsc, context, null, Admin.R_getAllFreeBusyProviders);
        
        Element response = zsc.createElement(AdminConstants.GET_ALL_FREE_BUSY_PROVIDERS_RESPONSE);
        
        for (FreeBusyProvider prov : FreeBusyProvider.getProviders()) {
            Element provElem = response.addElement(AdminConstants.E_PROVIDER);
            provElem.addAttribute(AdminConstants.A_NAME, prov.getName());
            provElem.addAttribute(AdminConstants.A_PROPAGATE, prov.registerForMailboxChanges());
            provElem.addAttribute(AdminConstants.A_START, prov.cachedFreeBusyStartTime());
            provElem.addAttribute(AdminConstants.A_END, prov.cachedFreeBusyEndTime());
            provElem.addAttribute(AdminConstants.A_QUEUE, prov.getQueueFilename());
            provElem.addAttribute(AdminConstants.A_PREFIX, prov.foreignPrincipalPrefix());
        }
	    return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getAllFreeBusyProviders);
    }
}
