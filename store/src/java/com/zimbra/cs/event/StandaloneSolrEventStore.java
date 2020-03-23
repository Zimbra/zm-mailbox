package com.zimbra.cs.event;

import org.apache.http.impl.client.CloseableHttpClient;

import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.contacts.RelatedContactsParams;
import com.zimbra.cs.contacts.RelatedContactsResults;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.analytics.RatioMetric;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.StandaloneSolrHelper;
import com.zimbra.cs.mailbox.MailboxIndex;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

/**
 * Event store backed by a standalone Solr server. This class does not support
 * methods that rely on the Solr Streaming API.
 */
public class StandaloneSolrEventStore extends SolrEventStore {

    public StandaloneSolrEventStore(String accountId, StandaloneSolrHelper solrHelper) {
        super(accountId, solrHelper);
    }

    @Override
    public RelatedContactsResults getContactAffinity(RelatedContactsParams params) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    @Override
    public RatioMetric getEventTimeDelta(EventType firstEventType, EventType secondEventType, String contact) throws ServiceException {
        //event time deltas are calculated with the Solr Streaming API, which is not available on Standalone Solr
        throw ServiceException.UNSUPPORTED();
    }

    public static class Factory extends SolrEventStore.Factory {

        public Factory() throws ServiceException {
            super();
        }

        @Override
        public EventStore getEventStore(String accountId) {
            return new StandaloneSolrEventStore(accountId, (StandaloneSolrHelper) solrHelper);
        }

        @Override
        protected SolrRequestHelper getRequestHelper() throws ServiceException {
            CloseableHttpClient httpClient = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
            String baseUrl = server.getEventBackendURL().substring("solr:".length());
            return new StandaloneSolrHelper(getCollectionLocator(), httpClient, MailboxIndex.IndexType.EVENTS, baseUrl);
        }
    }
}
