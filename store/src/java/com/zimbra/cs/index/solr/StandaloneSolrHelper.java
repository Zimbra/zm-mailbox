package com.zimbra.cs.index.solr;

import java.io.IOException;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;

import com.zimbra.common.service.ServiceException;

public class StandaloneSolrHelper extends SolrRequestHelper {

    private String baseUrl;
    private CloseableHttpClient httpClient;

    public StandaloneSolrHelper(SolrCollectionLocator locator, CloseableHttpClient httpClient,
            String configSet, String baseUrl) {
        super(locator, configSet);
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public void close() throws IOException {
        //no need to close CloseableHttpClient
    }

    @Override
    protected void executeRequest(String accountId, UpdateRequest request) throws ServiceException {
        String coreName = locator.getCoreName(accountId);
        try(SolrClient solrClient = SolrUtils.getSolrClient(httpClient, baseUrl, coreName)) {
            SolrUtils.executeRequestWithRetry(solrClient, request, baseUrl, coreName, configSet);
        } catch (IOException e) {
            throw ServiceException.FAILURE(String.format("unable to execute Solr request for account %s", accountId), e);
        }
    }

    @Override
    public UpdateRequest newRequest(String accountId) {
        return new UpdateRequest();
    }

    @Override
    public void deleteIndex(String accountId) throws ServiceException {
        String coreName = locator.getCoreName(accountId);
        try(SolrClient solrClient = SolrUtils.getSolrClient(httpClient, baseUrl, coreName)) {
            SolrUtils.deleteStandaloneIndex(solrClient, baseUrl, coreName);
        } catch (IOException e) {
            throw ServiceException.FAILURE(String.format("unable to execute Solr request for account %s", accountId), e);
        }

    }
}