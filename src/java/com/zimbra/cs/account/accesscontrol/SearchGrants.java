/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.soap.type.TargetBy;

public final class SearchGrants {
    private final Provisioning prov;
    private final Set<TargetType> targetTypes;
    private final Set<String> granteeIds;
    
    private final Account acct;
    private final Set<Right> rights;
    
    private final Set<String> fetchAttrs = Sets.newHashSet(
            Provisioning.A_cn, 
            Provisioning.A_zimbraId,
            Provisioning.A_objectClass, 
            Provisioning.A_zimbraACE);

    SearchGrants(Provisioning prov, Set<TargetType> targetTypes, Set<String> granteeIds) {
        this.prov = prov;
        this.targetTypes = targetTypes;
        this.granteeIds = granteeIds;
        this.acct = null;
        this.rights = null;
    }
    
    /*
     * search for rights applied to the acct
     */
    SearchGrants(Provisioning prov, Set<TargetType> targetTypes, Account acct, 
            Set<Right> rights) {
        this.prov = prov;
        this.targetTypes = targetTypes;
        this.granteeIds = null;
        this.acct = acct;
        this.rights = rights;
    }

    void addFetchAttribute(String attr) {
        fetchAttrs.add(attr);
    }

    void addFetchAttribute(Set<String> attrs) {
        fetchAttrs.addAll(attrs);
    }

    static final class GrantsOnTarget {
        private Entry targetEntry;
        private ZimbraACL acl;

        private GrantsOnTarget(Entry targetEntry, ZimbraACL acl) {
            this.targetEntry = targetEntry;
            this.acl = acl;
        }

        Entry getTargetEntry() {
            return targetEntry;
        }

        ZimbraACL getAcl() {
            return acl;
        }
    }

    static final class SearchGrantsResults {
        private final Provisioning prov;

        // map of raw(in ldap data form, quick way for staging grants found in search visitor, because
        // we don't want to do much processing in the visitor while taking a ldap connection) search results
        //    key: target id (or name if zimlet)
        //    value: grants on this target
        private final Map<String, GrantsOnTargetRaw> rawResults = new HashMap<String, GrantsOnTargetRaw>();

        // results in the form usable by callers
        private Set<GrantsOnTarget> results;

        SearchGrantsResults(Provisioning prov) {
            this.prov = prov;
        }

        private void addResult(GrantsOnTargetRaw result) {
            rawResults.put(result.getTargetId(), result);
        }

        /**
         * Returns a map of target entry and ZimbraACL object on the target.
         */
        Set<GrantsOnTarget> getResults() throws ServiceException {
            if (results == null) {
                results = new HashSet<GrantsOnTarget>();
                for (GrantsOnTargetRaw grants : rawResults.values()) {
                    results.add(getGrants(prov, grants));
                }
            }
            return results;
        }

        /**
         * Converts a {@link SearchGrantsResults} to {@code <Entry, ZimbraACL>} pair.
         */
        private GrantsOnTarget getGrants(Provisioning prov, GrantsOnTargetRaw sgr) throws ServiceException {
            TargetType tt;
            if (sgr.objectClass.contains(AttributeClass.OC_zimbraCalendarResource)) {
                tt = TargetType.calresource;
            } else if (sgr.objectClass.contains(AttributeClass.OC_zimbraAccount)) {
                tt = TargetType.account;
            } else if (sgr.objectClass.contains(AttributeClass.OC_zimbraCOS)) {
                tt = TargetType.cos;
            } else if (sgr.objectClass.contains(AttributeClass.OC_zimbraDistributionList)) {
                tt = TargetType.dl;
            } else if (sgr.objectClass.contains(AttributeClass.OC_zimbraGroup)) {
                tt = TargetType.group;    
            } else if (sgr.objectClass.contains(AttributeClass.OC_zimbraDomain)) {
                tt = TargetType.domain;
            } else if (sgr.objectClass.contains(AttributeClass.OC_zimbraServer)) {
                tt = TargetType.server;
            } else if (sgr.objectClass.contains(AttributeClass.OC_zimbraXMPPComponent)) {
                tt = TargetType.xmppcomponent;
            } else if (sgr.objectClass.contains(AttributeClass.OC_zimbraZimletEntry)) {
                tt = TargetType.zimlet;
            } else if (sgr.objectClass.contains(AttributeClass.OC_zimbraGlobalConfig)) {
                tt = TargetType.config;
            } else if (sgr.objectClass.contains(AttributeClass.OC_zimbraAclTarget)) {
                tt = TargetType.global;
            } else {
                throw ServiceException.FAILURE("cannot determine target type from SearchGrantResult. " + sgr, null);
            }
            Entry entry = null;
            try {
                if (tt == TargetType.zimlet) {
                    entry = TargetType.lookupTarget(prov, tt, TargetBy.name, sgr.cn);
                } else {
                    entry = TargetType.lookupTarget(prov, tt, TargetBy.id, sgr.zimbraId);
                }
                if (entry == null) {
                    ZimbraLog.acl.warn("canot find target by id %s", sgr.zimbraId);
                    throw ServiceException.FAILURE("canot find target by id " + sgr.zimbraId + ". " + sgr, null);
                }
                ZimbraACL acl = new ZimbraACL(sgr.zimbraACE, tt, entry.getLabel());
                return new GrantsOnTarget(entry, acl);
            } catch (ServiceException e) {
                throw ServiceException.FAILURE("canot find target by id " + sgr.zimbraId + ". " + sgr, null);
            }
        }
    }

