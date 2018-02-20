package com.zimbra.cs.event.logger;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchingHandlerFactory;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator.IndexNameFunc;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrUtils;

public abstract class SolrEventHandlerFactory extends BatchingHandlerFactory {

    protected SolrCollectionLocator getCoreLocator() throws ServiceException {

        IndexNameFunc indexNameFunc = new IndexNameFunc() {
            @Override
            public String getIndexName(String accountId) throws ServiceException {
                return SolrUtils.getEventIndexName(accountId);
            }
        };

        return new JointCollectionLocator(indexNameFunc);
    }
}
