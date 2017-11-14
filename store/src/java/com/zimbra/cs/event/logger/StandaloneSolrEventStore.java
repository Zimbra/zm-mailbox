package com.zimbra.cs.event.logger;

import org.apache.http.impl.client.CloseableHttpClient;

import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.StandaloneSolrHelper;

/**
 * Event store backed by a standalone Solr server. This class does not support
 * methods that rely on the Solr Streaming API.
 */
public class StandaloneSolrEventStore extends SolrEventStore {

    public StandaloneSolrEventStore(String accountId, StandaloneSolrHelper solrHelper) {
        super(accountId, solrHelper);
    }

    public static class Factory extends SolrEventStore.Factory {

        public Factory() throws ServiceException {
            super();
        }

        StandaloneSolrHelper solrHelper;

        @Override
        public EventStore getEventStore(String accountId) {
            return new StandaloneSolrEventStore(accountId, solrHelper);
        }

        @Override
        protected SolrRequestHelper getRequestHelper() throws ServiceException {
            CloseableHttpClient httpClient = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
            String baseUrl = server.getEventBackendURL().substring("solr:".length());
            return new StandaloneSolrHelper(getCollectionLocator(), httpClient, SolrConstants.CONFIGSET_EVENTS, baseUrl);
        }
    }
}
