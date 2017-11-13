package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.zimbra.cs.event.logger.SolrEventCallback;
import com.zimbra.cs.event.logger.SolrCloudEventStore;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;

public class TestSolrCloudEventStore {

    private static String ACCOUNT_ID_1 = "test-id-1";
    private static String ACCOUNT_ID_2 = "test-id-2";
    private static String JOINT_COLLECTION_NAME = "events_test";
    private static String ACCOUNT_COLLECTION_PREFIX = "events_test";
    private static CloudSolrClient client;
    private static String zkHost;

    @BeforeClass
    public static void init() throws Exception {
        String solrUrl = Provisioning.getInstance().getLocalServer().getIndexURL();
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
    }

    private List<Event> getSentEvents(String accountId, String dsId, int num, int startMsgId) {
        List<Event> events = new ArrayList<Event>(num);
        Map<EventContextField, Object> context = new HashMap<EventContextField, Object>();
        context.put(EventContextField.RECEIVER, "testrecipient");
        for (int i=0; i<num; i++) {
            context.put(EventContextField.MSG_ID, startMsgId+i);
            Event event = new Event(accountId, EventType.SENT, System.currentTimeMillis() - i*1000, context);
            if (dsId != null) {
                event.setDataSourceId(dsId);
            }
            events.add(event);
        }
        return events;
    }

    private SolrCloudEventStore getCombinedEventStore(String accountId) {
        SolrCollectionLocator locator = new JointCollectionLocator(JOINT_COLLECTION_NAME);
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrCloudHelper helper = new SolrCloudHelper(locator, client, SolrConstants.CONFIGSET_EVENTS);
        return new SolrCloudEventStore(accountId, helper);
    }

    private SolrCloudEventStore getAccountEventStore(String accountId) {
        SolrCollectionLocator locator = new AccountCollectionLocator(JOINT_COLLECTION_NAME);
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrCloudHelper helper = new SolrCloudHelper(locator, client, SolrConstants.CONFIGSET_EVENTS);
        return new SolrCloudEventStore(accountId, helper);
    }

    private void commit(String collectionName) throws Exception {
        UpdateRequest commitReq = new UpdateRequest();
        commitReq.setAction(ACTION.COMMIT, true, true);
        commitReq.setParam(CoreAdminParams.COLLECTION, collectionName);
        commitReq.process(client);
    }

    private static String getAccountCollectionName(String accountId) {
        return new AccountCollectionLocator(ACCOUNT_COLLECTION_PREFIX).getCoreName(accountId);
    }

    private SolrDocumentList queryEvents(String collection) throws Exception {
        return queryEvents(collection, null, null);
    }

    private SolrDocumentList queryEvents(String collection, String accountId) throws Exception {
        return queryEvents(collection, accountId, null);
    }

    private SolrDocumentList queryEvents(String collection, String accountId, String dsId) throws Exception {
        SolrQuery query = new SolrQuery("ev_type:*");
        if (accountId != null) {
            query.addFilterQuery("acct_id:"+accountId);
        }
        if (dsId != null) {
            query.addFilterQuery("datasource_id:"+dsId);
        }
        query.setParam(CoreAdminParams.COLLECTION, collection);
        QueryRequest req = new QueryRequest(query, METHOD.POST);
        QueryResponse resp = req.process(client);
        return resp.getResults();
    }

    private SolrEventCallback getCombinedCoreCallback() {
        SolrCollectionLocator locator = new JointCollectionLocator(JOINT_COLLECTION_NAME);
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrRequestHelper helper = new SolrCloudHelper(locator, client, SolrConstants.CONFIGSET_EVENTS);
        return new SolrEventCallback(helper);
    }

    private SolrEventCallback getAccountCoreCallback() {
        SolrCollectionLocator locator = new AccountCollectionLocator(JOINT_COLLECTION_NAME);
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrRequestHelper helper = new SolrCloudHelper(locator, client, SolrConstants.CONFIGSET_EVENTS);
        return new SolrEventCallback(helper);
    }

