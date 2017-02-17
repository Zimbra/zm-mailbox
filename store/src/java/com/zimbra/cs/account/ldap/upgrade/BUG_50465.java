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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;

public class BUG_50465 extends UpgradeOp {

    private static String ATTR_SPREADSHEET = Provisioning.A_zimbraFeatureBriefcaseSpreadsheetEnabled;
    private static String ATTR_SLIDES = Provisioning.A_zimbraFeatureBriefcaseSlidesEnabled;
    private static String ATTR_NOTEBOOK = Provisioning.A_zimbraFeatureNotebookEnabled;
    
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
    
    private static class Bug50465Visitor extends SearchLdapVisitor {
        private UpgradeOp upgradeOp;
        private ZLdapContext modZlc;
        
        private Bug50465Visitor(UpgradeOp upgradeOp, ZLdapContext modZlc) {
            this.upgradeOp = upgradeOp;
            this.modZlc = modZlc;
        }
        
        @Override
        public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
            ZMutableEntry entry = LdapClient.createMutableEntry();
            
            try {
                if (ldapAttrs.getAttrString(ATTR_SPREADSHEET) != null)
                    entry.setAttr(ATTR_SPREADSHEET, LdapConstants.LDAP_FALSE);
                
                if (ldapAttrs.getAttrString(ATTR_SLIDES) != null)
                    entry.setAttr(ATTR_SLIDES, LdapConstants.LDAP_FALSE);
                
                if (ldapAttrs.getAttrString(ATTR_NOTEBOOK) != null)
                    entry.setAttr(ATTR_NOTEBOOK, LdapConstants.LDAP_FALSE);
                
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
        SearchLdapOptions.SearchLdapVisitor visitor = new Bug50465Visitor(this, modZlc);

        String attrs[] = new String[] {ATTR_SPREADSHEET, ATTR_SLIDES, ATTR_NOTEBOOK};
        
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
        return "(|(" + ATTR_SPREADSHEET + "=" + LdapConstants.LDAP_TRUE + ")" + 
                 "(" + ATTR_SLIDES + "=" + LdapConstants.LDAP_TRUE + ")" + 
                 "(" + ATTR_NOTEBOOK + "=" + LdapConstants.LDAP_TRUE + ")" +
               ")";
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
