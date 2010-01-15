/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllDistributionLists extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
    /**
     * must be careful and only allow on dls domain admin has access to
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element response = null;
        
        Element d = request.getOptionalElement(AdminConstants.E_DOMAIN);
        Domain domain = null;
        
        if (d != null) {
            String key = d.getAttribute(AdminConstants.A_BY);
            String value = d.getText();
        
            if (key.equals(BY_NAME)) {
                domain = prov.get(DomainBy.name, value);
            } else if (key.equals(BY_ID)) {
                domain = prov.get(DomainBy.id, value);
            } else {
                throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
            }
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(value);            
        }
        
        if (isDomainAdminOnly(lc)) {
            if (domain != null && !canAccessDomain(lc, domain))
                throw ServiceException.PERM_DENIED("can not access domain");
            domain = getAuthTokenAccountDomain(lc);
        }

        if (domain != null) {
            response = lc.createElement(AdminConstants.GET_ALL_DISTRIBUTION_LISTS_RESPONSE);
            doDomain(response, domain);
        } else {
            response = lc.createElement(AdminConstants.GET_ALL_DISTRIBUTION_LISTS_RESPONSE);
            List domains = prov.getAllDomains();
            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
                Domain dm = (Domain) dit.next();
                doDomain(response, dm);                
            }
        }
        return response;        
    }
    
    public static void doDomain(Element e, Domain d) throws ServiceException {
        List dls = Provisioning.getInstance().getAllDistributionLists(d);
        for (Iterator it = dls.iterator(); it.hasNext(); ) {
            GetDistributionList.doDistributionList(e, (DistributionList) it.next());
        }        
    }
}
