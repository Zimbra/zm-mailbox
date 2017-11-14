package com.zimbra.cs.index.solr;

import org.apache.solr.common.SolrInputDocument;

/**
 * CoreLocator implementation used when each account has a separate index
 */
public class AccountCollectionLocator extends SolrCollectionLocator {

    private String corePrefix;

    public AccountCollectionLocator(String corePrefix) {
        this.corePrefix = corePrefix;
    }

    @Override
    public String getCoreName(String accountId) {
        return String.format("%s_%s",corePrefix, accountId);
    }

    @Override
    public void finalizeDoc(SolrInputDocument solrDoc, String accountId) {
        //nothing to do here
    }

    @Override
    boolean needsAccountFilter() {
        return false;
    }
}