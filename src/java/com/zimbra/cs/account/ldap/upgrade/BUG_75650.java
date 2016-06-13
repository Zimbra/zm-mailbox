/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_75650 extends UpgradeOp {

    private static final String ATTR_NAME = Provisioning.A_zimbraMailPurgeBatchSize;
    private static final int OLD_VALUE = 10000;
    private static final int NEW_VALUE = 1000;

    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doGlobalConfig(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    private void doGlobalConfig(ZLdapContext zlc) throws ServiceException {
        doEntry(zlc, prov.getConfig());
    }

    private void doEntry(ZLdapContext zlc, Entry entry) throws ServiceException {
        String entryName = entry.getLabel();

        printer.println();
        printer.println("------------------------------");
        printer.println("Checking " + ATTR_NAME + " on " + entryName);

        int curValue = entry.getIntAttr(ATTR_NAME, NEW_VALUE);
        if (curValue == OLD_VALUE) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            printer.println("Changing " + ATTR_NAME + " on " + entryName + " from " + OLD_VALUE + " to " + NEW_VALUE);
            attrs.put(Provisioning.A_zimbraMailPurgeBatchSize, NEW_VALUE);
            modifyAttrs(entry, attrs);
        } else {
            printer.println("Current value of " + ATTR_NAME + " on " + entryName + " is " + curValue + " - not changed");
        }
    }
}
