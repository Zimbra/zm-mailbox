package com.zimbra.cs.event.logger;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchedEventCallback;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchingHandlerFactory;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrRequestHelper;


/**
 * Abstract handler factory for Solr event logging
 */
public abstract class SolrEventHandlerFactory extends BatchingHandlerFactory {

    public static final String CORE_NAME_OR_PREFIX = "events";

    protected SolrCollectionLocator getCoreLocator() {
        SolrCollectionLocator locator;
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            switch(server.getEventSolrIndexType()) {
                case account:
                    locator = new AccountCollectionLocator("events");
                    break;
                case combined:
                default:
                    locator = new JointCollectionLocator("events");
                    break;

            }
        } catch (ServiceException e) {
            ZimbraLog.event.error("unable to determine event index type; defaulting to JointCoreLocator", e);
            locator = new JointCollectionLocator("events");
        }
        return locator;
    }

    @Override
    protected BatchedEventCallback createCallback(String solrUrl) {
        SolrCollectionLocator coreLocator = getCoreLocator();
        SolrRequestHelper requestHelper = getSolrRequestHelper(coreLocator, solrUrl);
        return new SolrEventCallback(requestHelper);
    }

    protected abstract SolrRequestHelper getSolrRequestHelper(SolrCollectionLocator coreLocator, String solrUrl);

    public static void registerSolrFactories() {
        ZimbraLog.event.info("registering Solr/SolrCloud EventLogHandlers");
        EventLogger.registerHandlerFactory("solr", new StandaloneSolrEventHandlerFactory());
        EventLogger.registerHandlerFactory("solrcloud", new SolrCloudEventHandlerFactory());
    }
}
