package com.zimbra.cs.contacts;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.contacts.RelatedContactsParams.AffinityTarget;

/**
 * In-memory implementation of contact affinity results cache
 */
public class InMemoryResultsCache extends ResultsCache {

    private Cache<String, RelatedContactsResults> cache;

    public InMemoryResultsCache() {
        initCache();
    }

    private void initCache() {
        cache = CacheBuilder.newBuilder().build();
    }

    private String getTargetKeyComponent(AffinityTarget target) {
        return String.format("%s:%s", target.getAffinityType().name(), target.getContactEmail());
    }

    private String buildCacheKey(RelatedContactsParams params) {
        List<String> targets = params.getTargets().stream().map(t->getTargetKeyComponent(t)).collect(Collectors.toList());
        Collections.sort(targets);
        String targetStr = Joiner.on(",").join(targets);
        String affinityType = params.getRequestedAffinityType().name();
        String acctId = params.getAccountId();
        return Joiner.on("|").join(acctId, affinityType, targetStr);
    }

    @Override
    public RelatedContactsResults get(RelatedContactsParams params)
            throws ServiceException {
        String key = buildCacheKey(params);
        return cache.getIfPresent(key);
    }

    @Override
    public void storeInCache(RelatedContactsResults results) throws ServiceException {
        String key = buildCacheKey(results.getQueryParams());
        cache.put(key, results);
    }
}
