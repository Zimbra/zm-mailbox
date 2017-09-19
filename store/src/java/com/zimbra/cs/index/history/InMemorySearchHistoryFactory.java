package com.zimbra.cs.index.history;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryConfig;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryIndex;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryMetadata;

public class InMemorySearchHistoryFactory extends ZimbraSearchHistory.Factory {

    private Map<String, SearchHistoryIndex> indexCache = new HashMap<String, SearchHistoryIndex>();
    private Map<String, SearchHistoryMetadata> mdCache = new HashMap<String, SearchHistoryMetadata>();
    private Map<String, SavedSearchPromptLog> promptLogCache = new HashMap<String, SavedSearchPromptLog>();
    private static InMemorySearchHistoryConfig configInstance = new InMemorySearchHistoryConfig(0);

    public static void setMaxAge(long maxAge) {
        InMemorySearchHistoryFactory.configInstance.setMaxAge(maxAge);
    }

    @Override
    public SearchHistoryIndex getIndex(Account acct) {
        String key = acct.getId();
        SearchHistoryIndex index = indexCache.get(key);
        if (index == null) {
            index = new InMemorySearchHistoryIndex();
            indexCache.put(key, index);
        }
        return index;
    }

    @Override
    public SearchHistoryMetadata getMetadataStore(Account acct) {
        String key = acct.getId();
        SearchHistoryMetadata mdStore = mdCache.get(key);
        if (mdStore == null) {
            mdStore = new InMemorySearchHistoryMetadata();
            mdCache.put(key, mdStore);
        }
        return mdStore;
    }

    @Override
    public SearchHistoryConfig getConfig(Account acct) {
        return configInstance;

    }

    @Override
    public SavedSearchPromptLog getPromptLog(Account acct) {
        String key = acct.getId();
        SavedSearchPromptLog promptLog = promptLogCache.get(key);
        if (promptLog == null) {
            promptLog = new InMemorySavedSearchPromptLog();
            promptLogCache.put(key, promptLog);
        }
        return promptLog;
    }
}
