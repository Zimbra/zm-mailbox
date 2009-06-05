/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009 Zimbra, Inc.
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
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
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
	    
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
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
        
        if (isDomainAdminOnly(zsc)) {
            if (domain != null) { 
                checkDomainRight(zsc, domain, AdminRight.PR_ALWAYS_ALLOW); 
            }
            domain = getAuthTokenAccountDomain(zsc);
        }

        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        
        if (domain != null) {
            response = zsc.createElement(AdminConstants.GET_ALL_DISTRIBUTION_LISTS_RESPONSE);
            doDomain(zsc, response, domain, aac);
        } else {
            response = zsc.createElement(AdminConstants.GET_ALL_DISTRIBUTION_LISTS_RESPONSE);
            List domains = prov.getAllDomains();
            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
                Domain dm = (Domain) dit.next();
                doDomain(zsc, response, dm, aac);                
            }
        }
        return response;        
    }
    
    private void doDomain(ZimbraSoapContext zsc, Element e, Domain d, AdminAccessControl aac) throws ServiceException {
        List dls = Provisioning.getInstance().getAllDistributionLists(d);
        for (Iterator it = dls.iterator(); it.hasNext(); ) {
            DistributionList dl = (DistributionList) it.next();
            if (aac.hasRightsToList(dl, Admin.R_listDistributionList, null))
                GetDistributionList.encodeDistributionList(e, dl, true, null, aac.getAttrRightChecker(dl));
        }        
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_listDistributionList);
        relatedRights.add(Admin.R_getDistributionList);
        
        notes.add(AdminRightCheckPoint.Notes.LIST_ENTRY);
    }
}
