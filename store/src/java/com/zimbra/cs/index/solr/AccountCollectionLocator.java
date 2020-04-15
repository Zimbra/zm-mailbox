package com.zimbra.cs.index.solr;

import java.util.Collection;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;

import com.zimbra.cs.index.solr.SolrIndex.OpType;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

/**
 * CoreLocator implementation used when each account has a separate index
 */
public class AccountCollectionLocator extends SolrCollectionLocator {

    private String corePrefix;

    public AccountCollectionLocator(String corePrefix) {
        this.corePrefix = corePrefix;
    }

    @Override
    public String getCollectionName(String accountId, Collection<IndexType> indexTypes, OpType opType) {
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

    @Override
    void finalizeQuery(SolrQuery query, String accountId) {
        // no query post-processing is needed when each account has its own collection
    }

    @Override
    String getSolrId(String accountId, Object... idParts) {
        return idJoiner.join(idParts);
    }
}