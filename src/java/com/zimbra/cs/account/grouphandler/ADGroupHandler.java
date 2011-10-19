/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.account.grouphandler;

import java.util.List;
import java.util.TreeSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.ExternalGroup;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.account.ldap.LdapHelper;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.IAttributes.CheckBinary;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;

public class ADGroupHandler extends GroupHandler {

    private static final String MAIL_ATTR = "mail";
    private static final String MEMBER_OF_ATTR = LdapConstants.ATTR_MEMBER_OF;
    
    @Override
    public boolean isGroup(IAttributes ldapAttrs) {
        try {
            List<String> objectclass = ldapAttrs.getMultiAttrStringAsList(
                    Provisioning.A_objectClass, IAttributes.CheckBinary.NOCHECK);
            return objectclass.contains("group");
        } catch (ServiceException e) {
            ZimbraLog.gal.warn("unable to get attribute " + Provisioning.A_objectClass, e);
        }
        return false;
    }

    @Override
    public String[] getMembers(ILdapContext ldapContext, String searchBase, 
            String entryDN, IAttributes ldapAttrs) {
        if (ZimbraLog.gal.isDebugEnabled()) {
            try {
                ZimbraLog.gal.debug("Fetching members for group " + ldapAttrs.getAttrString(MAIL_ATTR));
            } catch (ServiceException e) {
                ZimbraLog.gal.debug("unable to get email address of group " + entryDN, e);
            }
        }
        
        SearchADGroupMembers searcher = new SearchADGroupMembers();
        TreeSet<String> result = searcher.searchLdap(ldapContext, searchBase, entryDN);
        return result.toArray(new String[result.size()]);
    }
    
    
    private static class SearchADGroupMembers extends SearchLdapVisitor {

        TreeSet<String> result = new TreeSet<String>();
        
        SearchADGroupMembers() {
            super(false);
        }
        
        @Override
        public void visit(String dn, IAttributes ldapAttrs) {
            String email;
            try {
                email = ldapAttrs.getAttrString(MAIL_ATTR);
                if (email != null) {
                    result.add(email);
                }
            } catch (ServiceException e) {
                // swallow exceptions and continue
                ZimbraLog.gal.warn("unable to get attribute " + MAIL_ATTR + " from search result", e);
            }
        }
        
        private TreeSet<String> searchLdap(ILdapContext zlc, String searchBase, String dnOfGroup) {
            
            ZLdapFilter filter = ZLdapFilterFactory.getInstance().memberOf(dnOfGroup);
            String[] returnAttrs = new String[]{MAIL_ATTR};
            
            try {
                LdapHelper ldapHelper = LdapProv.getInst().getHelper();
                SearchLdapOptions searchOptions = new SearchLdapOptions(searchBase, filter, 
                        returnAttrs, SearchLdapOptions.SIZE_UNLIMITED, null, 
                        ZSearchScope.SEARCH_SCOPE_SUBTREE, this);
                ldapHelper.searchLdap(zlc, searchOptions);
            } catch (ServiceException e) {
                // log and continue
                ZimbraLog.gal.warn("unable to search group members", e);
            }
                        
            return result;
        }
    }

    @Override
    public ZLdapContext getExternalDelegatedAdminGroupsLdapContext(Domain domain, boolean asAdmin) 
    throws ServiceException {
        if (!domainAdminAuthMechIsAD(domain, asAdmin)) {
            throw ServiceException.INVALID_REQUEST("domain auth mech must be AD", null);
        }
        
        return super.getExternalDelegatedAdminGroupsLdapContext(domain, asAdmin);
    }
    
    private static boolean domainAdminAuthMechIsAD(Domain domain, boolean asAdmin) {
        return asAdmin ? AuthMech.ad.name().equals(domain.getAuthMechAdmin()) :
                         AuthMech.ad.name().equals(domain.getAuthMech());
    }
    
    /*
     * Check:
     *   - zimbraAuthMechAdmin on the domain must be AD
     *   - domain of the account must be the same as the domain in the grant
     *   
     * TODO: pass in auth token and validate that the auth was indeed via AD 
     */
    private boolean legitimateDelegatedAdminAsGroupMember(ExternalGroup group, 
            Account acct, boolean asAdmin) throws ServiceException {
        String zimbraDomainId = group.getZimbraDomainId();
        Domain domain = Provisioning.getInstance().getDomain(acct);
        
        if (domain == null) {
            return false;
        }
        
        if (!domainAdminAuthMechIsAD(domain, asAdmin)) {
            return false;
        }
        
        if (!domain.getId().equals(zimbraDomainId)) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean inDelegatedAdminGroup(ExternalGroup group, Account acct, boolean asAdmin) 
    throws ServiceException {
        
        if (!legitimateDelegatedAdminAsGroupMember(group, acct, asAdmin)) {
            return false;
        }
        
        // check cache
        @SuppressWarnings("unchecked")
        List<String> groupDNs = (List<String>)
            acct.getCachedData(EntryCacheDataKey.GROUPEDENTRY_EXTERNAL_GROUP_DNS);
        
        if (groupDNs != null) {
            return groupDNs.contains(group.getDN());
        }
        
        // get groups DNs this account belongs to in the external group
        groupDNs = getDelegatedAdminGroups(acct, asAdmin);
                
        acct.setCachedData(EntryCacheDataKey.GROUPEDENTRY_EXTERNAL_GROUP_DNS, groupDNs);
        
        return groupDNs.contains(group.getDN());
    }
    
    private List<String> getDelegatedAdminGroups(Account acct, boolean asAdmin) throws ServiceException {
        LdapProv prov = LdapProv.getInst();
        
        Domain domain = prov.getDomain(acct);
        if (domain == null) {
            throw ServiceException.FAILURE("unable to get domain for account " + 
                    acct.getName(), null);
        }
        
        // try explicit external DN on account first
        String extDN = acct.getAuthLdapExternalDn();
        if (extDN == null) {
            // then try bind DN template on domain
            // note: for AD auth, zimbraAuthLdapSearchFilter is not used, so we 
            //       skip that. See LdapProvisioning.externalLdapAuth
            String dnTemplate = domain.getAuthLdapBindDn();
            if (dnTemplate != null) {
                extDN = LdapUtilCommon.computeDn(acct.getName(), dnTemplate);
            }
        }
        
        if (extDN == null) {
            throw ServiceException.FAILURE("unable to get external DN for account " + 
                    acct.getName(), null);
        }
        
        ZLdapContext zlc = null;
        try {
            zlc = getExternalDelegatedAdminGroupsLdapContext(domain, asAdmin);
            
            ZAttributes attrs = prov.getHelper().getAttributes(zlc, extDN, new String[]{MEMBER_OF_ATTR});
            
            return attrs.getMultiAttrStringAsList(MEMBER_OF_ATTR, CheckBinary.NOCHECK);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
}
