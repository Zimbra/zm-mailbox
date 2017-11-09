package com.zimbra.qa.unittest;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CoreAdminParams;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.SolrCloudEventStore;
import com.zimbra.cs.event.logger.SolrEventCallback;
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

    @Test
    public void testContactFrequency() throws Exception {
        List<Event> events = new ArrayList<>(4);
        Event event1 = Event.generateSentEvent(ACCOUNT_ID_1, 1, "testSender@zcs-dev.test", "testRecipient@zcs-dev.test", "testDSId", System.currentTimeMillis());
        events.add(event1);
        Event event2 = Event.generateSentEvent(ACCOUNT_ID_1, 2, "testSender@zcs-dev.test", "testRecipient@zcs-dev.test", "testDSId", System.currentTimeMillis());
        events.add(event2);
        Event event3 = Event.generateReceivedEvent(ACCOUNT_ID_1, 3, "testRecipient@zcs-dev.test", "testSender@zcs-dev.test", "testDSId", System.currentTimeMillis());
        events.add(event3);
        Event event4 = Event.generateReceivedEvent(ACCOUNT_ID_1, 4, "testRecipient1@zcs-dev.test", "testSender@zcs-dev.test", "testDSId", System.currentTimeMillis());
        events.add(event4);

        try(SolrEventCallback callback = getAccountCoreCallback()) {
            callback.execute(ACCOUNT_ID_1, events);
            String collectionName = getAccountCollectionName(ACCOUNT_ID_1);
            commit(collectionName);
            SolrDocumentList results = queryEvents(collectionName);
            assertEquals("should see 4 results in test-id-1 collection", 4, (int) results.getNumFound());
            SolrEventStore eventStore = getAccountEventStore(ACCOUNT_ID_1);
            Long contactFrequencyCount = eventStore.getContactFrequencyCount("testRecipient@zcs-dev.test");
            assertEquals("frequency should be 3", new Long(3), contactFrequencyCount);
        }
    }
}
