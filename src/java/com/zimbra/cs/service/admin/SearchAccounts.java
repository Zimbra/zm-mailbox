/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import com.google.common.base.Splitter;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchAccountsOptions;
import com.zimbra.cs.account.Provisioning.SearchDirectoryObjectType;
import com.zimbra.cs.account.Provisioning.SearchObjectsOptions;
import com.zimbra.cs.account.Provisioning.SearchAccountsOptions.IncludeType;
import com.zimbra.cs.account.Provisioning.SearchObjectsOptions.SortOpt;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.List;
import java.util.Map;

/**
 * @author schemers
 */
public class SearchAccounts extends AdminDocumentHandler {

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String query = request.getAttribute(AdminConstants.E_QUERY);

        int limit = (int) request.getAttributeLong(AdminConstants.A_LIMIT, Integer.MAX_VALUE);
        if (limit == 0)
            limit = Integer.MAX_VALUE;
        int offset = (int) request.getAttributeLong(AdminConstants.A_OFFSET, 0);
        String domain = request.getAttribute(AdminConstants.A_DOMAIN, null);
        boolean applyCos = request.getAttributeBool(AdminConstants.A_APPLY_COS, true);
        String attrsStr = request.getAttribute(AdminConstants.A_ATTRS, null);
        String sortBy = request.getAttribute(AdminConstants.A_SORT_BY, null);
        String types = request.getAttribute(AdminConstants.A_TYPES, "accounts");
        boolean sortAscending = request.getAttributeBool(AdminConstants.A_SORT_ASCENDING, true);

        String[] attrs = attrsStr == null ? null : attrsStr.split(",");

        // if we are a domain admin only, restrict to domain
        //
        // Note: isDomainAdminOnly *always* returns false for pure ACL based AccessManager 
        if (isDomainAdminOnly(zsc)) {
            if (domain == null) {
                domain = getAuthTokenAccountDomain(zsc).getName();
            } else {
                checkDomainRight(zsc, domain, AdminRight.PR_ALWAYS_ALLOW);
            }
        }

        Domain d = null;
        if (domain != null) {
            d = prov.get(Key.DomainBy.name, domain);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
        }

        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        AdminAccessControl.SearchDirectoryRightChecker rightChecker = 
            new AdminAccessControl.SearchDirectoryRightChecker(aac, prov, null);
        
        List accounts;
        AdminSession session = (AdminSession) getSession(zsc, Session.Type.ADMIN);
        if (session != null) {
            int flags = Provisioning.searchDirectoryStringToMask(types);
            accounts = session.searchAccounts(d, query, attrs, sortBy, sortAscending, flags, offset, 0, rightChecker);
        } else {
            SearchObjectsOptions searchOpts = new SearchObjectsOptions(d, attrs);
            searchOpts.setTypes(types);
            searchOpts.setSortOpt(sortAscending ? SortOpt.SORT_ASCENDING : SortOpt.SORT_DESCENDING);
            searchOpts.setSortAttr(sortBy);
            
            searchOpts.setFilterString(FilterId.ADMIN_SEARCH, query);
            accounts = prov.searchDirectory(searchOpts);
                
            accounts = rightChecker.getAllowed(accounts);
        }

        Element response = zsc.createElement(AdminConstants.SEARCH_ACCOUNTS_RESPONSE);
        int i, limitMax = offset+limit;
        for (i=offset; i < limitMax && i < accounts.size(); i++) {
            NamedEntry entry = (NamedEntry) accounts.get(i);
            SearchDirectory.encodeEntry(prov, response, entry, applyCos, null, aac);
        }          

        response.addAttribute(AdminConstants.A_MORE, i < accounts.size());
        response.addAttribute(AdminConstants.A_SEARCH_TOTAL, accounts.size());
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getAccount);
        relatedRights.add(Admin.R_getCalendarResource);
        relatedRights.add(Admin.R_getDistributionList);
        relatedRights.add(Admin.R_getDomain);
        relatedRights.add(Admin.R_listAccount);
        relatedRights.add(Admin.R_listCalendarResource);
        relatedRights.add(Admin.R_listDistributionList);
        relatedRights.add(Admin.R_listDomain);
        
        notes.add(AdminRightCheckPoint.Notes.LIST_ENTRY);
    }
}
