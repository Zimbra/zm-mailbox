package com.zimbra.cs.index.solr;

import java.io.Closeable;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.solr.SolrIndex.IndexType;

/**
 * Helper class for building and routing Solr requests. There are two degrees of freedom that
 * this accounts for:
 * 1) Whether the Solr backend is a standalone Solr server or a Solrcloud cluster
 * 2) Whether the request is Solr collection being referenced is a account-level collection
 * or one that stores data for all accounts in a single index
 */
public abstract class SolrRequestHelper implements Closeable {
    protected SolrCollectionLocator locator;
    protected IndexType indexType;

    public SolrRequestHelper(SolrCollectionLocator locator, IndexType indexType) {
        this.locator = locator;
        this.indexType = indexType;
    }

    public SolrQuery newQuery(String accountId) {
        SolrQuery query = new SolrQuery();
        locator.finalizeQuery(query, accountId);
        return query;
    }

    protected abstract void executeUpdateRequest(String accountId, UpdateRequest request) throws ServiceException;

    public void executeUpdate(String accountId, UpdateRequest request) throws ServiceException {
        if (request.getDocuments() != null) {
            for (SolrInputDocument doc: request.getDocuments()) {
                locator.finalizeDoc(doc, accountId);
            }
        }
        executeUpdateRequest(accountId, request);
    }

    public abstract SolrResponse executeQueryRequest(String accountId, SolrQuery query) throws ServiceException;

    public String getCoreName(String accountId) throws ServiceException {
        return locator.getIndexName(accountId);
    }

    public boolean needsAccountFilter() {
        return locator.needsAccountFilter();
    }

    public abstract void deleteIndex(String accountId) throws ServiceException;

    public void deleteAccountData(String accountId) throws ServiceException {
        if (needsAccountFilter()) {
            UpdateRequest req = new UpdateRequest();
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(new TermQuery(new Term(LuceneFields.L_ACCOUNT_ID, accountId)), Occur.MUST);
            req.deleteByQuery(builder.build().toString());
            executeUpdate(accountId, req);
        } else {
            deleteIndex(accountId);
        }
    }

    public String getSolrId(String accountId, Object... idParts) {
        return locator.getSolrId(accountId, idParts);
    }
}