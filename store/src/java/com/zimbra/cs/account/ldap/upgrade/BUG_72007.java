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
        upgrdeAccount(config.getAttr(Provisioning.A_zimbraSpamIsNotSpamAccount));
        upgrdeAccount(config.getAttr(Provisioning.A_zimbraSpamIsSpamAccount));
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
    
    private void upgrdeAccount(String name) throws ServiceException {
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
