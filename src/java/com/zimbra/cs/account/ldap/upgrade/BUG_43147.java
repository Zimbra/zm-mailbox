/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_43147 extends UpgradeOp {

    @Override
    void doUpgrade() throws ServiceException {
        
        Set<String> galSyncAcctIds = getAllGalSyncAcctIds();
        
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            upgradeGalSyncAcct(zlc, galSyncAcctIds);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    private static class Bug43147Visitor extends SearchLdapOptions.SearchLdapVisitor {
        
        private UpgradeOp upgradeOp;
        private Set<String> galSyncAcctIds;
        
        Bug43147Visitor(UpgradeOp upgradeOp, Set<String> galSyncAcctIds) {
            this.upgradeOp = upgradeOp;
            this.galSyncAcctIds = galSyncAcctIds;
        }
        
        @Override
        public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
            upgradeOp.printer.println("Domain " + attrs.get(Provisioning.A_zimbraDomainName));
                
            Object values = attrs.get(Provisioning.A_zimbraGalAccountId);
            if (values instanceof String) {
                upgradeOp.printer.println(" GAL sync account " + (String)values);
                galSyncAcctIds.add((String)values);
            } else if (values instanceof String[]) {
                for (String value : (String[])values) {
                    upgradeOp.printer.println(" GAL sync account " + value);
                   galSyncAcctIds.add(value);
                }
            }
        }
    }
    
    private Set<String> getAllGalSyncAcctIds() throws ServiceException {
        Set<String> galSyncAcctIds = new HashSet<String>();
        
        Bug43147Visitor visitor = new Bug43147Visitor(this, galSyncAcctIds);
        
        String query = "(&(objectClass=zimbraDomain)(zimbraGalAccountId=*))";
        String bases[] = prov.getDIT().getSearchBases(Provisioning.SD_DOMAIN_FLAG);
        String attrs[] = new String[] {Provisioning.A_zimbraDomainName,
                                       Provisioning.A_zimbraGalAccountId};
        
        for (String base : bases) {
            prov.searchLdapOnMaster(base, query, attrs, visitor);
        }
        
        return galSyncAcctIds;
    }
    
    private void upgradeGalSyncAcct(ZLdapContext zlc, Set<String> galSyncAcctIds) throws ServiceException {
        printer.println();
        printer.println("Upgrading zimbraContactMaxNumEntries on GAL sync accounts ...");
        printer.println();
        
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraContactMaxNumEntries, "0");
        
        // got all GAL sync account ids, upgrade them
        for (String id : galSyncAcctIds) {
            Account acct = prov.getAccountById(id);
            if (acct != null) {
                // upgrade only when the gal sync account does not have zimbraContactMaxNumEntries 
                // set on the account entry
                String curValue = acct.getAttr(Provisioning.A_zimbraContactMaxNumEntries, false);
                
                if (curValue == null) {
                    try {
                        printer.println("Account: " + acct.getId() + "(" + acct.getName() + ") - "+ "modifying zimbraContactMaxNumEntries to 0");
                        modifyAttrs(zlc, acct, attrs);
                    } catch (ServiceException e) {
                        printer.println("Caught ServiceException while modifying GAL sync account entry " + acct.getName());
                        printer.printStackTrace(e);
                    }
                } else
                    printer.println("Account: " + acct.getId() + "(" + acct.getName() + ") - "+ "already has value " + curValue + ", skipping");
            } else {
                printer.println("Account: " + id + " - no such account");
            }
        }

    }

}
