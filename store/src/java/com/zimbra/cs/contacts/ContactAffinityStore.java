package com.zimbra.cs.contacts;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.contacts.RelatedContactsResults.RelatedContact;

/**
 * Entry point into the contact affinity system. Delegates to ComputationBackend
 * to calculate contact affinity and stores/retrieves results with ResultsCache.
 */
public class ContactAffinityStore {

    private static Factory factory;

    private String accountId;
    private ComputationBackend compBackend;
    private ResultsCache resultsCache;

    public ContactAffinityStore(String accountId, ComputationBackend compBackend,
            ResultsCache resultsCache) {
        this.accountId = accountId;
        this.compBackend = compBackend;
        this.resultsCache = resultsCache;
    }

    public static final void setFactory(Class<? extends Factory> factoryClass) throws ServiceException {
        String className = factoryClass.getName();
        ZimbraLog.search.info("setting ContactAffinityStore.Factory class %s", className);
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw ServiceException.FAILURE(String.format("unable to initialize ContactAffinityStore factory %s", className), e);
        }
    }

    public static Factory getFactory() {
        return factory;
    }

    private RelatedContactsResults applyLimit(RelatedContactsParams params, RelatedContactsResults results) {
        int limit = params.getLimit();
        List<RelatedContact> contactList = results.getResults();
        if (limit >= contactList.size()) {
            return results;
        }
        List<RelatedContact> requestedResultsRange = contactList.subList(0, limit);
        RelatedContactsResults limitedResults = new RelatedContactsResults(params, results.getDate());
        limitedResults.addResults(requestedResultsRange);
        return limitedResults;
    }

    public RelatedContactsResults getRelatedContacts(RelatedContactsParams params) throws ServiceException {

        RelatedContactsResults results = getFromCache(params);
        if (results == null) {
            //no cached results, so compute affinity. This may return more results than is
            //requested, which is OK, since we will trim to the requested number.
            results = calculateRelatedContacts(params);
        } else if (params.getLimit() > results.size() && results.maybeHasMore()) {
            //cached results don't satisfy the requested limit. If a wider scope is possible, compute again
            results = calculateRelatedContacts(params);
        }
        return applyLimit(params, results);
    }

    private RelatedContactsResults calculateRelatedContacts(RelatedContactsParams params) throws ServiceException {
        RelatedContactsResults results = compBackend.calculateContactAffinity(params);
        storeInCache(results);
        return results;
    }

    private RelatedContactsResults getFromCache(RelatedContactsParams params) throws ServiceException {
        if (resultsCache != null) {
            return resultsCache.getFromCache(params);
        } else {
            return null;
        }
    }

    private void storeInCache(RelatedContactsResults relatedContacts) throws ServiceException {
        if (resultsCache != null) {
            resultsCache.storeInCache(relatedContacts);
        }
    }

    public static abstract class Factory {

        /**
         * return an instance of ComputationBackend to be used for calculating contact affinity
         */
        protected abstract ComputationBackend getComputationBackend(String accountId) throws ServiceException;

        /**
         * return an instance of ResultsCache to be used for storing/querying known results
         */
        protected abstract ResultsCache getResultsCache(String accountId) throws ServiceException;

        /**
         * return an instance of ContactAffinityStore for the given account ID
         */
        public ContactAffinityStore getContactAffinityStore(String accountId) throws ServiceException{
            ComputationBackend backend = getComputationBackend(accountId);
            ResultsCache cache = getResultsCache(accountId);
            return new ContactAffinityStore(accountId, backend, cache);
        }
    }
}
