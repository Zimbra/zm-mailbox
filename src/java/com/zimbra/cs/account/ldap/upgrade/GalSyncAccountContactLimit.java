package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;

public class GalSyncAccountContactLimit extends LdapUpgrade {

    GalSyncAccountContactLimit() throws ServiceException {
    }

    @Override
    void doUpgrade() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        HashMap<String,String> map = new HashMap<String,String>();
        map.put(Provisioning.A_zimbraContactMaxNumEntries, "0");
        for (Domain domain : prov.getAllDomains()) {
            System.out.println("Checking domain "+domain.getName());
            String[] accountIds = domain.getMultiAttr(Provisioning.A_zimbraGalAccountId);
            if (accountIds != null && accountIds.length > 0) {
                for (String accountId : accountIds) {
                    Account a = prov.getAccountById(accountId);
                    if (a != null) {
                        System.out.println("Modifying account "+a.getName());
                        prov.modifyAttrs(a, map);
                    }
                }
            }
        }
    }
}
