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
