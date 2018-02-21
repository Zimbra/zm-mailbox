package com.zimbra.cs.index.solr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;

/**
 * Helper class that routes requests to the appropriate Solr index
 */
public abstract class SolrCollectionLocator {

    protected static final Joiner idJoiner = Joiner.on("_");

    /**
     * Return the name of the Solr collection used to store documents for the given account ID
     */
    abstract String getIndexName(String accountId) throws ServiceException;

    /**
     * Optionally post-process the Solr document appropriately
     */
    abstract void finalizeDoc(SolrInputDocument document, String accountId);

    /**
     * Whether this collection needs an account filter
     */
    abstract boolean needsAccountFilter();

    /**
     * Optionally post-process a query to make it appropriate for the Solr topology
     */
    abstract void finalizeQuery(SolrQuery query, String accountId);

    abstract String getSolrId(String accountId, Object... idParts);
}