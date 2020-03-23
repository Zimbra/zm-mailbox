package com.zimbra.cs.event.logger;

import org.apache.solr.client.solrj.impl.CloudSolrClient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchedEventCallback;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;
import com.zimbra.cs.mailbox.MailboxIndex;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

public class SolrCloudEventHandlerFactory extends SolrEventHandlerFactory {

    @Override
    protected BatchedEventCallback createCallback(String zkHost) throws ServiceException {
        SolrCollectionLocator coreLocator = getCoreLocator();
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrRequestHelper requestHelper = new SolrCloudHelper(coreLocator, client, MailboxIndex.IndexType.EVENTS);
        return new SolrEventCallback(requestHelper);
    }
}