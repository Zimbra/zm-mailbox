package com.zimbra.cs.event.logger;

import org.apache.solr.client.solrj.impl.CloudSolrClient;

import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;


/**
 * Handler factory used for the "solrcloud" event logging backend
 */
public class SolrCloudEventHandlerFactory extends SolrEventHandlerFactory {

    @Override
    protected SolrRequestHelper getSolrRequestHelper(
            SolrCollectionLocator coreLocator, String zkHost) {
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        return new SolrCloudHelper(coreLocator, client, "events");
    }
}
