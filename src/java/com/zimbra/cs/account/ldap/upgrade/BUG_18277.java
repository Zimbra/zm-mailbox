/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.InlineAttrRight;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.soap.type.TargetBy;

public class BUG_18277 extends UpgradeOp {

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
    
    @Override
    void doUpgrade() throws ServiceException {
        
        Set<String> domainAdminIds = new HashSet<String>();
        Set<String> globalAdminIds = new HashSet<String>();
        
        getAllDomainOrGlobalAdmins(domainAdminIds, globalAdminIds);
        
        for (String domainAdminId : domainAdminIds) {
            try {
                Account domainAdmin = prov.get(AccountBy.id, domainAdminId);
                if (domainAdmin == null)
                    continue;
                
                Domain domain = prov.getDomain(domainAdmin);
                if (domain == null)
                    continue;
                
                printer.println("Upgrading domain admin: " + domainAdmin.getName());
                grantRights(domain, domainAdmin);
            } catch (ServiceException e) {
                printer.println("Skipped upgrading global admin " + domainAdminId + " (Encountered error: " + e.getMessage() + ")");
            }
        }
        
        for (String globalAdminId : globalAdminIds) {
            try {
                Account globalAdmin = prov.get(AccountBy.id, globalAdminId);
                if (globalAdmin == null)
                    continue;
                
                printer.println("Upgrading global admin: " + globalAdmin.getName());
                setGlobalAdminUIComp(globalAdmin);
            } catch (ServiceException e) {
                printer.println("Skipped upgrading global admin " + globalAdminId + " (Encountered error: " + e.getMessage() + ")");
            }
        }
        
    }
    
    
    private static class Bug18277Visitor extends SearchLdapOptions.SearchLdapVisitor {
        
        private UpgradeOp upgradeOp;
        private String configBranchBaseDn;
        private Set<String> domainAdminIds;
        private Set<String> globalAdminIds;
        
        private Bug18277Visitor(UpgradeOp upgradeOp, String configBranchBaseDn, 
                Set<String> domainAdminIds, Set<String> globalAdminIds) {
            super(false);
            this.upgradeOp = upgradeOp;
            this.configBranchBaseDn = configBranchBaseDn;
            this.domainAdminIds = domainAdminIds;
            this.globalAdminIds = globalAdminIds;
        }
        
        @Override
        public void visit(String dn, IAttributes ldapAttrs) {
            try {
                doVisit(dn, (ZAttributes) ldapAttrs);
            } catch (ServiceException e) {
                upgradeOp.printer.println("entry skipped, encountered error while processing entry at:" + dn);
                upgradeOp.printer.printStackTrace(e);
            }
        }
        
        private void doVisit(String dn, ZAttributes ldapAttrs) throws ServiceException {
            
            // skip admin accounts under config branch
            if (dn.endsWith(configBranchBaseDn)) {
                return;
            }
            
            String globalAdminId = getZimbraIdIfGlobalAdmin(ldapAttrs);
            if (globalAdminId != null)
                globalAdminIds.add(globalAdminId);
            else {
                String domainAdminId = getZimbraIdIfDomainOnlyAdmin(ldapAttrs);
                if (domainAdminId != null)
                    domainAdminIds.add(domainAdminId);
            }
        }
        
        
        private String getZimbraIdIfDomainOnlyAdmin(ZAttributes attrs) throws ServiceException {
            String isAdmin = attrs.getAttrString(Provisioning.A_zimbraIsAdminAccount);
            String isDomainAdmin = attrs.getAttrString(Provisioning.A_zimbraIsDomainAdminAccount);
            String isDelegatedAdmin = attrs.getAttrString(Provisioning.A_zimbraIsDelegatedAdminAccount);
            
            if (LdapConstants.LDAP_TRUE.equals(isDomainAdmin) &&
                !LdapConstants.LDAP_TRUE.equals(isAdmin) &&         // is a global admin, don't touch it
                !LdapConstants.LDAP_TRUE.equals(isDelegatedAdmin))  // already migrated, don't touch it
                return attrs.getAttrString(Provisioning.A_zimbraId);
            else
                return null;
        }
        
