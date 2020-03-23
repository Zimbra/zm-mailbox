package com.zimbra.cs.event.logger;

import org.apache.http.impl.client.CloseableHttpClient;

import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchedEventCallback;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.StandaloneSolrHelper;
import com.zimbra.cs.mailbox.MailboxIndex;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

public class StandaloneSolrEventHandlerFactory extends SolrEventHandlerFactory {

    @Override
    protected BatchedEventCallback createCallback(String baseUrl) throws ServiceException {
        SolrCollectionLocator coreLocator = getCoreLocator();
        CloseableHttpClient httpClient = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
        SolrRequestHelper requestHelper = new StandaloneSolrHelper(coreLocator, httpClient, MailboxIndex.IndexType.EVENTS, baseUrl);
        return new SolrEventCallback(requestHelper);
    }
}