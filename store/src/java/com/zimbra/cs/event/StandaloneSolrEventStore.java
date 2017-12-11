package com.zimbra.cs.event;

import org.apache.http.impl.client.CloseableHttpClient;

import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.contacts.RelatedContactsParams;
import com.zimbra.cs.contacts.RelatedContactsResults;
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

    @Override
    public RelatedContactsResults getContactAffinity(RelatedContactsParams params) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    @Override
    public Double getPercentageOpenedEmails(String contact) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    @Override
    public Double getAvgTimeToOpenEmail(String contact) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    @Override
    public Double getAvgTimeToOpenEmailForAccount() throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    @Override
    public Double getPercentageRepliedEmails(String contact) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
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
