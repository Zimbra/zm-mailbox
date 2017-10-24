package com.zimbra.cs.event.logger;

import org.apache.solr.client.solrj.impl.CloudSolrClient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchedEventCallback;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchingHandlerFactory;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;

public class SolrEventHandlerFactory extends BatchingHandlerFactory {

    public static final String CORE_NAME_OR_PREFIX = "events";

    protected SolrCollectionLocator getCoreLocator() {
        SolrCollectionLocator locator;
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            switch(server.getEventSolrIndexType()) {
                case account:
                    locator = new AccountCollectionLocator(CORE_NAME_OR_PREFIX);
                    break;
                case combined:
                default:
                    locator = new JointCollectionLocator(CORE_NAME_OR_PREFIX);
                    break;

            }
        } catch (ServiceException e) {
            ZimbraLog.event.error("unable to determine event index type; defaulting to JointCollectionLocator", e);
            locator = new JointCollectionLocator(CORE_NAME_OR_PREFIX);
        }
        return locator;
    }

    @Override
    protected BatchedEventCallback createCallback(String zkHost) {
        SolrCollectionLocator coreLocator = getCoreLocator();
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrRequestHelper requestHelper = new SolrCloudHelper(coreLocator, client, CORE_NAME_OR_PREFIX);
        return new SolrEventCallback(requestHelper);
    }
}
