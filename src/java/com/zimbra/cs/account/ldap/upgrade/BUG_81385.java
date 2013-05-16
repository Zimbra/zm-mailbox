/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;

public class BUG_81385 extends UpgradeOp {

    private static String ATTR_DEVICEENCRYTIONENABLED = Provisioning.A_zimbraMobilePolicyDeviceEncryptionEnabled;
    private static String ATTR_REQUIRESTORAGECARDENCRYPTION = Provisioning.A_zimbraMobilePolicyRequireStorageCardEncryption;
    
    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doCos(zlc);
            doAccount(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    private static class Bug81385Visitor extends SearchLdapVisitor {
        private UpgradeOp upgradeOp;
        private ZLdapContext modZlc;
        
        private Bug81385Visitor(UpgradeOp upgradeOp, ZLdapContext modZlc) {
            this.upgradeOp = upgradeOp;
            this.modZlc = modZlc;
        }
        
        @Override
        public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
            ZMutableEntry entry = LdapClient.createMutableEntry();
            
            try {
                if (ldapAttrs.getAttrString(ATTR_DEVICEENCRYTIONENABLED) != null) {
                    entry.setAttr(ATTR_REQUIRESTORAGECARDENCRYPTION, ldapAttrs.getAttrString(ATTR_DEVICEENCRYTIONENABLED));
                }
                
                upgradeOp.printer.println("Modifying " + dn);
                upgradeOp.replaceAttrs(modZlc, dn, entry);
            } catch (ServiceException e) {
                // log and continue
                upgradeOp.printer.println("Caught ServiceException while modifying " + dn);
                upgradeOp.printer.printStackTrace(e);
            }
        }
    }
    
    private void upgrade(ZLdapContext modZlc, String bases[], String query) {
        SearchLdapOptions.SearchLdapVisitor visitor = new Bug81385Visitor(this, modZlc);

        String attrs[] = new String[] {ATTR_DEVICEENCRYTIONENABLED, ATTR_REQUIRESTORAGECARDENCRYPTION};
        
        for (String base : bases) {
            try {
                prov.searchLdapOnMaster(base, query, attrs, visitor);
            } catch (ServiceException e) {
                // log and continue
                printer.println("Caught ServiceException while searching " + query + " under base " + base);
                printer.printStackTrace(e);
            }
        }
    }
    
    private String query() {
        return "(" + ATTR_DEVICEENCRYTIONENABLED + "=*)";
    }

    private void doCos(ZLdapContext modZlc) {
        String bases[] = prov.getDIT().getSearchBases(Provisioning.SD_COS_FLAG);
        String query = "(&" + ZLdapFilterFactory.getInstance().allCoses().toFilterString() + query() + ")";
        upgrade(modZlc, bases, query);
    }
    
    private void doAccount(ZLdapContext modZlc) {
        String bases[] = prov.getDIT().getSearchBases(Provisioning.SD_ACCOUNT_FLAG);
        String query = "(&" + ZLdapFilterFactory.getInstance().allAccounts().toFilterString() + query() + ")";
        upgrade(modZlc, bases, query);
    }
}
