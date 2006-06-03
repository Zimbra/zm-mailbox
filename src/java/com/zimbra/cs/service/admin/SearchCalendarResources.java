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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author jhahm
 */
public class SearchCalendarResources extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";

    /**
     * must be careful and only allow access if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        int limit = (int) request.getAttributeLong(AdminService.A_LIMIT,
                                                   Integer.MAX_VALUE);
        if (limit == 0) limit = Integer.MAX_VALUE;
        int offset = (int) request.getAttributeLong(AdminService.A_OFFSET, 0);        
        String domain = request.getAttribute(AdminService.A_DOMAIN, null);
        boolean applyCos =
            request.getAttributeBool(AdminService.A_APPLY_COS, true);
        String sortBy = request.getAttribute(AdminService.A_SORT_BY, null);        
        boolean sortAscending =
            request.getAttributeBool(AdminService.A_SORT_ASCENDING, true);        
        String attrsStr = request.getAttribute(AdminService.A_ATTRS, null);
        String[] attrs = attrsStr == null ? null : attrsStr.split(",");

        EntrySearchFilter filter =
            com.zimbra.cs.service.account.SearchCalendarResources.
            parseSearchFilter(request);

        // if we are a domain admin only, restrict to domain
        if (isDomainAdminOnly(lc)) {
            if (domain == null) {
                domain = getAuthTokenAccountDomain(lc).getName();
            } else {
                if (!canAccessDomain(lc, domain)) 
                    throw ServiceException.PERM_DENIED("cannot access domain");
            }
        }

        Domain d = null;
        if (domain != null) {
            d = prov.get(DomainBy.NAME, domain);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
        }

        List resources;
        AdminSession session =
            (AdminSession) lc.getSession(SessionCache.SESSION_ADMIN);
        if (session != null) {
            resources = session.searchCalendarResources(
                    d, filter, attrs, sortBy, sortAscending, offset);
        } else {
            if (d != null) {
                resources = d.searchCalendarResources(
                        filter, attrs, sortBy, sortAscending);
            } else {
                resources = prov.searchCalendarResources(
                        filter, attrs, sortBy, sortAscending);
            }
        }

        Element response = lc.createElement(
                AdminService.SEARCH_CALENDAR_RESOURCES_RESPONSE);
        int i, limitMax = offset+limit;
        for (i=offset; i < limitMax && i < resources.size(); i++) {
            NamedEntry entry = (NamedEntry) resources.get(i);
            ToXML.encodeCalendarResource(response,
                                         (CalendarResource) entry,
                                         applyCos);
        }

        response.addAttribute(AdminService.A_MORE, i < resources.size());
        response.addAttribute(AdminService.A_SEARCH_TOTAL, resources.size());
        return response;
    }
}
