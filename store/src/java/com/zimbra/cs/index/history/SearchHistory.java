package com.zimbra.cs.index.history;

import java.util.List;

import com.google.common.base.Strings;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.SearchParams;

public abstract class SearchHistory {

    protected static Factory factory;

    static {
        try {
            setFactory(ZimbraSearchHistoryFactory.class);
        } catch (ServiceException e) {
            ZimbraLog.search.error("unable to set search history factory", e);
        }
    }
    public static final void setFactory(Class<? extends Factory> factoryClass) throws ServiceException {
        String className = factoryClass.getName();
        ZimbraLog.search.info("setting SearchHistory.Factory class %s", className);
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw ServiceException.FAILURE(String.format("unable to initialize SearchHistory factory %s", className), e);
        }
    }

    public static Factory getFactory() {
        return factory;
    }


    /**
     * Return a boolean representing whether the given search string already
     * exists in the search history for the provided mailbox
     */
    public abstract boolean contains(String searchQuery) throws ServiceException;


    /**
     * Return the number of times the given string has been searched
     */
    public abstract int getCount(String searchQuery) throws ServiceException;


    /**
     * Add a new search string entry to the history.
     * This must be done before an entry can be logged for the first time.
     * @throws ServiceException if an entry with the given ID already exists
     */
    public abstract void registerSearch(int queryId, String searchQuery) throws ServiceException;

    /**
     * Record a new instance for a search string already in the history
     */
    public abstract void logSearch(String search, long timestamp) throws ServiceException;

    /**
     * Return a list of search strings matching the the given parameters
     */
    public abstract List<String> search(SearchHistoryParams params)  throws ServiceException;

    /**
     * Remove search history entries older than maxAgeMillis
     */
    public abstract void purgeHistory(long maxAgeMillis) throws ServiceException;

    /**
     * Delete all search history for the given mailbox
     */
    public abstract void deleteHistory() throws ServiceException;

    /**
     * Return a SavedSearchPromptLog object for this search history
     */
    public abstract SavedSearchPromptLog getPromptLog() throws ServiceException;

    /**
     * Returns whether search history is enabled for the specified account
     */
    public static boolean featureEnabled(Account acct) throws ServiceException {
        return !LC.disable_all_search_history.booleanValue() && acct.isFeatureSearchHistoryEnabled();
    }

    /**
     * Returns a boolean representing whether this query string should be saved in the history.
     * This is used to ignore queries that were not explicitly issued by a user.
     * This is only necessary until we add the ability for the client to specify whether a query
     * should be saved in the history
     */
    public static boolean shouldSaveInHistory(SearchParams params) {
        String query = params.getQueryString();
        return params.getOffset() == 0
                && !query.startsWith("inid:")
                && !query.startsWith("in:");
    }

    public void logSearch(String searchString) throws ServiceException {
        logSearch(searchString, System.currentTimeMillis());
    }

    public interface Factory {
        public SearchHistory getSearchHistory(Account acct) throws ServiceException;
    }

    public static class SearchHistoryParams {

        private String prefix;
        private int numResults;

        public SearchHistoryParams() {
            this(0, null);
        }

        public SearchHistoryParams(int numResults, String prefix) {
            this.prefix = prefix;
            this.numResults = numResults;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public boolean hasPrefix() {
            return !Strings.isNullOrEmpty(prefix);
        }

        public String getPrefix() {
            return prefix;
        }

        public int getNumResults() {
            return numResults;
        }

        public void setNumResults(int num) {
            numResults = num;
        }
    }
}

