/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.ldap;

import java.util.Set;

import com.zimbra.common.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.soap.type.AutoProvPrincipalBy;

public class AutoProvisionManual extends AutoProvision {

    private AutoProvPrincipalBy by;
    private String principal;
    private String password;
    
    protected AutoProvisionManual(LdapProv prov, Domain domain, 
            AutoProvPrincipalBy by, String principal, String password) {
        super(prov, domain);
        this.by = by;
        this.principal = principal;
        this.password = password;
    }

    @Override
    Account handle() throws ServiceException {
        if (!autoProvisionEnabled()) {
            throw ServiceException.FAILURE("MANUAL auto provision is not enabled on domain " 
                    + domain.getName(), null);
        }
        
        return createAccount();
    }
    
    private boolean autoProvisionEnabled() {
        Set<String> modesEnabled = domain.getMultiAttrSet(Provisioning.A_zimbraAutoProvMode);
        return modesEnabled.contains(AutoProvMode.MANUAL.name());
    }
    
    private Account createAccount() throws ServiceException {
        String acctZimbraName;
        ExternalEntry externalEntry;
        if (by == AutoProvPrincipalBy.dn) {
            ZAttributes externalAttrs = getExternalAttrsByDn(principal);
            externalEntry = new ExternalEntry(principal, externalAttrs);
            acctZimbraName = mapName(externalAttrs, null);
        } else if (by == AutoProvPrincipalBy.name) {
            externalEntry = getExternalAttrsByName(principal);
            acctZimbraName = mapName(externalEntry.getAttrs(), principal);
        } else {
            throw ServiceException.FAILURE("unknown AutoProvPrincipalBy", null);
        }

        ZimbraLog.autoprov.info("auto creating account in MANUAL mode: " + acctZimbraName);
        return createAccount(acctZimbraName, externalEntry, password, AutoProvMode.MANUAL);
    }

}
