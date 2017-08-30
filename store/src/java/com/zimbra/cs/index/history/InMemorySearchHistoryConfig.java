package com.zimbra.cs.index.history;

import com.zimbra.common.service.ServiceException;

/**
 * In-memory implementation of the config provider that bypasses LDAP.
 * Used for testing and debugging.
 * @author iraykin
 *
 */
public class InMemorySearchHistoryConfig implements ZimbraSearchHistory.SearchHistoryConfig {

    private long maxAge;

    public InMemorySearchHistoryConfig(long maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public long getMaxAge() throws ServiceException {
        return maxAge;
    }

    void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

}
