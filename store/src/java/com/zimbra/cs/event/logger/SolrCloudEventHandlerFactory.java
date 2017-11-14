package com.zimbra.cs.event.logger;

import org.apache.solr.client.solrj.impl.CloudSolrClient;

import com.zimbra.cs.event.logger.BatchingEventLogger.BatchedEventCallback;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;

public class SolrCloudEventHandlerFactory extends SolrEventHandlerFactory {

    @Override
    protected BatchedEventCallback createCallback(String zkHost) {
        SolrCollectionLocator coreLocator = getCoreLocator();
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrRequestHelper requestHelper = new SolrCloudHelper(coreLocator, client, SolrConstants.CONFIGSET_EVENTS);
        return new SolrEventCallback(requestHelper);
    }
}