package com.zimbra.cs.index.history;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;

public class LdapSearchHistoryConfig implements ZimbraSearchHistory.SearchHistoryConfig {

    private Account acct;

    public LdapSearchHistoryConfig(Account acct) {
        this.acct = acct;
    }

    @Override
    public long getMaxAge() throws ServiceException {
        return acct.getSearchHistoryAge();
    }
}