    @Test
    public void testAccountCoreCallback() throws Exception {
        try(SolrEventCallback callback = getAccountCoreCallback()) {
            String collection1 = getAccountCollectionName(ACCOUNT_ID_1);
            String collection2 = getAccountCollectionName(ACCOUNT_ID_2);
            callback.execute(ACCOUNT_ID_1, getSentEvents(ACCOUNT_ID_1, null, 10, 1));
            callback.execute(ACCOUNT_ID_2, getSentEvents(ACCOUNT_ID_2, null, 5, 1));
            commit(collection1);
            commit(collection2);
            SolrDocumentList results = queryEvents(collection1);
            assertEquals("should see 10 results in collection 1", 10, (int) results.getNumFound());
            results = queryEvents(collection2);
            assertEquals("should see 5 results in collection 2", 5, (int) results.getNumFound());
        }
    }

    @Test
    public void testCombinedCoreCallback() throws Exception {
        try(SolrEventCallback callback = getCombinedCoreCallback()) {
            callback.execute(ACCOUNT_ID_1, getSentEvents(ACCOUNT_ID_1, null, 10, 1));
            callback.execute(ACCOUNT_ID_2, getSentEvents(ACCOUNT_ID_2, null, 5, 1));
            commit(JOINT_COLLECTION_NAME);
            SolrDocumentList results = queryEvents(JOINT_COLLECTION_NAME);
            assertEquals("should see 15 results in joint collection", 15, (int) results.getNumFound());
        }
    }

