package com.zimbra.cs.contacts;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.EventStore;

/**
 * Backend that uses the Solrcloud streaming API to calculate contact affinity from event data
 */
public class EventContactAffinityComputationBackend implements ComputationBackend {

    EventStore eventStore;

    public EventContactAffinityComputationBackend(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public RelatedContactsResults calculateContactAffinity(RelatedContactsParams query)
            throws ServiceException {
        return eventStore.getContactAffinity(query);
    }
}
