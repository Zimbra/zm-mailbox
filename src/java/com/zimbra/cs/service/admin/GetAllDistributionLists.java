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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
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
        
        Element d = request.getOptionalElement(AdminService.E_DOMAIN);
        Domain domain = null;
        
        if (d != null) {
            String key = d.getAttribute(AdminService.A_BY);
            String value = d.getText();
        
            if (key.equals(BY_NAME)) {
                domain = prov.getDomainByName(value);
            } else if (key.equals(BY_ID)) {
                domain = prov.getDomainById(value);
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
            response = lc.createElement(AdminService.GET_ALL_DISTRIBUTION_LISTS_RESPONSE);
            doDomain(response, domain);
        } else {
            response = lc.createElement(AdminService.GET_ALL_DISTRIBUTION_LISTS_RESPONSE);
            List domains = prov.getAllDomains();
            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
                Domain dm = (Domain) dit.next();
                doDomain(response, dm);                
            }
        }
        return response;        
    }
    
    public static void doDomain(Element e, Domain d) throws ServiceException {
        List dls = d.getAllDistributionLists();
        for (Iterator it = dls.iterator(); it.hasNext(); ) {
            GetDistributionList.doDistributionList(e, (DistributionList) it.next());
        }        
    }
}
