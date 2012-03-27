/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
import java.util.Map;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Entry.EntryType;

public class BUG_72007 extends UpgradeOp {
    
    private static final String[] ATTRS = new String[] {
            Provisioning.A_zimbraIsSystemResource,
            Provisioning.A_zimbraIsSystemAccount};

    @Override
    void doUpgrade() throws ServiceException {
        
        Config config = prov.getConfig();
        upgrdeAcount(config.getAttr(Provisioning.A_zimbraSpamIsNotSpamAccount));
        upgrdeAcount(config.getAttr(Provisioning.A_zimbraSpamIsSpamAccount));
    }
    
    @Override
    Description getDescription() {
        return new Description(this, 
                ATTRS, 
                new EntryType[] {EntryType.ACCOUNT},
                null, 
                ProvisioningConstants.TRUE, 
                String.format("Set %s of %s and %s accounts to %s", 
                        Arrays.deepToString(ATTRS), 
                        Provisioning.A_zimbraSpamIsNotSpamAccount,
                        Provisioning.A_zimbraSpamIsSpamAccount,
                        ProvisioningConstants.TRUE));
    }
    
    private void upgrdeAcount(String name) throws ServiceException {
        if (name != null) {
            Account acct = prov.get(AccountBy.name, name);
            if (acct != null) {
                Map<String, Object> attrs = new HashMap<String, Object>();
                if (!acct.isIsSystemResource()) {
                    attrs.put(Provisioning.A_zimbraIsSystemResource, ProvisioningConstants.TRUE);
                }
                if (!acct.isIsSystemAccount()) {
                    attrs.put(Provisioning.A_zimbraIsSystemAccount, ProvisioningConstants.TRUE);
                }
                modifyAttrs(acct, attrs);
            }
        }
    }
}
