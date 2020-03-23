package com.zimbra.cs.index.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.params.CoreAdminParams;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

public class SolrCloudHelper extends SolrRequestHelper {

    private CloudSolrClient cloudClient;
    private SolrClientCache clientCache;

    public SolrCloudHelper(SolrCollectionLocator locator, CloudSolrClient cloudClient) {
        super(locator);
        this.cloudClient = cloudClient;
        this.clientCache = new SolrClientCache();
    }

    @Override
    public void close() throws IOException {
        cloudClient.close();
        clientCache.close();
    }

    @Override
    public void deleteIndex(String accountId, IndexType indexType) throws ServiceException {
        SolrUtils.deleteCloudIndex(cloudClient, getCoreName(accountId, indexType));
    }

    @Override
    public void executeUpdateRequest(String accountId, UpdateRequest request, IndexType indexType)
            throws ServiceException {
        request.setParam(CoreAdminParams.COLLECTION, locator.getCollectionName(accountId, indexType));
        SolrUtils.executeCloudRequestWithRetry(accountId, cloudClient, request, locator.getCollectionName(accountId, indexType), indexType);
    }

    @Override
    public SolrResponse executeQueryRequest(String accountId, SolrQuery query, IndexType indexType) throws ServiceException {
        QueryRequest queryRequest = new QueryRequest(query, METHOD.POST);
        String collectionName = locator.getCollectionName(accountId, indexType);
        return SolrUtils.executeCloudRequestWithRetry(accountId, cloudClient, queryRequest, collectionName, indexType);
    }

    public String getZkHost() {
        return cloudClient.getZkHost();
    }

    public SolrClientCache getClientCache() {
        return clientCache;
    }
}