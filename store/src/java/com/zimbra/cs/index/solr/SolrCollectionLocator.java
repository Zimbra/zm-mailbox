package com.zimbra.cs.index.solr;

import java.util.Collection;
import java.util.EnumSet;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.solr.SolrIndex.OpType;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

/**
 * Helper class that routes requests to the appropriate Solr index
 */
public abstract class SolrCollectionLocator {

    protected static final Joiner idJoiner = Joiner.on("_");

    public String getCollectionName(String accountId, IndexType indexType) throws ServiceException {
        return getCollectionName(accountId, indexType, OpType.WRITE);
    }

    public String getCollectionName(String accountId, IndexType indexType, OpType opType) throws ServiceException {
        return getCollectionName(accountId, EnumSet.of(indexType), opType);
    }

    /**
     * Return the name of the Solr collection used to store documents for the given account ID
     */
    abstract String getCollectionName(String accountId, Collection<IndexType> indexTypes, OpType opType) throws ServiceException;

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