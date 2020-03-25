package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.util.Collection;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

public class StandaloneSolrHelper extends SolrRequestHelper {

    private String baseUrl;
    private CloseableHttpClient httpClient;

    public StandaloneSolrHelper(SolrCollectionLocator locator, CloseableHttpClient httpClient, String baseUrl) {
        super(locator);
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public void close() throws IOException {
        //no need to close CloseableHttpClient
    }

    @Override
    protected void executeUpdateRequest(String accountId, UpdateRequest request, IndexType indexType) throws ServiceException {
        String coreName = locator.getCollectionName(accountId, indexType);
        try(SolrClient solrClient = SolrUtils.getSolrClient(httpClient, baseUrl, coreName)) {
            doRequest(accountId, request, solrClient);
        } catch (IOException e) {
            throw ServiceException.FAILURE(String.format("unable to execute Solr request for account %s", accountId), e);
        }
    }

    @Override
    public SolrResponse executeQueryRequest(String accountId, SolrQuery query, Collection<IndexType> indexTypes) throws ServiceException {
        String coreName = locator.getCollectionName(accountId, indexTypes);
        try(SolrClient solrClient = SolrUtils.getSolrClient(httpClient, baseUrl, coreName)) {
            QueryRequest queryRequest = new QueryRequest(query, METHOD.POST);
            return doRequest(accountId, queryRequest, solrClient);
        } catch (IOException e) {
            throw ServiceException.FAILURE(String.format("unable to execute Solr request for account %s", accountId), e);
        }
    }

    @Override
    public void deleteIndex(String accountId, IndexType indexType) throws ServiceException {
        String coreName = locator.getCollectionName(accountId, indexType);
        try(SolrClient solrClient = SolrUtils.getSolrClient(httpClient, baseUrl, coreName)) {
            SolrUtils.deleteStandaloneIndex(solrClient, baseUrl, coreName);
        } catch (IOException e) {
            throw ServiceException.FAILURE(String.format("unable to execute Solr request for account %s", accountId), e);
        }
    }

    private SolrResponse doRequest(String accountId, SolrRequest request, SolrClient client) throws ServiceException {
        try {
            return request.process(client);
        } catch (SolrServerException | IOException e) {
            throw ServiceException.FAILURE(String.format("unable to execute Solr request for account %s", accountId), e);
        }
    }
}