    /**
     * grants found on a target based on the search criteria.
     */
    private static class GrantsOnTargetRaw {
        private final String cn;
        private final String zimbraId;
        private final Set<String> objectClass;
        private final String[] zimbraACE;

        private GrantsOnTargetRaw(Map<String, Object> attrs) {
            cn = (String) attrs.get(Provisioning.A_cn);
            zimbraId = (String) attrs.get(Provisioning.A_zimbraId);
            objectClass = ImmutableSet.copyOf(getMultiAttrString(attrs, Provisioning.A_objectClass));
            zimbraACE = getMultiAttrString(attrs, Provisioning.A_zimbraACE);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("cn", cn)
                .add("zimbraId", zimbraId)
                .add("objectClass", objectClass)
                .add("zimbraACE", ImmutableList.copyOf(zimbraACE))
                .toString();
        }

        private String[] getMultiAttrString(Map<String, Object> attrs, String attrName) {
            Object obj = attrs.get(attrName);
            if (obj instanceof String) {
                return new String[] {(String) obj};
            } else {
                return (String[]) obj;
            }
        }

        private String getTargetId() {
            // urg! zimlet does not have an id, use cn.
            // need to return something for the map key for SearchGrantVisitor.visit
            // id is only used for grants granted on group-ed entries (account, cr, dl)
            // in computeRightsOnGroupShape
            return zimbraId != null ? zimbraId : cn;
        }
    }

    private static class SearchGrantVisitor extends SearchLdapVisitor {
        private final SearchGrantsResults results;

        SearchGrantVisitor(SearchGrantsResults results) {
            this.results = results;
        }

        @Override
        public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
            results.addResult(new GrantsOnTargetRaw(attrs));
        }
    }

    /**
     * search grants granted to any of the grantees, granted on any of the target types.
     */
    SearchGrantsResults doSearch() throws ServiceException {
       Map<String, Set<String>> basesAndOcs = TargetType.getSearchBasesAndOCs(prov, targetTypes);
       SearchGrantsResults results = new SearchGrantsResults(prov);
       SearchGrantVisitor visitor = new SearchGrantVisitor(results);
       for (Map.Entry<String, Set<String>> entry : basesAndOcs.entrySet()) {
           search(entry.getKey(), entry.getValue(), visitor);
       }
       return results;
    }
    
    private Set<String> getGranteeIds() throws ServiceException {
        if (granteeIds != null) {
            return granteeIds;
        } else {
            Set<String> ids = Sets.newHashSet(new RightBearer.Grantee(acct, false).getIdAndGroupIds());
            ids.add(GuestAccount.GUID_AUTHUSER);
            ids.add(GuestAccount.GUID_PUBLIC);
            String domainId = acct.getDomainId();
            if (domainId != null) {
                ids.add(domainId);
            }
            
            return ids;
        }
    }

    private void search(String base, Set<String> ocs, SearchGrantVisitor visitor) 
    throws ServiceException {
        StringBuilder query = new StringBuilder("(&(|");
        for (String oc : ocs) {
            query.append('(').append(Provisioning.A_objectClass).append('=').append(oc).append(")");
        }
        query.append(")(|");
        
        if (rights == null) {
            for (String granteeId : getGranteeIds()) {
                query.append('(').append(Provisioning.A_zimbraACE).append('=').append(granteeId).append("*)");
            }
        } else {
            for (String granteeId : getGranteeIds()) {
                for (Right right : rights) {
                    query.append('(').append(Provisioning.A_zimbraACE).append('=').append(granteeId).append("*").append(right.getName()).append(")");
                }
            }
        }
        query.append("))");
        LdapProv.getInst().searchLdapOnMaster(base, query.toString(),
                fetchAttrs.toArray(new String[fetchAttrs.size()]), visitor);
    }
}
