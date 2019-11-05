package com.zimbra.cs.event.logger;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchingHandlerFactory;
import com.zimbra.cs.index.solr.MultiCollectionLocator;
import com.zimbra.cs.index.solr.SolrCollectionLocator;

public abstract class SolrEventHandlerFactory extends BatchingHandlerFactory {

    protected SolrCollectionLocator getCoreLocator() throws ServiceException {
        return new MultiCollectionLocator();
    }
}
