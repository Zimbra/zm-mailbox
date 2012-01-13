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

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;

public class BUG_68831 extends UpgradeOp {
    private static final String ATTR_NAME = Provisioning.A_zimbraMailHost;
    
    @Override
    void doUpgrade() throws ServiceException {
        
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            upgradeDLs(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    @Override
    Description getDescription() {
        return new Description(this, 
                new String[] {ATTR_NAME}, 
                new EntryType[] {EntryType.DISTRIBUTIONLIST},
                null, 
                "randomly picked hostname", 
                String.format("Add home server(%s) on distribution lists", ATTR_NAME));
    }
    
    
    private void upgradeDLs(ZLdapContext zlc) throws ServiceException {
        String bases[] = prov.getDIT().getSearchBases(Provisioning.SD_DISTRIBUTION_LIST_FLAG);
        
        ZLdapFilterFactory filterFactory = ZLdapFilterFactory.getInstance();
        ZLdapFilter homeServerNotPresent = 
            filterFactory.negate(filterFactory.presenceFilter(FilterId.LDAP_UPGRADE, ATTR_NAME));
        
        String query = filterFactory.andWith(
                filterFactory.allDistributionLists(), homeServerNotPresent).toFilterString();

        upgrade(zlc, bases, query);
    }
    
    
    private void upgrade(ZLdapContext modZlc, String bases[], String filter) 
    throws ServiceException {
        SearchLdapOptions.SearchLdapVisitor visitor = new Bug68831Visitor(this, modZlc);

        String attrs[] = new String[] {ATTR_NAME};
        
        for (String base : bases) {
            try {
                printer.format("\n=== Searching LDAP: base = %s, filter = %s\n", base, filter);
                prov.searchLdapOnMaster(base, filter, attrs, visitor);
            } catch (ServiceException e) {
                // log and continue
                printer.println("Caught ServiceException while searching " + filter + " under base " + base);
                printer.printStackTrace(e);
            }
        }
    }
    
    private static class Bug68831Visitor extends SearchLdapOptions.SearchLdapVisitor {
        private UpgradeOp upgradeOp;
        private ZLdapContext modZlc;
        private List<String> servers = Lists.newArrayList();
        private int index = 0; // index into the List for picking a server in a round robin manner
        
        Bug68831Visitor(UpgradeOp upgradeOp, ZLdapContext modZlc) throws ServiceException {
            super(false);
            this.upgradeOp = upgradeOp;
            this.modZlc = modZlc;
            
            for (Server server: upgradeOp.prov.getAllServers()) {
                String serviceHostname = server.getServiceHostname();
                if (server.hasMailboxService() && serviceHostname != null) {
                    servers.add(serviceHostname);
                }
            }
            
            if (servers.size() == 0) {
                throw ServiceException.FAILURE("no mailbox server for assigning home server for distribution lists", null);
            }
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
        
        public void doVisit(String dn, ZAttributes ldapAttrs) throws ServiceException {
            upgradeOp.printer.println();
            upgradeOp.printer.println("Found entry " + dn);
                      
            String homeServer = pickServer();
            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.setAttr(ATTR_NAME, homeServer);
            upgradeOp.replaceAttrs(modZlc, dn, entry);
        }
        
        private String pickServer() {
            String serviceHostname = servers.get(index);
            index = (index + 1) % servers.size();
            return serviceHostname;
        }
    }
}
