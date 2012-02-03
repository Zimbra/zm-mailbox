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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchResultCallback;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.type.GalSearchType;

/**
 * @since May 26, 2004
 * @author schemers
 */
public class SearchGal extends AdminGalDocumentHandler {
    
    /**
     * must be careful and only return accounts a domain admin can see
     */
    @Override
    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        String domainName = request.getAttribute(AdminConstants.A_DOMAIN);
        Provisioning prov = Provisioning.getInstance();
        Domain domain = prov.get(Key.DomainBy.name, domainName);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(domainName);

        checkDomainRight(zsc, domain, Admin.R_accessGAL);
        
        String name = request.getAttribute(AdminConstants.E_NAME, "");
        int limit = (int) request.getAttributeLong(AdminConstants.A_LIMIT, 0);
        String typeStr = request.getAttribute(AdminConstants.A_TYPE, GalSearchType.account.name());
        GalSearchType type = GalSearchType.fromString(typeStr);
        
        String galAcctId = request.getAttribute(AccountConstants.A_GAL_ACCOUNT_ID, null);
        
        String token = request.getAttribute(AdminConstants.A_TOKEN, null);

        GalSearchParams params = new GalSearchParams(domain, zsc);
        if (token != null)
            params.setToken(token);
        params.setType(type);
        params.setRequest(request);
        params.setQuery(name);
        params.setLimit(limit);
        params.setResponseName(AdminConstants.SEARCH_GAL_RESPONSE);
        if (galAcctId != null)
            params.setGalSyncAccount(Provisioning.getInstance().getAccountById(galAcctId));
        
        params.setResultCallback(new SearchGal.AdminGalCallback(params));
        GalSearchControl gal = new GalSearchControl(params);
        if (token != null)
            gal.sync();
        else
            gal.search();
        return params.getResultCallback().getResponse();
    }

    static class AdminGalCallback extends GalSearchResultCallback {
        private Element proxiedResponse;
        
        AdminGalCallback(GalSearchParams params) {
            super(params);
        }
        
        @Override
        public boolean passThruProxiedGalAcctResponse() {
            return true;
        }
        
        @Override
        public void handleProxiedResponse(Element resp) {
            proxiedResponse = resp;
            proxiedResponse.detach();
        }
        
        @Override
        public Element getResponse() {
            if (proxiedResponse != null)
                return proxiedResponse;
            else
                return super.getResponse();
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_accessGAL);
    }

}
