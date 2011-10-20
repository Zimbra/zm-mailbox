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

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.Key.CosBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_63722 extends UpgradeOp {

    private static final String ATTR_NAME = Provisioning.A_zimbraPrefGalAutoCompleteEnabled;
    private static final String OLD_VALUE = ProvisioningConstants.FALSE;
    private static final String NEW_VALUE = ProvisioningConstants.TRUE;
    
    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doDefaultCos(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    @Override
    Description getDescription() {
        return new Description(this, 
                new String[] {ATTR_NAME}, 
                new EntryType[] {EntryType.COS},
                OLD_VALUE, 
                NEW_VALUE, 
                "Upgrades only the default cos.");
    }
    
    private void doEntry(ZLdapContext zlc, Entry entry) throws ServiceException {
        String entryName = entry.getLabel();
        
        printer.println();
        printer.println("------------------------------");
        printer.println("Checking " + ATTR_NAME + " on " + entryName);
        
        String curValue = entry.getAttr(ATTR_NAME, false);
        if (OLD_VALUE.equals(curValue)) {
            printer.println(
                    "    Changing " + ATTR_NAME + " on " + entryName + " from " + curValue + " to " + NEW_VALUE);

            Map<String, Object> attr = new HashMap<String, Object>();
            attr.put(ATTR_NAME, NEW_VALUE);
            try {
                modifyAttrs(zlc, entry, attr);
            } catch (ServiceException e) {
                // log the exception and continue
                printer.println("Caught ServiceException while modifying " + entryName + " attribute " + attr);
                printer.printStackTrace(e);
            }
        } else {
            printer.println(
                    "    Current value of " + ATTR_NAME + " on " + entryName + " is " + curValue + " - not changed");
        }
    }
    
    private void doDefaultCos(ZLdapContext zlc) throws ServiceException {
        Cos cos = prov.get(CosBy.name, Provisioning.DEFAULT_COS_NAME);
        if (cos != null) {
            doEntry(zlc, cos);
        }
    }
}
