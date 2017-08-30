package com.zimbra.cs.index.history;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryConfig;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryIndex;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryMetadata;

public class ZimbraSearchHistoryFactory extends ZimbraSearchHistory.Factory {

    private Map<String, SearchHistoryIndex> indexCache = new HashMap<String, SearchHistoryIndex>();

    @Override
    public SearchHistoryIndex getIndex(Account acct) throws ServiceException {
        String key = acct.getId();
        SearchHistoryIndex index = indexCache.get(key);
        if (index == null) {
            index = new InMemorySearchHistoryIndex();
            indexCache.put(key, index);
        }
        return index;
    }

    @Override
    public SearchHistoryMetadata getMetadataStore(Account acct) throws ServiceException {
        return new DbSearchHistoryMetadata(acct);
    }

    @Override
    public SearchHistoryConfig getConfig(Account acct) throws ServiceException {
        return new LdapSearchHistoryConfig(acct);
    }

    @Override
    public SavedSearchPromptLog getPromptLog(Account acct) throws ServiceException {
        return new DbSavedSearchPromptLog(acct);
    }
}
