package com.zimbra.cs.index.solr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ShardParams;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.LuceneFields;

/**
 * Implementation used when all data for all accounts are stored in one index.
 * This locator adds the acct_id field to documents for filtering
 */
public class JointCollectionLocator extends SolrCollectionLocator {

    private String indexName;
    private IndexNameFunc nameFunc;

    public JointCollectionLocator(String indexName) {
        this.indexName = indexName;
    }

    public JointCollectionLocator(IndexNameFunc indexNameFunc) {
        this.nameFunc = indexNameFunc;
    }

    @Override
    public String getIndexName(String accountId) throws ServiceException {
        return indexName != null ? indexName : nameFunc.getIndexName(accountId);
    }

    @Override
    public void finalizeDoc(SolrInputDocument solrDoc, String accountId) {
        solrDoc.addField(LuceneFields.L_ACCOUNT_ID, accountId);
    }

    @Override
    boolean needsAccountFilter() {
        return true;
    }

    @Override
    public void finalizeQuery(SolrQuery query, String accountId) {
        query.addFilterQuery(SolrUtils.getAccountFilter(accountId));
        query.set(ShardParams._ROUTE_, String.format("%s!", accountId));
    }

    @Override
    String getSolrId(String accountId, Object... idParts) {
        return String.format("%s!%s", accountId, idJoiner.join(idParts));
    }

    public static interface IndexNameFunc {
        public String getIndexName(String accountId) throws ServiceException;
    }
}