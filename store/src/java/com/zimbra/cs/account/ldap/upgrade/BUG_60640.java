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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_60640 extends UpgradeOp {
    
    private static final String[] ATTR_NAMES = new String[] {
        Provisioning.A_zimbraPrefReadingPaneLocation,
        Provisioning.A_zimbraPrefTasksReadingPaneLocation,
        Provisioning.A_zimbraPrefBriefcaseReadingPaneLocation
    };
    
    private static final String OLD_VALUE = "bottom";
    private static final String NEW_VALUE = "right";

    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doAllCos(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    Description getDescription() {
        return new Description(this, 
                ATTR_NAMES, 
                new EntryType[] {EntryType.COS},
                OLD_VALUE, 
                NEW_VALUE, 
                String.format("Upgrade attribute %s on all cos from \"%s\" to \"%s\"", 
                        Arrays.deepToString(ATTR_NAMES), OLD_VALUE, NEW_VALUE));
    }
    
    private void doEntry(ZLdapContext zlc, Entry entry) throws ServiceException {
        String entryName = entry.getLabel();
        
        printer.println();
        printer.println("------------------------------");
        printer.format("Checking %s on cos %s\n", Arrays.deepToString(ATTR_NAMES), entryName);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (String attrName : ATTR_NAMES) {
            String curValue = entry.getAttr(attrName, false);
            if (OLD_VALUE.equals(curValue)) {
                attrs.put(attrName, NEW_VALUE);
            } else {
                printer.println(
                        String.format("    Current value of %s on cos %s is \"%s\" - not changed",
                                attrName, entryName, curValue));
            }
        }
        
        try {
            modifyAttrs(zlc, entry, attrs);
        } catch (ServiceException e) {
            printer.printStackTrace(e);
        }

    }
    
    private void doAllCos(ZLdapContext zlc) throws ServiceException {
        List<Cos> coses = prov.getAllCos();
        for (Cos cos : coses) {
            doEntry(zlc, cos);
        }
    }
}
