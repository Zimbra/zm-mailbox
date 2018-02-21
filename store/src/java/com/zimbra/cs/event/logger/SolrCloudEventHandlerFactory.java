package com.zimbra.cs.event.logger;

import org.apache.solr.client.solrj.impl.CloudSolrClient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchedEventCallback;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrIndex.IndexType;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;

public class SolrCloudEventHandlerFactory extends SolrEventHandlerFactory {

    @Override
    protected BatchedEventCallback createCallback(String zkHost) throws ServiceException {
        SolrCollectionLocator coreLocator = getCoreLocator();
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrRequestHelper requestHelper = new SolrCloudHelper(coreLocator, client, IndexType.EVENTS);
        return new SolrEventCallback(requestHelper);
    }
}