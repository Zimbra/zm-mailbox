package com.zimbra.cs.event;

import org.apache.solr.client.solrj.impl.CloudSolrClient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;

public class SolrCloudEventStore extends SolrEventStore {

    public SolrCloudEventStore(String accountId, SolrCloudHelper solrHelper) {
        super(accountId, solrHelper);
    }

    public static class Factory extends SolrEventStore.Factory {

        private CloudSolrClient client;
        SolrCloudHelper solrHelper;

        public Factory() throws ServiceException {
            super();
        }

        @Override
        public EventStore getEventStore(String accountId) {
            return new SolrCloudEventStore(accountId, solrHelper);
        }

        @Override
        protected SolrRequestHelper getRequestHelper() throws ServiceException {
            String zkHost = server.getEventBackendURL().substring("solrcloud:".length());
            client = SolrUtils.getCloudSolrClient(zkHost);
            return new SolrCloudHelper(getCollectionLocator(), client, SolrConstants.CONFIGSET_EVENTS);
        }
    }
}
