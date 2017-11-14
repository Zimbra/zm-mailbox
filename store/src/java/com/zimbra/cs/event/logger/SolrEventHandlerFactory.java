package com.zimbra.cs.event.logger;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchingHandlerFactory;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrConstants;

public abstract class SolrEventHandlerFactory extends BatchingHandlerFactory {

    protected SolrCollectionLocator getCoreLocator() {
        SolrCollectionLocator locator;
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            switch(server.getEventSolrIndexType()) {
                case account:
                    locator = new AccountCollectionLocator(SolrConstants.EVENT_CORE_NAME_OR_PREFIX);
                    break;
                case combined:
                default:
                    locator = new JointCollectionLocator(SolrConstants.EVENT_CORE_NAME_OR_PREFIX);
                    break;

            }
        } catch (ServiceException e) {
            ZimbraLog.event.error("unable to determine event index type; defaulting to JointCollectionLocator", e);
            locator = new JointCollectionLocator(SolrConstants.EVENT_CORE_NAME_OR_PREFIX);
        }
        return locator;
    }
}
