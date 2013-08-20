/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
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
