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
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.account.ldap.entry.LdapDomain;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZSearchScope;

public class BUG_32557 extends UpgradeOp {
    
    private static class Bug32557Visitor extends SearchLdapOptions.SearchLdapVisitor {
        
        private UpgradeOp upgradeOp;
        private ZLdapContext modZlc;
        
        private int mDomainsVisited;
    
        Bug32557Visitor(UpgradeOp upgradeOp, ZLdapContext modZlc) {
            super(false);
            this.upgradeOp = upgradeOp;
            this.modZlc = modZlc;
        }
        
        @Override
        public void visit(String dn, IAttributes ldapAttrs) {
            Domain domain;
            try {
                domain = new LdapDomain(dn, (ZAttributes)ldapAttrs, 
                        upgradeOp.prov.getConfig().getDomainDefaults(), upgradeOp.prov);
                visit(domain);
            } catch (ServiceException e) {
                upgradeOp.printer.println("entry skipped, encountered error while processing entry at:" + dn);
                upgradeOp.printer.printStackTrace(e);
            }
            
        }
        
        private void visit(Domain domain) {
            mDomainsVisited++;
            
            Map<String, Object> attrs = new HashMap<String, Object>(); 
            attrs.put("+" + Provisioning.A_objectClass, "amavisAccount");
            
            try {
                upgradeOp.printer.format("Updating domain %-30s: objectClass=amavisAccount\n",
                                  domain.getName());
                upgradeOp.modifyAttrs(modZlc, domain, attrs);
            } catch (ServiceException e) {
                // log the exception and continue
                upgradeOp.printer.println("Caught ServiceException while modifying domain " + domain.getName());
                upgradeOp.printer.printStackTrace(e);
            }
        }
        
        void reportStat() {
            upgradeOp.printer.println();
            upgradeOp.printer.println("Number of domains modified = " + mDomainsVisited);
            upgradeOp.printer.println();
        }
    }
    
    /**
     * Add objectClass=amavisAccount to all existing domains
     * 
     */
    @Override
    void doUpgrade() throws ServiceException {
        
        String query = "(&(objectClass=zimbraDomain)(!(objectClass=amavisAccount)))";
        String bases[] = prov.getDIT().getSearchBases(Provisioning.SD_DOMAIN_FLAG);
        String attrs[] = new String[] {Provisioning.A_objectClass,
                                       Provisioning.A_zimbraId,
                                       Provisioning.A_zimbraDomainName};
                
        ZLdapContext zlc = null; 
        Bug32557Visitor visitor = new Bug32557Visitor(this, zlc);
        
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
            
            for (String base : bases) {
                // should really have one base, but iterate thought the array anyway
                if (verbose) {
                    printer.println("LDAP search base: " + base);
                    printer.println("LDAP search query: " + query);
                    printer.println();
                }
                
                SearchLdapOptions searchOpts = new SearchLdapOptions(base, getFilter(query), 
                        attrs, SearchLdapOptions.SIZE_UNLIMITED, null, 
                        ZSearchScope.SEARCH_SCOPE_SUBTREE, visitor);
                
                zlc.searchPaged(searchOpts);
             
            }
        } finally {
            LdapClient.closeContext(zlc);
            if (visitor != null)
                visitor.reportStat();
        }
    }
    
    @Override
    Description getDescription() {
        return new Description(
                this, 
                new String[] {Provisioning.A_objectClass}, 
                new EntryType[] {EntryType.DOMAIN},
                null, 
                "amavisAccount",  
                "Add objectClass=amavisAccount to all existing domains.");
    }

}
