package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;

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
    public void executeUpdateRequest(String accountId, UpdateRequest request, IndexType indexType) throws ServiceException {
        String collection = locator.getCollectionName(accountId, indexType);
        doRequest(request, cloudClient, collection);
    }

    @Override
    public SolrResponse executeQueryRequest(String accountId, SolrQuery query, Collection<IndexType> indexTypes) throws ServiceException {
        QueryRequest queryRequest = new QueryRequest(query, METHOD.POST);
        String collections = locator.getCollectionName(accountId, indexTypes);
        return doRequest(queryRequest, cloudClient, collections);
    }

    public String getZkHost() {
        return cloudClient.getZkHost();
    }

    public SolrClientCache getClientCache() {
        return clientCache;
    }

    private SolrResponse doRequest(SolrRequest request, SolrClient client, String collections) throws ServiceException {
        try {
            return request.process(client, collections);
        } catch (SolrServerException | IOException e) {
            throw ServiceException.FAILURE("search error", e);
        }
    }
}