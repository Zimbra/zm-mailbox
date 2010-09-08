/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class SearchGal extends AdminDocumentHandler {

    /**
     * must be careful and only return accounts a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        String n = request.getAttribute(AdminConstants.E_NAME);

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(getZimbraSoapContext(context));
        
        while (n.endsWith("*"))
            n = n.substring(0, n.length() - 1);

        String domain = request.getAttribute(AdminConstants.A_DOMAIN);
        String typeStr = request.getAttribute(AdminConstants.A_TYPE, "account");
        String token = request.getAttribute(AdminConstants.A_TOKEN, null);
        String galAcctId = request.getAttribute(AccountConstants.A_ID, null);

        Provisioning.GalSearchType type = Provisioning.GalSearchType.fromString(typeStr);

        Provisioning prov = Provisioning.getInstance();
        Domain d = prov.get(DomainBy.name, domain);
        if (d == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(domain);
        
        checkDomainRight(zsc, d, Admin.R_accessGAL); 

        GalSearchParams params = new GalSearchParams(d, zsc);
        if (token != null)
            params.setToken(token);
        params.setType(type);
        params.setRequest(request);
        params.setQuery(n);
        params.setResponseName(AdminConstants.SEARCH_GAL_RESPONSE);
        if (galAcctId != null)
        	params.setGalSyncAccount(Provisioning.getInstance().getAccountById(galAcctId));
        GalSearchControl gal = new GalSearchControl(params);
        if (token != null)
        	gal.sync();
        else
        	gal.search();
        return params.getResultCallback().getResponse();
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_accessGAL);
    }

}
