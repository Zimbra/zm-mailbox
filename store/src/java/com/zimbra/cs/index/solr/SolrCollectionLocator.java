package com.zimbra.cs.index.solr;

import org.apache.solr.common.SolrInputDocument;

/**
 * Helper class that routes requests to the appropriate Solr index
 */
public abstract class SolrCollectionLocator {

    /**
     * Return the name of the Solr collection used to store documents for the given account ID
     */
    abstract String getCoreName(String accountId);

    /**
     * Optionally post-process the Solr document appropriately
     */
    abstract void finalizeDoc(SolrInputDocument document, String accountId);

    /**
     * Whether this collection needs an account filter
     */
    abstract boolean needsAccountFilter();
}