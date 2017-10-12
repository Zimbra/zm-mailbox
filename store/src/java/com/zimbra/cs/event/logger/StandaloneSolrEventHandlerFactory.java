package com.zimbra.cs.event.logger;

import org.apache.http.impl.client.CloseableHttpClient;

import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.StandaloneSolrHelper;


/**
 * Handler factory used for the "solr" event logging backend
 */
public class StandaloneSolrEventHandlerFactory extends SolrEventHandlerFactory {

    @Override
    protected SolrRequestHelper getSolrRequestHelper(SolrCollectionLocator coreLocator, String solrUrl) {
        CloseableHttpClient client = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
        return new StandaloneSolrHelper(coreLocator, client, "events", solrUrl);
    }
}
