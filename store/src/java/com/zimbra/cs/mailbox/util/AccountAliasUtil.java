package com.zimbra.cs.mailbox.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;

public class AccountAliasUtil {

    private final LdapProvisioning ldapProvisioning;

    public AccountAliasUtil(LdapProvisioning prov) {
        this.ldapProvisioning = prov;
    }

    public void addAliasHidingInfo(Account account, String alias) throws ServiceException {
        ldapProvisioning.addAliasHidingDetails(account, alias);
    }
}
