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
package com.zimbra.cs.account.ldap.upgrade;

import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.InlineAttrRight;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.account.ldap.ZimbraLdapContext.LdapConfig;

public class AdminRights extends LdapUpgrade {
    
    private static String[] sAdminUICompForAllDomainAdmins = new String[] {
        "accountListView",
        "aliasListView",
        "DLListView",
        "resourceListView",
        "saveSearch"
    };
    
    private static String[] sAdminUICompForAllGlobalAdmins = new String[] {
        "cartBlancheUI"
    };

    AdminRights() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        
        Set<String> domainAdminIds = new HashSet<String>();
        Set<String> globalAdminIds = new HashSet<String>();
        
        getAllDomainOrGlobalAdmins(domainAdminIds, globalAdminIds);
        
        for (String domainAdminId : domainAdminIds) {
            try {
                Account domainAdmin = mProv.get(AccountBy.id, domainAdminId);
                if (domainAdmin == null)
                    continue;
                
                Domain domain = mProv.getDomain(domainAdmin);
                if (domain == null)
                    continue;
                
                System.out.println("Upgrading domain admin: " + domainAdmin.getName());
                grantRights(domain, domainAdmin);
            } catch (ServiceException e) {
                System.out.println("Skipped upgrading global admin " + domainAdminId + " (Encountered error: " + e.getMessage() + ")");
            }
        }
        
