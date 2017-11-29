package com.zimbra.cs.contacts;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.EventStore;

public class ZimbraContactAffinityFactory extends ContactAffinityStore.Factory {

    private InMemoryResultsCache cacheInstance;

    @Override
    protected ComputationBackend getComputationBackend(String accountId) throws ServiceException {
        EventStore eventStore = EventStore.getFactory().getEventStore(accountId);
        return new EventContactAffinityComputationBackend(eventStore);
    }

    @Override
    protected ResultsCache getResultsCache(String accountId) throws ServiceException {
        if (cacheInstance == null) {
            synchronized (this) {
                cacheInstance = new InMemoryResultsCache();
            }
        }
        return cacheInstance;
    }
}