    @Test
    public void testDeleteDatasourceCombinedCollection() throws Exception {
        String dsId1 = "test-datasource-id-1";
        String dsId2 = "test-datasource-id-2";
        try(SolrEventCallback callback = getCombinedCoreCallback()) {
            callback.execute(ACCOUNT_ID_1, getSentEvents(ACCOUNT_ID_1, dsId1, 5, 1));
            callback.execute(ACCOUNT_ID_1, getSentEvents(ACCOUNT_ID_1, dsId2, 5, 10));
            callback.execute(ACCOUNT_ID_1, getSentEvents(ACCOUNT_ID_1, null, 5, 20));
            callback.execute(ACCOUNT_ID_2, getSentEvents(ACCOUNT_ID_2, dsId1, 5, 1));
            commit(JOINT_COLLECTION_NAME);
            //sanity check
            SolrDocumentList results = queryEvents(JOINT_COLLECTION_NAME);
            assertEquals("should see 20 results in joint collection", 20, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_1);
            assertEquals("should see 15 results in joint collection for test-id-1", 15, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_2);
            assertEquals("should see 5 results in joint collection for test-id-2", 5, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_1, dsId1);
            assertEquals("should see 5 results in joint collection for test-id-1 with test-datasource-id-1", 5, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_1, dsId2);
            assertEquals("should see 5 results in joint collection for test-id-1 with test-datasource-id-2", 5, (int) results.getNumFound());

            SolrCloudEventStore eventStore = getCombinedEventStore(ACCOUNT_ID_1);
            eventStore.deleteEvents(dsId1);
            commit(JOINT_COLLECTION_NAME);
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_1);
            assertEquals("should see 10 results in joint collection for test-id-1", 10, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_1, dsId1);
            assertEquals("should see 0 results in joint collection for test-id-1 with test-datasource-id-1", 0, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_1, dsId2);
            assertEquals("should see 5 results in joint collection for test-id-1 with test-datasource-id-2", 5, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_2, dsId1);
            assertEquals("should see 5 results in joint collection for test-id-2 with test-datasource-id-2", 5, (int) results.getNumFound());
        }
    }

    @Test
    public void testDeleteDatasourceAccountCollection() throws Exception {
        String dsId1 = "datasource-id-1";
        String dsId2 = "datasource-id-2";
        String collectionName = getAccountCollectionName(ACCOUNT_ID_1);
        try(SolrEventCallback callback = getAccountCoreCallback()) {
            callback.execute(ACCOUNT_ID_1, getSentEvents(ACCOUNT_ID_1, dsId1, 5, 1));
            callback.execute(ACCOUNT_ID_1, getSentEvents(ACCOUNT_ID_1, dsId2, 5, 10));
            callback.execute(ACCOUNT_ID_1, getSentEvents(ACCOUNT_ID_1, null, 5, 20));
            commit(collectionName);
            //sanity check
            SolrDocumentList results = queryEvents(collectionName);
            assertEquals("should see 15 results in test-id-1 collection", 15, (int) results.getNumFound());
            results = queryEvents(collectionName, null, dsId1);
            assertEquals("should see 5 results in test-id-1 collection with datasource-id-1", 5, (int) results.getNumFound());
            SolrCloudEventStore eventStore = getAccountEventStore(ACCOUNT_ID_1);
            eventStore.deleteEvents(dsId1);
            commit(collectionName);
            results = queryEvents(collectionName);
            assertEquals("should see 10 results in test-id-1 collection", 10, (int) results.getNumFound());
            results = queryEvents(collectionName, null, dsId1);
            assertEquals("should see 0 results in test-id-1 collection or datasource-id-1", 0, (int) results.getNumFound());
            results = queryEvents(collectionName, null, dsId2);
            assertEquals("should see 5 results in test-id-1 collection or datasource-id-2", 5, (int) results.getNumFound());
        }
    }

    @Test
    public void testDeleteAccountEventsCombinedCollection() throws Exception {
        try(SolrEventCallback callback = getCombinedCoreCallback()) {
            callback.execute(ACCOUNT_ID_1, getSentEvents(ACCOUNT_ID_1, null, 10, 1));
            callback.execute(ACCOUNT_ID_2, getSentEvents(ACCOUNT_ID_2, null, 10, 1));
            commit(JOINT_COLLECTION_NAME);
            //sanity check
            SolrDocumentList results = queryEvents(JOINT_COLLECTION_NAME);
            assertEquals("should see 20 results in joint collection", 20, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_1);
            assertEquals("should see 10 results in joint collection for test-id-1", 10, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_2);
            assertEquals("should see 10 results in joint collection for test-id-2", 10, (int) results.getNumFound());

            SolrCloudEventStore eventStore1 = getCombinedEventStore(ACCOUNT_ID_1);
            eventStore1.deleteEvents();
            commit(JOINT_COLLECTION_NAME);
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_1);
            assertEquals("should see 0 results in joint collection for test-id-1", 0, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_2);
            assertEquals("should see 10 results in joint collection for test-id-2", 10, (int) results.getNumFound());

            SolrCloudEventStore eventStore2 = getCombinedEventStore(ACCOUNT_ID_2);
            eventStore2.deleteEvents();
            commit(JOINT_COLLECTION_NAME);
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_2);
            assertEquals("should see 0 results in joint collection for test-id-2", 0, (int) results.getNumFound());
        }
    }

    @Test
    public void testDeleteAccountEventsAccountCollection() throws Exception {
        String collectionName = getAccountCollectionName(ACCOUNT_ID_1);
        try(SolrEventCallback callback = getAccountCoreCallback()) {
            callback.execute(ACCOUNT_ID_1, getSentEvents(ACCOUNT_ID_1, null, 10, 1));
            commit(collectionName);
            //sanity check
            SolrDocumentList results = queryEvents(collectionName);
            assertEquals("should see 10 results in test-id-1 collection", 10, (int) results.getNumFound());

            SolrCloudEventStore eventStore = getAccountEventStore(ACCOUNT_ID_1);
            eventStore.deleteEvents();
            try {
                results = queryEvents(collectionName);
                fail("collection should be deleted");
            } catch (Exception e) {
                String msg = e.getMessage().toLowerCase();
                assertTrue(msg.startsWith("collection not found"));
            }
        }
    }

    @Test
    public void testSkipExisting() throws Exception {
        int msgId = 1;
        long timestamp1 = 1000;
        long timestamp2 = 2000;
        List<Event> events = new ArrayList<>(2);
        Event event1 = new Event(ACCOUNT_ID_1, EventType.SENT, timestamp1);
        event1.setContextField(EventContextField.MSG_ID, msgId);
        events.add(event1);
        Event event2 = new Event(ACCOUNT_ID_1, EventType.SENT, timestamp2);
        event2.setContextField(EventContextField.MSG_ID, msgId);
        events.add(event2);

        try(SolrEventCallback callback = getCombinedCoreCallback()) {
            callback.execute(ACCOUNT_ID_1, events);
            commit(JOINT_COLLECTION_NAME);
            SolrDocumentList results = queryEvents(JOINT_COLLECTION_NAME);
            assertEquals("should only see one event for test-id-1", 1, (int) results.getNumFound());
            SolrDocument eventDoc = results.get(0);
            Date date = (Date) eventDoc.getFieldValue("ev_timestamp");
            assertEquals("event should have first timestamp", timestamp1, date.getTime());
        }
    }
}
