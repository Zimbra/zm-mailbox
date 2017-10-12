package com.zimbra.cs.index.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.params.CoreAdminParams;

import com.zimbra.common.service.ServiceException;

public class SolrCloudHelper extends SolrRequestHelper {

    private CloudSolrClient cloudClient;

    public SolrCloudHelper(SolrCollectionLocator locator, CloudSolrClient cloudClient, String configSet) {
        super(locator, configSet);
        this.cloudClient = cloudClient;
    }

    @Override
    public void close() throws IOException {
        cloudClient.close();
    }

    @Override
    protected void executeRequest(String accountId, UpdateRequest request)
            throws ServiceException {
        request.setParam(CoreAdminParams.COLLECTION, locator.getCoreName(accountId));
        SolrUtils.executeCloudRequestWithRetry(cloudClient, request, locator.getCoreName(accountId), configSet);
    }

    @Override
    public UpdateRequest newRequest(String accountId) {
        UpdateRequest req = new UpdateRequest();
        req.setParam(CoreAdminParams.COLLECTION, locator.getCoreName(accountId));
        return req;
    }

    public String getZkHost() {
        return cloudClient.getZkHost();
    }
}