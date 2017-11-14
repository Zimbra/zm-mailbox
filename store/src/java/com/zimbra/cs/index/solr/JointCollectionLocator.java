package com.zimbra.cs.index.solr;

import org.apache.solr.common.SolrInputDocument;

import com.zimbra.cs.index.LuceneFields;

/**
 * CoreLocator implementation used when all events for all accounts are stored in one index.
 * This locator adds the acct_id field to documents
 */
public class JointCollectionLocator extends SolrCollectionLocator {

    private String coreName;

    public JointCollectionLocator(String coreName) {
        this.coreName = coreName;
    }

    @Override
    public String getCoreName(String accountId) {
        return coreName;
    }

    @Override
    public void finalizeDoc(SolrInputDocument solrDoc, String accountId) {
        solrDoc.addField(LuceneFields.L_ACCOUNT_ID, accountId);
    }

    @Override
    boolean needsAccountFilter() {
        return true;
    }
}