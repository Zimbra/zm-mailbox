/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.SearchOptions;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.Session;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author schemers
 */
public class SearchDirectory extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";

    public static final int MAX_SEARCH_RESULTS = 5000;
    
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

        int maxResults = (int) request.getAttributeLong(AdminConstants.A_MAX_RESULTS, MAX_SEARCH_RESULTS);
        int limit = (int) request.getAttributeLong(AdminConstants.A_LIMIT, Integer.MAX_VALUE);
        if (limit == 0)
            limit = Integer.MAX_VALUE;
        int offset = (int) request.getAttributeLong(AdminConstants.A_OFFSET, 0);
        String domain = request.getAttribute(AdminConstants.A_DOMAIN, null);
        boolean applyCos = request.getAttributeBool(AdminConstants.A_APPLY_COS, true);
        boolean applyConfig = request.getAttributeBool(AdminConstants.A_APPLY_CONFIG, true);
        String attrsStr = request.getAttribute(AdminConstants.A_ATTRS, null);
        String sortBy = request.getAttribute(AdminConstants.A_SORT_BY, null);
        String types = request.getAttribute(AdminConstants.A_TYPES, "accounts");
        boolean sortAscending = request.getAttributeBool(AdminConstants.A_SORT_ASCENDING, true);

        int flags = 0;
        
        if (types.indexOf("accounts") != -1) flags |= Provisioning.SA_ACCOUNT_FLAG;
        if (types.indexOf("aliases") != -1) flags |= Provisioning.SA_ALIAS_FLAG;
        if (types.indexOf("distributionlists") != -1) flags |= Provisioning.SA_DISTRIBUTION_LIST_FLAG;
        if (types.indexOf("resources") != -1) flags |= Provisioning.SA_CALENDAR_RESOURCE_FLAG;
        if (types.indexOf("domains") != -1) flags |= Provisioning.SA_DOMAIN_FLAG;
        if (types.indexOf("coses") != -1) flags |= Provisioning.SD_COS_FLAG;
        
        // cannot specify a domain with the "coses" flag 
        if (((flags & Provisioning.SD_COS_FLAG) == Provisioning.SD_COS_FLAG) &&
            (domain != null))
            throw ServiceException.INVALID_REQUEST("cannot specify domain with coses flag", null);

        String[] attrs = attrsStr == null ? null : attrsStr.split(",");
        Set<String> reqAttrs = attrs == null ? null : new HashSet(Arrays.asList(attrs));
        Element response = zsc.createElement(AdminConstants.SEARCH_DIRECTORY_RESPONSE);
        
        // if we are a domain admin only, restrict to domain
        //
        // Note: isDomainAdminOnly *always* returns false for pure ACL based AccessManager 
        if (isDomainAdminOnly(zsc)) {
            if ((flags & Provisioning.SA_DOMAIN_FLAG) == Provisioning.SA_DOMAIN_FLAG) {
            	if(query != null && query.length()>0) {
            		throw ServiceException.PERM_DENIED("cannot search for domains");
            	} else {
            		domain = getAuthTokenAccountDomain(zsc).getName();
            		Domain d = null;
                    if (domain != null) {
                        d = prov.get(DomainBy.name, domain);
                        if (d == null)
                            throw AccountServiceException.NO_SUCH_DOMAIN(domain);
                    }
            		GetDomain.doDomain(response, d, applyConfig, reqAttrs);
                    response.addAttribute(AdminConstants.A_MORE, false);
                    response.addAttribute(AdminConstants.A_SEARCH_TOTAL, 1);
                    return response;
            	}
            	
            }
            if ((flags & Provisioning.SD_COS_FLAG) == Provisioning.SD_COS_FLAG)
                throw ServiceException.PERM_DENIED("cannot search for coses");

            if (domain == null) {
                domain = getAuthTokenAccountDomain(zsc).getName();
            } else {
                checkDomainRight(zsc, domain, AdminRight.PR_ALWAYS_ALLOW);
            }
        }

        Domain d = null;
        if (domain != null) {
            d = prov.get(DomainBy.name, domain);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
        }

        List accounts;
        AdminSession session = (AdminSession) getSession(zsc, Session.Type.ADMIN);
        if (session != null) {
            accounts = session.searchAccounts(d, query, attrs, sortBy, sortAscending, flags, offset, maxResults);
        } else {
            SearchOptions options = new SearchOptions();
            options.setDomain(d);
            options.setFlags(flags);
            options.setMaxResults(maxResults);
            options.setQuery(query);
            options.setReturnAttrs(attrs);
            options.setSortAscending(sortAscending);
            options.setSortAttr(sortBy);
            options.setConvertIDNToAscii(true);
            accounts = prov.searchDirectory(options, false);
        }


        int i, limitMax = offset+limit;
        for (i=offset; i < limitMax && i < accounts.size(); i++) {
            NamedEntry entry = (NamedEntry) accounts.get(i);
        	if (entry instanceof CalendarResource) {
        	    if (hasRightsToList(this, zsc, entry, Admin.R_listCalendarResource, Admin.R_getCalendarResource, reqAttrs))
        	        ToXML.encodeCalendarResourceOld(response, (CalendarResource) entry, applyCos, reqAttrs);
        	} else if (entry instanceof Account) {
        	    if (hasRightsToList(this, zsc, entry, Admin.R_listAccount, Admin.R_getAccount, reqAttrs))
                    ToXML.encodeAccountOld(response, (Account)entry, applyCos, reqAttrs);
            } else if (entry instanceof DistributionList) {
                if (hasRightsToList(this, zsc, entry, Admin.R_listDistributionList, Admin.R_getDistributionList, reqAttrs))
                    doDistributionList(response, (DistributionList)entry);
            } else if (entry instanceof Alias) {
                if (hasRightsToListAlias(this, prov, zsc, (Alias)entry))
                    doAlias(response, prov, (Alias)entry);
            } else if (entry instanceof Domain) {
                if (hasRightsToList(this, zsc, entry, Admin.R_listDomain, Admin.R_getDomain, reqAttrs))
                    GetDomain.doDomain(response, (Domain)entry, applyConfig, reqAttrs);
            } else if (entry instanceof Cos) {
                if (hasRightsToList(this, zsc, entry, Admin.R_listCos, Admin.R_getCos, reqAttrs))
                    GetCos.doCos(response, (Cos)entry);
            }
        }          

        response.addAttribute(AdminConstants.A_MORE, i < accounts.size());
        response.addAttribute(AdminConstants.A_SEARCH_TOTAL, accounts.size());
        return response;
    }
    
    static boolean hasRightsToList(AdminDocumentHandler handler, ZimbraSoapContext zsc, NamedEntry target, 
            AdminRight listRight, Object getAllAttrsRight, Set<String> getAttrsRight) throws ServiceException {
        
        if (getAttrsRight == null || getAttrsRight.isEmpty())
            return handler.hasRightsToList(zsc, target, listRight, getAllAttrsRight);
        else
            return handler.hasRightsToList(zsc, target, listRight, getAttrsRight);
    }
    
    private static boolean hasRightsToListDanglingAlias(AdminDocumentHandler handler, ZimbraSoapContext zsc, Alias alias) 
    throws ServiceException {
        /*
         * gross, this is the only case we would ever pass an Alias object for ACL checking.
         * 
         * We want to pass alias instead of null so if PERM_DENIED the skipping WARN can be 
         * nicely logged just like whenever we skip listing any object.
         * 
         * Alias is *not* a valid TargetTytpe for ACL checking.  Luckily(and hackily), the pseudo 
         * right PR_SYSTEM_ADMIN_ONLY would never lead to a path that needs to refer to the 
         * target. 
         */
        return handler.hasRightsToList(zsc, alias, AdminRight.PR_SYSTEM_ADMIN_ONLY, null);
    }
    
    static boolean hasRightsToListAlias(AdminDocumentHandler handler, Provisioning prov,
            ZimbraSoapContext zsc, Alias alias) throws ServiceException {
        boolean hasRight;
        
        // if an admin can list the account/cr/dl, he can do the same on their aliases
        // don't need any getAttrs rights on the account/cr/dl, because the returned alias
        // entry contains only attrs on the alias, not the target entry.
        TargetType tt = alias.getTargetType(prov);
        
        if (tt == null) // can't check right, allows only system admin
            hasRight = hasRightsToListDanglingAlias(handler, zsc, alias);
        else if (tt == TargetType.dl)
            hasRight = handler.hasRightsToList(zsc, alias.getTarget(prov), Admin.R_listDistributionList, null);
        else if (tt == TargetType.calresource)
            hasRight = handler.hasRightsToList(zsc, alias.getTarget(prov), Admin.R_listCalendarResource, null);
        else
            hasRight = handler.hasRightsToList(zsc, alias.getTarget(prov), Admin.R_listAccount, null);
        
        return hasRight;
    }

    static void doDistributionList(Element e, DistributionList list) {
        Element elist = e.addElement(AdminConstants.E_DL);
        elist.addAttribute(AdminConstants.A_NAME, list.getUnicodeName());
        elist.addAttribute(AdminConstants.A_ID, list.getId());
        Map attrs = list.getUnicodeAttrs();
        doAttrs(elist, attrs);
    }

    static void doAlias(Element e, Provisioning prov, Alias a) throws ServiceException {
        Element ealias = e.addElement(AdminConstants.E_ALIAS);
        ealias.addAttribute(AdminConstants.A_NAME, a.getUnicodeName());
        ealias.addAttribute(AdminConstants.A_ID, a.getId());
        ealias.addAttribute(AdminConstants.A_TARGETNAME, a.getTargetUnicodeName(prov));
        
        TargetType tt = a.getTargetType(prov);
        if (tt != null)
            ealias.addAttribute(AdminConstants.A_TYPE, tt.getCode());
        
        Map attrs = a.getUnicodeAttrs();
        doAttrs(ealias, attrs);
    }

    static void doAttrs(Element e, Map attrs) {
        for (Iterator mit = attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++)
                    e.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText(sv[i]);
            } else if (value instanceof String)
                e.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText((String) value);
        }       
    }   
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getAccount);
        relatedRights.add(Admin.R_getCalendarResource);
        relatedRights.add(Admin.R_getDistributionList);
        relatedRights.add(Admin.R_getDomain);
        relatedRights.add(Admin.R_getCos);
        relatedRights.add(Admin.R_listAccount);
        relatedRights.add(Admin.R_listCalendarResource);
        relatedRights.add(Admin.R_listDistributionList);
        relatedRights.add(Admin.R_listDomain);
        relatedRights.add(Admin.R_listCos);
        
        notes.add(AdminRightCheckPoint.Notes.LIST_ENTRY);
    }
}
