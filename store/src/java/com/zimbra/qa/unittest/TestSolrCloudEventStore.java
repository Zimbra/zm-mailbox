package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CoreAdminParams;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.logger.SolrCloudEventStore;
import com.zimbra.cs.event.logger.SolrEventCallback;
import com.zimbra.cs.event.logger.SolrEventStore;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;

public class TestSolrCloudEventStore extends SolrEventStoreTestBase {

    private static CloudSolrClient client;
    private static String zkHost;

    @BeforeClass
    public static void init() throws Exception {
        String solrUrl = Provisioning.getInstance().getLocalServer().getEventBackendURL();
        Assume.assumeTrue(solrUrl.startsWith("solrcloud"));
        zkHost = solrUrl.substring("solrcloud:".length());
        client = SolrUtils.getCloudSolrClient(zkHost);
        cleanUp();
    }

    private static void deleteCollection(String collection) {
        CollectionAdminRequest.Delete deleteCollectionRequest = CollectionAdminRequest.deleteCollection(collection);
        try {
            deleteCollectionRequest.process(client);
        } catch (RemoteSolrException | SolrServerException | IOException e) {
            //collection may not exist, that's OK
        }
    }

    public static void cleanUp() throws Exception {
        deleteCollection(JOINT_COLLECTION_NAME);
        deleteCollection(getAccountCollectionName(ACCOUNT_ID_1));
        deleteCollection(getAccountCollectionName(ACCOUNT_ID_2));
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
        client.close();
    }

    @Override
    protected SolrCloudEventStore getCombinedEventStore(String accountId) {
        SolrCollectionLocator locator = new JointCollectionLocator(JOINT_COLLECTION_NAME);
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrCloudHelper helper = new SolrCloudHelper(locator, client, SolrConstants.CONFIGSET_EVENTS);
        return new SolrCloudEventStore(accountId, helper);
    }

    @Override
    protected SolrCloudEventStore getAccountEventStore(String accountId) {
        SolrCollectionLocator locator = new AccountCollectionLocator(JOINT_COLLECTION_NAME);
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrCloudHelper helper = new SolrCloudHelper(locator, client, SolrConstants.CONFIGSET_EVENTS);
        return new SolrCloudEventStore(accountId, helper);
    }

    @Override
    protected void commit(String collectionName) throws Exception {
        UpdateRequest commitReq = new UpdateRequest();
        commitReq.setAction(ACTION.COMMIT, true, true);
        commitReq.setParam(CoreAdminParams.COLLECTION, collectionName);
        commitReq.process(client);
    }

    @Override
    protected SolrEventCallback getCombinedCoreCallback() {
        SolrCollectionLocator locator = new JointCollectionLocator(JOINT_COLLECTION_NAME);
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrRequestHelper helper = new SolrCloudHelper(locator, client, SolrConstants.CONFIGSET_EVENTS);
        return new SolrEventCallback(helper);
    }

    @Override
    protected SolrEventCallback getAccountCoreCallback() {
        SolrCollectionLocator locator = new AccountCollectionLocator(JOINT_COLLECTION_NAME);
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrRequestHelper helper = new SolrCloudHelper(locator, client, SolrConstants.CONFIGSET_EVENTS);
        return new SolrEventCallback(helper);
    }

    @Override
    protected SolrQuery newQuery(String collection) {
        SolrQuery query = new SolrQuery();
        query.setParam(CoreAdminParams.COLLECTION, collection);
        return query;
    }

    @Override
    protected SolrDocumentList executeRequest(String coreOrCollection,
            QueryRequest req) throws Exception {
        QueryResponse resp = req.process(client);
        return resp.getResults();
    }
}
