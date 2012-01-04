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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.SearchLdapOptions.StopIteratingException;

public class BUG_57866 extends UpgradeOp {

    private static final String ATTR_NAME = Provisioning.A_zimbraIsSystemAccount;
    private static final String VALUE = LdapConstants.LDAP_TRUE;
    
    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = null;
        
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
            
            try {
                printer.println("\n=== Upgrading spam account");
                upgradeSpamAccount(zlc);
            } catch (ServiceException e) {
                printer.printStackTrace("unable to upgrade spam account", e);
            }
            
            try {
                printer.println("\n=== Upgrading ham account");
                upgradeHamAccount(zlc);
            } catch (ServiceException e) {
                printer.printStackTrace("unable to upgrade ham account", e);
            }
            
            try {
                printer.println("\n=== Upgrading wiki accounts");
                upgradeWikiAccounts(zlc);
            } catch (ServiceException e) {
                printer.printStackTrace("unable to upgrade wiki accounts", e);
            }
            
            try {
                printer.println("\n=== Upgrading GAL sync accounts");
                upgradeGalSyncAccounts(zlc);
            } catch (ServiceException e) {
                printer.printStackTrace("unable to upgrade GAL sync accounts", e);
            }
            
            try {
                printer.println("\n=== Upgrading quarantine account");
                upgradeQuarantineAccount(zlc);
            } catch (ServiceException e) {
                printer.printStackTrace("unable to upgrade quarantine account", e);
            }
            
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    @Override
    Description getDescription() {
        return new Description(
                this, 
                new String[] {ATTR_NAME}, 
                new EntryType[] {EntryType.ACCOUNT},
                null, 
                VALUE, 
                String.format("set %s to %s on all spam/ham/global and domain wiki/GAL sync" +
                        "/quarantine accounts.", ATTR_NAME, VALUE));
    }

    private void setIsSystemAccount(ZLdapContext zlc, Account acct) throws ServiceException {
        if (acct == null) {
            return;
        }
        
        if (!acct.isIsSystemAccount()) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(ATTR_NAME, VALUE); 
            modifyAttrs(zlc, acct, attrs);
        }
    }
    
    private void upgradeSpamAccount(ZLdapContext zlc) throws ServiceException {
        String acctName = prov.getConfig().getSpamIsSpamAccount();
        if (acctName != null) {
            printer.format("Checking spam account %s\n", acctName);
            Account acct = prov.get(AccountBy.name, acctName);
            setIsSystemAccount(zlc, acct);
        }
    }
    
    private void upgradeHamAccount(ZLdapContext zlc) throws ServiceException {
        String acctName = prov.getConfig().getSpamIsNotSpamAccount();
        if (acctName != null) {
            printer.format("Checking ham account %s\n", acctName);
            Account acct = prov.get(AccountBy.name, acctName);
            setIsSystemAccount(zlc, acct);
        }
    }
    
    private void upgradeWikiAccounts(ZLdapContext zlc) throws ServiceException {
        // global wiki account
        String acctName = prov.getConfig().getNotebookAccount();
        if (acctName != null) {
            printer.format("Checking global wiki account %s\n", acctName);
            Account acct = prov.get(AccountBy.name, acctName);
            setIsSystemAccount(zlc, acct);
        }
        
        // domain wiki accounts
        LdapDIT dit = prov.getDIT();
        String returnAttrs[] = new String[] {Provisioning.A_zimbraNotebookAccount};
        
        String base = dit.mailBranchBaseDN();
        String query = "(&(objectclass=zimbraDomain)(zimbraNotebookAccount=*))";
        
        final Set<String> wikiAcctNames = new HashSet<String>();
        SearchLdapVisitor visitor = new SearchLdapVisitor(false) {
            @Override
            public void visit(String dn, IAttributes ldapAttrs)
            throws StopIteratingException {
                
                try {
                    String acctName;
                    acctName = ldapAttrs.getAttrString(Provisioning.A_zimbraNotebookAccount);
                    if (acctName != null) {
                        wikiAcctNames.add(acctName);
                    }
                } catch (ServiceException e) {
                    printer.printStackTrace("unsble to search domains for wiki accounts", e);
                }
             }
        };
        
        SearchLdapOptions searchOpts = new SearchLdapOptions(base, getFilter(query), 
                returnAttrs, SearchLdapOptions.SIZE_UNLIMITED, null, 
                ZSearchScope.SEARCH_SCOPE_SUBTREE, visitor);
        
        zlc.searchPaged(searchOpts);

        for (String wikiAcctName : wikiAcctNames) {
            printer.format("Checking domain wiki account %s\n", wikiAcctName);
            Account acct = prov.get(AccountBy.name, wikiAcctName);
            setIsSystemAccount(zlc, acct);
        }
    }
    
    private void upgradeGalSyncAccounts(ZLdapContext zlc) throws ServiceException {
        LdapDIT dit = prov.getDIT();
        String returnAttrs[] = new String[] {Provisioning.A_zimbraGalAccountId};
        
        String base = dit.mailBranchBaseDN();
        String query = "(&(objectclass=zimbraDomain)(zimbraGalAccountId=*))";
        
        final Set<String> galAcctIds = new HashSet<String>();
        SearchLdapVisitor visitor = new SearchLdapVisitor(false) {
            @Override
            public void visit(String dn, IAttributes ldapAttrs)
            throws StopIteratingException {
                
                try {
                    String acctId;
                    acctId = ldapAttrs.getAttrString(Provisioning.A_zimbraGalAccountId);
                    if (acctId != null) {
                        galAcctIds.add(acctId);
                    }
                } catch (ServiceException e) {
                    printer.printStackTrace("unsble to search domains for GAL sync accounts", e);
                }
             }
        };
        
        SearchLdapOptions searchOpts = new SearchLdapOptions(base, getFilter(query), 
                returnAttrs, SearchLdapOptions.SIZE_UNLIMITED, null, 
                ZSearchScope.SEARCH_SCOPE_SUBTREE, visitor);
        
        zlc.searchPaged(searchOpts);

        for (String galAcctId : galAcctIds) {
            printer.format("Checking GAL sync account %s\n", galAcctId);
            Account acct = prov.get(AccountBy.id, galAcctId);
            setIsSystemAccount(zlc, acct);
        }
    }
    
    private void upgradeQuarantineAccount(ZLdapContext zlc) throws ServiceException {
        String acctName = prov.getConfig().getAmavisQuarantineAccount();
        if (acctName != null) {
            printer.format("Checking quarantine account %s\n", acctName);
            Account acct = prov.get(AccountBy.name, acctName);
            setIsSystemAccount(zlc, acct);
        }
    }
    

}
