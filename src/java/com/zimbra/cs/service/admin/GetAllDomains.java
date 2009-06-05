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
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetAllDomains extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        boolean applyConfig = request.getAttributeBool(AdminConstants.A_APPLY_CONFIG, true);
        List domains = prov.getAllDomains();
        
        Element response = zsc.createElement(AdminConstants.GET_ALL_DOMAINS_RESPONSE);
        for (Iterator it = domains.iterator(); it.hasNext(); ) {
            Domain domain = (Domain) it.next();
            
            if (!hasRightsToList(zsc, domain, Admin.R_listDomain, Admin.R_getDomain))
                continue;
            
            GetDomain.encodeDomain(response, domain, applyConfig);
        }

	    return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_listDomain);
        relatedRights.add(Admin.R_getDomain);
        
        notes.add(AdminRightCheckPoint.Notes.LIST_ENTRY);
    }
}
