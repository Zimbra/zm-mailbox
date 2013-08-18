/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
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
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_68394 extends UpgradeOp {

    private static final String ATTR_NAME = Provisioning.A_zimbraMailSSLClientCertPort;
    private static final String OLD_VALUE = "0";
    private static final String NEW_VALUE = "9443";

    @Override
    void doUpgrade() throws ServiceException {

        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doGlobalConfig(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    Description getDescription() {
        return new Description(this,
                new String[] {ATTR_NAME},
                new EntryType[] {EntryType.GLOBALCONFIG},
                OLD_VALUE,
                NEW_VALUE,
                String.format("Upgrade attribute %s on global config from \"%s\" to \"%s\"",
                        ATTR_NAME, OLD_VALUE, NEW_VALUE));
    }

    private void doGlobalConfig(ZLdapContext zlc) throws ServiceException {
        Config config = prov.getConfig();
        String curValue = config.getAttr(ATTR_NAME);
        if (OLD_VALUE.equals(curValue)) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(ATTR_NAME, NEW_VALUE);
            modifyAttrs(config, attrs);
        }
    }
}
