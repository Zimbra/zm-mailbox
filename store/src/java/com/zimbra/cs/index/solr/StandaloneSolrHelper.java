package com.zimbra.cs.index.solr;

import java.io.IOException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.solr.SolrIndex.IndexType;

public class StandaloneSolrHelper extends SolrRequestHelper {

    private String baseUrl;
    private CloseableHttpClient httpClient;

    public StandaloneSolrHelper(SolrCollectionLocator locator, CloseableHttpClient httpClient, IndexType indexType, String baseUrl) {
        super(locator, indexType);
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public void close() throws IOException {
        //no need to close CloseableHttpClient
    }

    @Override
    protected void executeUpdateRequest(String accountId, UpdateRequest request) throws ServiceException {
        String coreName = locator.getIndexName(accountId);
        try(SolrClient solrClient = SolrUtils.getSolrClient(httpClient, baseUrl, coreName)) {
            SolrUtils.executeRequestWithRetry(accountId, solrClient, request, baseUrl, coreName, indexType);
        } catch (IOException e) {
            throw ServiceException.FAILURE(String.format("unable to execute Solr request for account %s", accountId), e);
        }
    }

    @Override
    public SolrResponse executeQueryRequest(String accountId, SolrQuery query) throws ServiceException {
        String coreName = locator.getIndexName(accountId);
        try(SolrClient solrClient = SolrUtils.getSolrClient(httpClient, baseUrl, coreName)) {
            return SolrUtils.executeRequestWithRetry(accountId, solrClient, new QueryRequest(query), baseUrl, coreName, indexType);
        } catch (IOException e) {
            throw ServiceException.FAILURE(String.format("unable to execute Solr request for account %s", accountId), e);
        }
    }

    @Override
    public void deleteIndex(String accountId) throws ServiceException {
        String coreName = locator.getIndexName(accountId);
        try(SolrClient solrClient = SolrUtils.getSolrClient(httpClient, baseUrl, coreName)) {
            SolrUtils.deleteStandaloneIndex(solrClient, baseUrl, coreName);
        } catch (IOException e) {
            throw ServiceException.FAILURE(String.format("unable to execute Solr request for account %s", accountId), e);
        }

    }
}