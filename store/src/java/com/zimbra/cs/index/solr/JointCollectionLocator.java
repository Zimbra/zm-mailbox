package com.zimbra.cs.index.solr;

import org.apache.solr.common.SolrInputDocument;

/**
 * CoreLocator implementation used when all events for all accounts are stored in one index.
 * This locator adds the acct_id field to documents
 */
public class JointCollectionLocator extends SolrCollectionLocator {

    private String coreName;
    private static final String ACCOUNT_ID_FIELD = "acct_id";

    public JointCollectionLocator(String coreName) {
        this.coreName = coreName;
    }

    @Override
    public String getCoreName(String accountId) {
        return coreName;
    }

    @Override
    public void finalizeDoc(SolrInputDocument solrDoc, String accountId) {
        solrDoc.addField(ACCOUNT_ID_FIELD, accountId);
    }

    @Override
    boolean needsAccountFilter() {
        return true;
    }
}