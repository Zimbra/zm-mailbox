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
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.Session;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author schemers
 */
public class SearchDirectory extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
    private static final String SEARCH_DIRECTORY_ACCOUNT_DATA = "SearchDirectoryAccount";

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
                    GetDomain.encodeDomain(response, d, applyConfig, reqAttrs, null);
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

        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        AdminAccessControl.SearchDirectoryRightChecker rightChecker = 
            new AdminAccessControl.SearchDirectoryRightChecker(aac, prov, reqAttrs);
        
        List accounts;
        AdminSession session = (AdminSession) getSession(zsc, Session.Type.ADMIN);
        
        // bug 36017.  
        // See comment for Provisioning.SO_NO_ACCOUNT_DEFAULTS
        // 
        // We set defaults when accounts are paged back to the SOAP client.
        //
        // Account object returned from Provisioning.searchDirectory are not cached anywhere,
        // they are just referenced here.
        //
        flags |= Provisioning.SO_NO_ACCOUNT_DEFAULTS;
        
        if (session != null) {
            accounts = session.searchAccounts(d, query, attrs, sortBy, sortAscending, flags, offset, maxResults, rightChecker);
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
            accounts = prov.searchDirectory(options);
            accounts = rightChecker.getAllowed(accounts);
        }

        LdapProvisioning ldapProv = null;
        if (prov instanceof LdapProvisioning)
            ldapProv = (LdapProvisioning)prov;

        int i, limitMax = offset+limit;
        for (i=offset; i < limitMax && i < accounts.size(); i++) {
            NamedEntry entry = (NamedEntry) accounts.get(i);
            
            boolean applyDefault = true;
            
            if (entry instanceof Account) {
                applyDefault = applyCos;
                setAccountDefaults(ldapProv, (Account)entry);
            } else if (entry instanceof Domain) {
                applyDefault = applyConfig;
            }
            
            encodeEntry(prov, response, entry, applyDefault, reqAttrs, aac);
        }          

        response.addAttribute(AdminConstants.A_MORE, i < accounts.size());
        response.addAttribute(AdminConstants.A_SEARCH_TOTAL, accounts.size());
        return response;
    }
    
    private void setAccountDefaults(LdapProvisioning ldapProv, Account entry) throws ServiceException {
        if (ldapProv == null)
            return;
        
        Boolean isDefaultSet = (Boolean)entry.getCachedData(SEARCH_DIRECTORY_ACCOUNT_DATA);
        if (isDefaultSet == null || isDefaultSet == Boolean.FALSE) {
            ldapProv.setAccountDefaults((Account)entry, 0);
            entry.setCachedData(SEARCH_DIRECTORY_ACCOUNT_DATA, Boolean.TRUE);
        }
    }
    
    static void encodeEntry(Provisioning prov, Element parent, NamedEntry entry, boolean applyDefault, Set<String> reqAttrs, AdminAccessControl aac) 
    throws ServiceException {
        if (entry instanceof CalendarResource) {
            ToXML.encodeCalendarResource(parent, (CalendarResource)entry, applyDefault, reqAttrs, aac.getAttrRightChecker((CalendarResource)entry));
        } else if (entry instanceof Account) {
            ToXML.encodeAccount(parent, (Account)entry, applyDefault, reqAttrs, aac.getAttrRightChecker((Account)entry));
        } else if (entry instanceof DistributionList) {
            GetDistributionList.encodeDistributionList(parent, (DistributionList)entry, false, reqAttrs, aac.getAttrRightChecker((DistributionList)entry));
        } else if (entry instanceof Alias) {
            encodeAlias(parent, prov, (Alias)entry, reqAttrs);
        } else if (entry instanceof Domain) {
            GetDomain.encodeDomain(parent, (Domain)entry, applyDefault, reqAttrs, aac.getAttrRightChecker((Domain)entry));
        } else if (entry instanceof Cos) {
            GetCos.encodeCos(parent, (Cos)entry, reqAttrs, aac.getAttrRightChecker((Cos)entry));
        }
    }

    private static void encodeAlias(Element e, Provisioning prov, Alias a, Set<String> reqAttrs) throws ServiceException {
        Element ealias = e.addElement(AdminConstants.E_ALIAS);
        ealias.addAttribute(AdminConstants.A_NAME, a.getUnicodeName());
        ealias.addAttribute(AdminConstants.A_ID, a.getId());
        ealias.addAttribute(AdminConstants.A_TARGETNAME, a.getTargetUnicodeName(prov));
        
        TargetType tt = a.getTargetType(prov);
        if (tt != null)
            ealias.addAttribute(AdminConstants.A_TYPE, tt.getCode());
        
        Map attrs = a.getUnicodeAttrs();
        
        ToXML.encodeAttrs(ealias, attrs, reqAttrs, null); // don't have/need an AttrRightChecker for alias
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
