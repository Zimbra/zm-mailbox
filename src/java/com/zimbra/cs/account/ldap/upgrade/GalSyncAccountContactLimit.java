/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.account.ldap.LdapUtil.SearchLdapVisitor;

public class GalSyncAccountContactLimit extends LdapUpgrade {

    GalSyncAccountContactLimit() throws ServiceException {
    }

    @Override
    void doUpgrade() throws ServiceException {
        
        Set<String> galSyncAcctIds = getAllGalSyncAcctIds();
        
        ZimbraLdapContext zlc = new ZimbraLdapContext(true);
        try {
            upgradeGalSyncAcct(zlc, galSyncAcctIds);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }
    
    private Set<String> getAllGalSyncAcctIds() throws ServiceException {
        final Set<String> galSyncAcctIds = new HashSet<String>();
        
        SearchLdapVisitor visitor = new SearchLdapVisitor() {
            public void visit(String dn, Map<String, Object> attrs, Attributes ldapAttrs) {
                System.out.println("Domain " + attrs.get(Provisioning.A_zimbraDomainName));
                
                Object values = attrs.get(Provisioning.A_zimbraGalAccountId);
                if (values instanceof String) {
                    System.out.println(" GAL sync account " + (String)values);
                    galSyncAcctIds.add((String)values);
                } else if (values instanceof String[]) {
                    for (String value : (String[])values) {
                       System.out.println(" GAL sync account " + value);
                       galSyncAcctIds.add(value);
                    }
                }
            }
        };
        
        String query = "(&(objectClass=zimbraDomain)(zimbraGalAccountId=*))";
        String bases[] = mProv.getSearchBases(Provisioning.SA_DOMAIN_FLAG);
        String attrs[] = new String[] {Provisioning.A_zimbraDomainName,
                                       Provisioning.A_zimbraGalAccountId};
        
        for (String base : bases) {
            LdapUtil.searchLdapOnMaster(base, query, attrs, visitor);
        }
        
        return galSyncAcctIds;
    }
    
    private void upgradeGalSyncAcct(ZimbraLdapContext zlc, Set<String> galSyncAcctIds) throws ServiceException {
        System.out.println();
        System.out.println("Upgrading zimbraContactMaxNumEntries on GAL sync accounts ...");
        System.out.println();
        
        HashMap<String,String> attrs = new HashMap<String,String>();
        attrs.put(Provisioning.A_zimbraContactMaxNumEntries, "0");
        
        // got all GAL sync account ids, upgrade them
        for (String id : galSyncAcctIds) {
            Account acct = mProv.getAccountById(id);
            if (acct != null) {
                // upgrade only when the gal sync account does not have zimbraContactMaxNumEntries 
                // set on the account entry
                String curValue = acct.getAttr(Provisioning.A_zimbraContactMaxNumEntries, false);
                
                if (curValue == null) {
                    try {
                        System.out.println("Account: " + acct.getId() + "(" + acct.getName() + ") - "+ "modifying zimbraContactMaxNumEntries to 0");
                        modifyAttrs(acct, zlc, attrs);
                    } catch (ServiceException e) {
                        System.out.println("Caught ServiceException while modifying GAL sync account entry " + acct.getName());
                        e.printStackTrace();
                    } catch (NamingException e) {
                        System.out.println("Caught NamingException while modifying GAL sync account entry " + acct.getName());
                        e.printStackTrace();
                    }
                } else
                    System.out.println("Account: " + acct.getId() + "(" + acct.getName() + ") - "+ "already has value " + curValue + ", skipping");
            } else {
                System.out.println("Account: " + id + " - no such account");
            }
        }

    }
}
