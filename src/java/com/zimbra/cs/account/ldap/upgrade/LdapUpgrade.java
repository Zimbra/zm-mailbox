package com.zimbra.cs.account.ldap.upgrade;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.LdapClient;

public class LdapUpgrade {

    public static void main(String[] args) throws ServiceException {
        if (LdapClient.isLegacy()) {
            // delegate to the legacy package
            com.zimbra.cs.account.ldap.upgrade.legacy.LegacyLdapUpgrade.legacyMain(args);
        }
    }
}
