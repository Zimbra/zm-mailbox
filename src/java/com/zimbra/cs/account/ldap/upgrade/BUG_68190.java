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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZMutableEntry;

public class BUG_68190 extends UpgradeOp {
    
    private static final String ATTR_NAME = Provisioning.A_zimbraWebClientLoginURL;
    private static final String OLD_VALUE = "../../service/spnego";
    private static final String NEW_VALUE = "/service/spnego";

    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            upgradeDomains(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    Description getDescription() {
        return new Description(this, 
                new String[] {ATTR_NAME}, 
                new EntryType[] {EntryType.DOMAIN},
                OLD_VALUE, 
                NEW_VALUE, 
                String.format("Upgrade attribute %s on all domains from \"%s\" to \"%s\"", 
                        ATTR_NAME, OLD_VALUE, NEW_VALUE));
    }
    
    private void upgradeDomains(ZLdapContext zlc) {
        String bases[] = prov.getDIT().getSearchBases(Provisioning.SD_DOMAIN_FLAG);
        String query = "(&" + ZLdapFilterFactory.getInstance().allDomains().toFilterString() + 
            "(" + ATTR_NAME + "=" + OLD_VALUE + ")"+ ")";
        
        upgrade(zlc, bases, query);
    }
    
    
    private void upgrade(ZLdapContext modZlc, String bases[], String filter) {
        SearchLdapOptions.SearchLdapVisitor visitor = new Bug68190Visitor(this, modZlc);

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
    
    private static class Bug68190Visitor extends SearchLdapOptions.SearchLdapVisitor {
        private UpgradeOp upgradeOp;
        private ZLdapContext modZlc;
        
        Bug68190Visitor(UpgradeOp upgradeOp, ZLdapContext modZlc) {
            super(false);
            this.upgradeOp = upgradeOp;
            this.modZlc = modZlc;
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
            String curValue = ldapAttrs.getAttrString(ATTR_NAME);
            
            upgradeOp.printer.println();
            upgradeOp.printer.println("Found entry " + dn);
            upgradeOp.printer.format("current value of %s is %s\n", ATTR_NAME, curValue);
            
            if (OLD_VALUE.equals(curValue)) {
                ZMutableEntry entry = LdapClient.createMutableEntry();
                entry.setAttr(ATTR_NAME, NEW_VALUE);
                upgradeOp.printer.format("updating %s to %s\n", ATTR_NAME, NEW_VALUE);
                upgradeOp.replaceAttrs(modZlc, dn, entry);
            } else {
                upgradeOp.printer.format("not updating %s\n", ATTR_NAME);
            }
        }
    }
}