        for (String globalAdminId : globalAdminIds) {
            try {
                Account globalAdmin = mProv.get(AccountBy.id, globalAdminId);
                if (globalAdmin == null)
                    continue;
                
                System.out.println("Upgrading global admin: " + globalAdmin.getName());
                setGlobalAdminUIComp(globalAdmin);
            } catch (ServiceException e) {
                System.out.println("Skipped upgrading global admin " + globalAdminId + " (Encountered error: " + e.getMessage() + ")");
            }
        }
        
    }
    
    private void getAllDomainOrGlobalAdmins(Set<String> domainAdminIds, Set<String> globalAdminIds) throws ServiceException {
        
        LdapDIT dit = mProv.getDIT();
        String returnAttrs[] = new String[] {Provisioning.A_objectClass,
                                             Provisioning.A_zimbraId,
                                             Provisioning.A_zimbraIsAdminAccount,
                                             Provisioning.A_zimbraIsDomainAdminAccount,
                                             Provisioning.A_zimbraIsDelegatedAdminAccount};
        
        String configBranchBaseDn = dit.configBranchBaseDN();
        String base = dit.mailBranchBaseDN();
        String query = "(&(objectclass=zimbraAccount)(|(zimbraIsDomainAdminAccount=TRUE)(zimbraIsAdminAccount=TRUE)))";
        
        int maxResults = 0; // no limit
        ZimbraLdapContext zlc = null; 
        
        try {
            // use master, do not use connection pool, use infinite read timeout
            zlc = new ZimbraLdapContext(true, new LdapConfig(Boolean.FALSE, null, LdapConfig.NO_TIMEOUT));  
            
            SearchControls searchControls =
                new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, returnAttrs, false, false);

            //Set the page size and initialize the cookie that we pass back in subsequent pages
            int pageSize = LdapUtil.adjustPageSize(maxResults, 1000);
            byte[] cookie = null;

            NamingEnumeration ne = null;
            
            
            try {
                do {
                    zlc.setPagedControl(pageSize, cookie, true);

                    ne = zlc.searchDir(base, query, searchControls);
                    while (ne != null && ne.hasMore()) {
                        SearchResult sr = (SearchResult) ne.nextElement();
                        String dn = sr.getNameInNamespace();

                        Attributes attrs = sr.getAttributes();
                        
                        // skip admin accounts under config branch
                        if (dn.endsWith(configBranchBaseDn))
                            continue;
                        
                        String globalAdminId = getZimbraIdIfGlobalAdmin(attrs);
                        if (globalAdminId != null)
                            globalAdminIds.add(globalAdminId);
                        else {
                            String domainAdminId = getZimbraIdIfDomainOnlyAdmin(attrs);
                            if (domainAdminId != null)
                                domainAdminIds.add(domainAdminId);
                        }
                    }
                    cookie = zlc.getCookie();
                } while (cookie != null);
            } finally {
                if (ne != null) ne.close();
            }
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }
    
    String getZimbraIdIfDomainOnlyAdmin(Attributes attrs) throws NamingException {
        String isAdmin = LdapUtil.getAttrString(attrs, Provisioning.A_zimbraIsAdminAccount);
        String isDomainAdmin = LdapUtil.getAttrString(attrs, Provisioning.A_zimbraIsDomainAdminAccount);
        String isDelegatedAdmin = LdapUtil.getAttrString(attrs, Provisioning.A_zimbraIsDelegatedAdminAccount);
        
        if (LdapUtil.LDAP_TRUE.equals(isDomainAdmin) &&
            !LdapUtil.LDAP_TRUE.equals(isAdmin) &&         // is a global admin, don't touch it
            !LdapUtil.LDAP_TRUE.equals(isDelegatedAdmin))  // already migrated, don't touch it
            return LdapUtil.getAttrString(attrs, Provisioning.A_zimbraId);
        else
            return null;
    }
    
    String getZimbraIdIfGlobalAdmin(Attributes attrs) throws NamingException {
        String isAdmin = LdapUtil.getAttrString(attrs, Provisioning.A_zimbraIsAdminAccount);
        
        if (LdapUtil.LDAP_TRUE.equals(isAdmin))
            return LdapUtil.getAttrString(attrs, Provisioning.A_zimbraId);
        else
            return null;
    }
    
    private void grantRights(Domain domain, Account domainAdmin) throws ServiceException {
        //
        // turn it into a delegated admin
        //
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, Provisioning.TRUE);
        mProv.modifyAttrs(domainAdmin, attrs);
        
        //
        // domain rights
        //
        mProv.grantRight(TargetType.domain.getCode(), TargetBy.id, domain.getId(), 
                GranteeType.GT_USER.getCode(), GranteeBy.id, domainAdmin.getId(), null,
                RightConsts.RT_domainAdminConsoleRights, RightModifier.RM_CAN_DELEGATE);
        
        //
        // cos rights
        //
        grantCosRights(domain, domainAdmin);
        
        //
        // zimlet rights
        //
        mProv.grantRight(TargetType.global.getCode(), null, null, 
                GranteeType.GT_USER.getCode(), GranteeBy.id, domainAdmin.getId(), null,
                RightConsts.RT_listZimlet, RightModifier.RM_CAN_DELEGATE);
        
        mProv.grantRight(TargetType.global.getCode(), null, null, 
                GranteeType.GT_USER.getCode(), GranteeBy.id, domainAdmin.getId(), null,
                RightConsts.RT_getZimlet, RightModifier.RM_CAN_DELEGATE);
        
        //
        // admin UI components
        //
        setDomainAdminUIComp(domainAdmin);
        
        //
        // quota
        //
        long maxQuota = domainAdmin.getLongAttr(Provisioning.A_zimbraDomainAdminMaxMailQuota, -1);
        if (maxQuota == -1)  // they don't have permission to change quota
            mProv.grantRight(TargetType.domain.getCode(), TargetBy.id, domain.getId(), 
                    GranteeType.GT_USER.getCode(), GranteeBy.id, domainAdmin.getId(), null,
                    InlineAttrRight.composeSetRight(TargetType.account, Provisioning.A_zimbraMailQuota), RightModifier.RM_DENY);
            
    }
    
    private void grantCosRights(Domain domain, Account domainAdmin) throws ServiceException {
        Set<String> allowedCoses = domain.getMultiAttrSet(Provisioning.A_zimbraDomainCOSMaxAccounts);
        
        for (String c : allowedCoses) {
            String[] parts = c.split(":");
            if (parts.length != 2)
                continue;  // bad value skip
            String cosId = parts[0];
            
            // sanity check
            Cos cos = mProv.get(CosBy.id, cosId);
            if (cos == null) {
                System.out.println("    cannot find cos " + cosId + ", skipping granting cos right to " + domainAdmin.getName());
                continue;
            }
            
            mProv.grantRight(TargetType.cos.getCode(), TargetBy.id, cosId, 
                    GranteeType.GT_USER.getCode(), GranteeBy.id, domainAdmin.getId(), null,
                    RightConsts.RT_listCos, RightModifier.RM_CAN_DELEGATE);
            
            mProv.grantRight(TargetType.cos.getCode(), TargetBy.id, cosId, 
                    GranteeType.GT_USER.getCode(), GranteeBy.id, domainAdmin.getId(), null,
                    RightConsts.RT_getCos, RightModifier.RM_CAN_DELEGATE);
            
            mProv.grantRight(TargetType.cos.getCode(), TargetBy.id, cosId, 
                    GranteeType.GT_USER.getCode(), GranteeBy.id, domainAdmin.getId(), null,
                    RightConsts.RT_assignCos, RightModifier.RM_CAN_DELEGATE);
        }
        
    }
    
    private void setDomainAdminUIComp(Account domainAdmin) throws ServiceException {
        setAdminUIComp(domainAdmin, sAdminUICompForAllDomainAdmins);
    }
    
    private void setGlobalAdminUIComp(Account globalAdmin) throws ServiceException {
        setAdminUIComp(globalAdmin, sAdminUICompForAllGlobalAdmins);
    }
    
    private void setAdminUIComp(Account admin, String[] adminUIComp) throws ServiceException {
        
        String attrName = Provisioning.A_zimbraAdminConsoleUIComponents;
        
        // do nothing if already set, should we?
        /*
        String[] curUIComps = admin.getMultiAttr(attrName);
        if (curUIComps.length > 0)
            return;
        */
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("+" + attrName, adminUIComp);
        
        mProv.modifyAttrs(admin, attrs);
    }
    
    
    public static void main(String[] args) throws ServiceException {

        /*
        Provisioning prov = Provisioning.getInstance();
        
        String domainName = "test.com";
        Domain domain = prov.createDomain(domainName, new HashMap<String, Object>());
        
        String globalAdminName = "global-admin@" + domainName;
        String domainAdminName = "domain-admin@" + domainName;
        String bothAdminName = "both-admin@" + domainName;
        
        Map<String, Object> globalAdminAttrs = new HashMap<String, Object>();
        globalAdminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account globalAdmin = prov.createAccount(globalAdminName, "test123", globalAdminAttrs);
        
        Map<String, Object> domainAdminAttrs = new HashMap<String, Object>();
        domainAdminAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account domainAdmin = prov.createAccount(domainAdminName, "test123", domainAdminAttrs);
        
        Map<String, Object> bothAdminAttrs = new HashMap<String, Object>();
        bothAdminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        bothAdminAttrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account bothAdmin = prov.createAccount(bothAdminName, "test123", bothAdminAttrs);
        */
        
        UpgradeTask upgradeTask = UpgradeTask.fromString("18277");
        LdapUpgrade upgrade = upgradeTask.getUpgrader();
        upgrade.setVerbose(true);
        upgrade.doUpgrade();
        
    }

}
