package com.zimbra.qa.unittest;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.event.EventStore;
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

import org.junit.*;

import com.zimbra.cs.event.analytics.contact.ContactAnalytics;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.SolrCloudEventStore;
import com.zimbra.cs.event.SolrEventStore;
import com.zimbra.cs.event.logger.SolrEventCallback;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrConstants;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;

public class TestSolrCloudEventStore extends SolrEventStoreTestBase {

    private static String ACCOUNT_ID_1 = "test-id-1";
    private static String ACCOUNT_ID_2 = "test-id-2";
    private static String JOINT_COLLECTION_NAME = "events_test";
    private static String CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_USERNAME = "contactFrequencyGraphTestAccount";
    private static Account CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT;

    private static CloudSolrClient client;
    private static String zkHost;

    @BeforeClass
    public static void init() throws Exception {
        String solrUrl = Provisioning.getInstance().getLocalServer().getEventBackendURL();
        Assume.assumeTrue(solrUrl.startsWith("solrcloud"));
        zkHost = solrUrl.substring("solrcloud:".length());
        client = SolrUtils.getCloudSolrClient(zkHost);
        TestUtil.deleteAccountIfExists(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_USERNAME);
        CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT = TestUtil.createAccount(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_USERNAME);
        CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID = CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT.getId();
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
        deleteCollection(getAccountCollectionName(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID));
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
        client.close();
    }

    @AfterClass
    public static void clean() throws Exception {
        TestUtil.deleteAccountIfExists(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_USERNAME);
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
    protected SolrDocumentList executeRequest(String coreOrCollection, QueryRequest req) throws Exception {
        QueryResponse resp = req.process(client);
        return resp.getResults();
    }

    @Test
    public void testContactFrequencyCountForAllTimeRanges() throws Exception {
        for (ContactAnalytics.ContactFrequencyTimeRange timeRange : getContactFrequencyCountTimeRanges()) {
            testContactFrequencyCountForAccountCore(timeRange);
            testContactFrequencyCountForCombinedCore(timeRange);
        }
    }

    public void testContactFrequencyCountForAccountCore(ContactAnalytics.ContactFrequencyTimeRange timeRange) throws Exception {
        cleanUp();
        try(SolrEventCallback eventCallback = getAccountCoreCallback()) {
            testContactFrequencyCount(timeRange, eventCallback, getAccountCollectionName(ACCOUNT_ID_1), getAccountEventStore(ACCOUNT_ID_1));
        }
    }

    public void testContactFrequencyCountForCombinedCore(ContactAnalytics.ContactFrequencyTimeRange timeRange) throws Exception {
        cleanUp();
        try(SolrEventCallback eventCallback = getCombinedCoreCallback()) {
            testContactFrequencyCount(timeRange, eventCallback, JOINT_COLLECTION_NAME, getCombinedEventStore(ACCOUNT_ID_1));
        }
    }

    @Test
    public void testGetContactFrequencyGraphForAllTimeRanges() throws Exception {
        for (ContactAnalytics.ContactFrequencyGraphTimeRange timeRange : getContactFrequencyGraphTimeRanges()) {
            testContactFrequencyGraphForAccountCore(timeRange);
            testContactFrequencyGraphForCombinedCore(timeRange);
        }
    }

    private void testContactFrequencyGraphForAccountCore(ContactAnalytics.ContactFrequencyGraphTimeRange timeRange) throws Exception {
        cleanUp();
        try(SolrEventCallback eventCallback = getAccountCoreCallback()) {
            String collectionName = getAccountCollectionName(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID);
            SolrEventStore eventStore = getAccountEventStore(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID);
            testContactFrequencyGraph(timeRange, eventCallback, collectionName, eventStore);
        }
    }

    private void testContactFrequencyGraphForCombinedCore(ContactAnalytics.ContactFrequencyGraphTimeRange timeRange) throws Exception {
        cleanUp();
        try(SolrEventCallback eventCallback = getCombinedCoreCallback()) {
            SolrEventStore eventStore = getCombinedEventStore(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID);
            testContactFrequencyGraph(timeRange, eventCallback, JOINT_COLLECTION_NAME, eventStore);
        }
    }

    @Test
    public void testPercentageOpenedEmails() throws Exception {
        testPercentageOpenedEmailsForAccountCore();
        testPercentageOpenedEmailsForCombinedCore();
    }

    private void testPercentageOpenedEmailsForAccountCore() throws Exception {
        cleanUp();
        try(SolrEventCallback eventCallback = getAccountCoreCallback()) {
            testPercentageOpenedEmails(eventCallback, getAccountCollectionName(ACCOUNT_ID_1), getAccountEventStore(ACCOUNT_ID_1));
        }
    }

    private void testPercentageOpenedEmailsForCombinedCore() throws Exception {
        cleanUp();
        try(SolrEventCallback eventCallback = getCombinedCoreCallback()) {
            testPercentageOpenedEmails(eventCallback, JOINT_COLLECTION_NAME, getCombinedEventStore(ACCOUNT_ID_1));
        }
    }

    @Test
    public void testGetAvgTimeToOpenEmailForAccount() throws Exception {
        testGetAvgTimeToOpenEmailForAccountForAccountCore();
        testGetAvgTimeToOpenEmailForAccountForCombinedCore();
    }

    private void testGetAvgTimeToOpenEmailForAccountForAccountCore() throws Exception {
        cleanUp();
        try(SolrEventCallback eventCallback = getAccountCoreCallback()) {
            testGetAvgTimeToOpenEmailForAccount(eventCallback, getAccountCollectionName(ACCOUNT_ID_1), getAccountEventStore(ACCOUNT_ID_1));
            cleanUp();
            testGetAvgTimeToOpenEmail(eventCallback, getAccountCollectionName(ACCOUNT_ID_1), getAccountEventStore(ACCOUNT_ID_1));
            cleanUp();
            testGetRatioOfAvgTimeToOpenEmailToGlobalAvg(eventCallback, getAccountCollectionName(ACCOUNT_ID_1), getAccountEventStore(ACCOUNT_ID_1));
        }
    }

    private void testGetAvgTimeToOpenEmailForAccountForCombinedCore() throws Exception {
        cleanUp();
        try(SolrEventCallback eventCallback = getCombinedCoreCallback()) {
            testGetAvgTimeToOpenEmailForAccount(eventCallback, JOINT_COLLECTION_NAME, getCombinedEventStore(ACCOUNT_ID_1));
            cleanUp();
            testGetAvgTimeToOpenEmail(eventCallback, JOINT_COLLECTION_NAME, getCombinedEventStore(ACCOUNT_ID_1));
            cleanUp();
            testGetRatioOfAvgTimeToOpenEmailToGlobalAvg(eventCallback, JOINT_COLLECTION_NAME, getCombinedEventStore(ACCOUNT_ID_1));
        }
    }
}
