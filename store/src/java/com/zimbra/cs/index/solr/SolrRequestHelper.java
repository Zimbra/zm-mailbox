package com.zimbra.cs.index.solr;

import java.io.Closeable;

import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;

import com.zimbra.common.service.ServiceException;

/**
 * Helper class for building and routing Solr requests. There are two degrees of freedom that
 * this accounts for:
 * 1) Whether the Solr backend is a standalone Solr server or a Solrcloud cluster
 * 2) Whether the request is Solr collection being referenced is a account-level collection
 * or one that stores data for all accounts in a single index
 */
public abstract class SolrRequestHelper implements Closeable {
    protected SolrCollectionLocator locator;
    protected String configSet;

    public SolrRequestHelper(SolrCollectionLocator locator, String configSet) {
        this.locator = locator;
        this.configSet = configSet;
    }

    public abstract UpdateRequest newRequest(String accountId);

    protected abstract void executeRequest(String accountId, UpdateRequest request) throws ServiceException;

    public void execute(String accountId, UpdateRequest request) throws ServiceException {
        if (request.getDocuments() != null) {
            for (SolrInputDocument doc: request.getDocuments()) {
                locator.finalizeDoc(doc, accountId);
            }
        }
        executeRequest(accountId, request);
    }

    public String getCoreName(String accountId) {
        return locator.getCoreName(accountId);
    }

    public boolean needsAccountFilter() {
        return locator.needsAccountFilter();
    }

    public abstract void deleteIndex(String accountId) throws ServiceException;
}