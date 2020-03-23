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
import com.zimbra.cs.mailbox.MailboxIndex;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

public class SolrCloudHelper extends SolrRequestHelper {

    private CloudSolrClient cloudClient;
    private SolrClientCache clientCache;

    public SolrCloudHelper(SolrCollectionLocator locator, CloudSolrClient cloudClient, MailboxIndex.IndexType indexType) {
        super(locator, indexType);
        this.cloudClient = cloudClient;
        this.clientCache = new SolrClientCache();
    }

    @Override
    public void close() throws IOException {
        cloudClient.close();
        clientCache.close();
    }

    @Override
    public void deleteIndex(String accountId) throws ServiceException {
        SolrUtils.deleteCloudIndex(cloudClient, getCoreName(accountId));
    }

    @Override
    public void executeUpdateRequest(String accountId, UpdateRequest request)
            throws ServiceException {
        request.setParam(CoreAdminParams.COLLECTION, locator.getCollectionName(accountId));
        SolrUtils.executeCloudRequestWithRetry(accountId, cloudClient, request, locator.getCollectionName(accountId), indexType);
    }

    @Override
    public SolrResponse executeQueryRequest(String accountId, SolrQuery query) throws ServiceException {
        QueryRequest queryRequest = new QueryRequest(query, METHOD.POST);
        String collectionName = locator.getCollectionName(accountId);
        return SolrUtils.executeCloudRequestWithRetry(accountId, cloudClient, queryRequest, collectionName, indexType);
    }

    public String getZkHost() {
        return cloudClient.getZkHost();
    }

    public SolrClientCache getClientCache() {
        return clientCache;
    }
}