        private String getZimbraIdIfGlobalAdmin(ZAttributes attrs) throws ServiceException {
            String isAdmin = attrs.getAttrString(Provisioning.A_zimbraIsAdminAccount);
            
            if (LdapConstants.LDAP_TRUE.equals(isAdmin))
                return attrs.getAttrString(Provisioning.A_zimbraId);
            else
                return null;
        }
    }
    
    private void getAllDomainOrGlobalAdmins(Set<String> domainAdminIds, Set<String> globalAdminIds) throws ServiceException {
        
        LdapDIT dit = prov.getDIT();
        String returnAttrs[] = new String[] {Provisioning.A_objectClass,
                                             Provisioning.A_zimbraId,
                                             Provisioning.A_zimbraIsAdminAccount,
                                             Provisioning.A_zimbraIsDomainAdminAccount,
                                             Provisioning.A_zimbraIsDelegatedAdminAccount};
        
        String configBranchBaseDn = dit.configBranchBaseDN();
        String base = dit.mailBranchBaseDN();
        String query = "(&(objectclass=zimbraAccount)(|(zimbraIsDomainAdminAccount=TRUE)(zimbraIsAdminAccount=TRUE)))";
        
        ZLdapContext zlc = null; 
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
            
            Bug18277Visitor visitor = new Bug18277Visitor(this, configBranchBaseDn, domainAdminIds, globalAdminIds);
            
            SearchLdapOptions searchOpts = new SearchLdapOptions(base, getFilter(query), 
                    returnAttrs, SearchLdapOptions.SIZE_UNLIMITED, null, 
                    ZSearchScope.SEARCH_SCOPE_SUBTREE, visitor);
            
            zlc.searchPaged(searchOpts);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    private void grantRights(Domain domain, Account domainAdmin) throws ServiceException {
        //
        // turn it into a delegated admin
        //
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, ProvisioningConstants.TRUE);
        prov.modifyAttrs(domainAdmin, attrs);
        
        //
        // domain rights
        //
        prov.grantRight(TargetType.domain.getCode(), TargetBy.id, domain.getId(), 
                GranteeType.GT_USER.getCode(), Key.GranteeBy.id, domainAdmin.getId(), null,
                RightConsts.RT_domainAdminConsoleRights, RightModifier.RM_CAN_DELEGATE);
        
        //
        // cos rights
        //
        grantCosRights(domain, domainAdmin);
        
        //
        // zimlet rights
        //
        prov.grantRight(TargetType.global.getCode(), null, null, 
                GranteeType.GT_USER.getCode(), Key.GranteeBy.id, domainAdmin.getId(), null,
                RightConsts.RT_listZimlet, RightModifier.RM_CAN_DELEGATE);
        
        prov.grantRight(TargetType.global.getCode(), null, null, 
                GranteeType.GT_USER.getCode(), Key.GranteeBy.id, domainAdmin.getId(), null,
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
            prov.grantRight(TargetType.domain.getCode(), TargetBy.id, domain.getId(), 
                    GranteeType.GT_USER.getCode(), Key.GranteeBy.id, domainAdmin.getId(), null,
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
            Cos cos = prov.get(Key.CosBy.id, cosId);
            if (cos == null) {
                printer.println("    cannot find cos " + cosId + ", skipping granting cos right to " + domainAdmin.getName());
                continue;
            }
            
            prov.grantRight(TargetType.cos.getCode(), TargetBy.id, cosId, 
                    GranteeType.GT_USER.getCode(), Key.GranteeBy.id, domainAdmin.getId(), null,
                    RightConsts.RT_listCos, RightModifier.RM_CAN_DELEGATE);
            
            prov.grantRight(TargetType.cos.getCode(), TargetBy.id, cosId, 
                    GranteeType.GT_USER.getCode(), Key.GranteeBy.id, domainAdmin.getId(), null,
                    RightConsts.RT_getCos, RightModifier.RM_CAN_DELEGATE);
            
            prov.grantRight(TargetType.cos.getCode(), TargetBy.id, cosId, 
                    GranteeType.GT_USER.getCode(), Key.GranteeBy.id, domainAdmin.getId(), null,
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
        
        prov.modifyAttrs(admin, attrs);
    }

}
