/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.ldap.ZMutableEntry;

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

        ZLdapFilter homeServerPresent = filterFactory.fromFilterString(
                FilterId.LDAP_UPGRADE, filterFactory.presenceFilter(ATTR_NAME));
        ZLdapFilter homeServerNotPresent =
            filterFactory.negate(homeServerPresent);

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
        private final UpgradeOp upgradeOp;
        private final ZLdapContext modZlc;
        private final List<String> servers = Lists.newArrayList();
        private int index = 0; // index into the List for picking a server in a round robin manner

        Bug68831Visitor(UpgradeOp upgradeOp, ZLdapContext modZlc) throws ServiceException {
            super(false);
            this.upgradeOp = upgradeOp;
            this.modZlc = modZlc;

            for (Server server: upgradeOp.prov.getAllServers()) {
                String serviceHostname = server.getServiceHostname();
                if (server.hasMailClientService() && serviceHostname != null) {
                    servers.add(serviceHostname);
                }
            }

            if (servers.size() == 0) {
                throw ServiceException.FAILURE("no mailclient server running service for assigning home server for distribution lists", null);
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
