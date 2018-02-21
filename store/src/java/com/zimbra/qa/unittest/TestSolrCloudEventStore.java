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
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.SolrCloudEventStore;
import com.zimbra.cs.event.SolrEventStore;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyGraphSpec;
import com.zimbra.cs.event.logger.SolrEventCallback;
import com.zimbra.cs.index.solr.AccountCollectionLocator;
import com.zimbra.cs.index.solr.JointCollectionLocator;
import com.zimbra.cs.index.solr.SolrCloudHelper;
import com.zimbra.cs.index.solr.SolrCollectionLocator;
import com.zimbra.cs.index.solr.SolrIndex.IndexType;
import com.zimbra.cs.index.solr.SolrRequestHelper;
import com.zimbra.cs.index.solr.SolrUtils;

public class TestSolrCloudEventStore extends SolrEventStoreTestBase {

    private static String JOINT_COLLECTION_NAME = "events_test";
    private static String CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_USERNAME = "contactFrequencyGraphTestAccount";
    private static Account CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT;

    private CloudSolrClient client;
    private String zkHost;

    @Before
    public void setUp() throws Exception {
        String solrUrl = Provisioning.getInstance().getLocalServer().getEventBackendURL();
        Assume.assumeTrue(solrUrl.startsWith("solrcloud"));
        zkHost = solrUrl.substring("solrcloud:".length());
        client = SolrUtils.getCloudSolrClient(zkHost);
        TestUtil.deleteAccountIfExists(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_USERNAME);
        TestUtil.deleteAccountIfExists(ACCOUNT_1);
        TestUtil.deleteAccountIfExists(ACCOUNT_2);
        ACCOUNT_ID_1 = TestUtil.createAccount(ACCOUNT_1).getId();
        ACCOUNT_ID_2 = TestUtil.createAccount(ACCOUNT_2).getId();
        CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT = TestUtil.createAccount(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_USERNAME);
        CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID = CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT.getId();
        cleanUp();
    }

    private void deleteCollection(String collection) {
        CollectionAdminRequest.Delete deleteCollectionRequest = CollectionAdminRequest.deleteCollection(collection);
        try {
            deleteCollectionRequest.process(client);
        } catch (RemoteSolrException | SolrServerException | IOException e) {
            //collection may not exist, that's OK
        }
    }

    public void cleanUp() throws Exception {
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
        TestUtil.deleteAccountIfExists(ACCOUNT_1);
        TestUtil.deleteAccountIfExists(ACCOUNT_2);
    }

    @Override
    protected SolrCloudEventStore getCombinedEventStore(String accountId) {
        SolrCollectionLocator locator = new JointCollectionLocator(JOINT_COLLECTION_NAME);
        SolrCloudHelper helper = new SolrCloudHelper(locator, client, IndexType.EVENTS);
        return new SolrCloudEventStore(accountId, helper);
    }

    @Override
    protected SolrCloudEventStore getAccountEventStore(String accountId) {
        SolrCollectionLocator locator = new AccountCollectionLocator(JOINT_COLLECTION_NAME);
        SolrCloudHelper helper = new SolrCloudHelper(locator, client, IndexType.EVENTS);
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
        SolrRequestHelper helper = new SolrCloudHelper(locator, client, IndexType.EVENTS);
        return new SolrEventCallback(helper);
    }

    @Override
    protected SolrEventCallback getAccountCoreCallback() {
        SolrCollectionLocator locator = new AccountCollectionLocator(JOINT_COLLECTION_NAME);
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        SolrRequestHelper helper = new SolrCloudHelper(locator, client, IndexType.EVENTS);
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
        try (SolrCloudHelper solrHelper = getSolrHelper(new AccountCollectionLocator(JOINT_COLLECTION_NAME))) {
            SolrEventCallback eventCallback = new SolrEventCallback(solrHelper);
            SolrEventStore eventStore = new SolrCloudEventStore(ACCOUNT_ID_1, solrHelper);
            testContactFrequencyCount(timeRange, eventCallback, getAccountCollectionName(ACCOUNT_ID_1), eventStore);
        }
    }

    private SolrCloudHelper getSolrHelper(SolrCollectionLocator locator) {
        CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
        return new SolrCloudHelper(locator, client, IndexType.EVENTS);
    }

    public void testContactFrequencyCountForCombinedCore(ContactAnalytics.ContactFrequencyTimeRange timeRange) throws Exception {
        cleanUp();
        try (SolrCloudHelper solrHelper = getSolrHelper(new JointCollectionLocator(JOINT_COLLECTION_NAME))) {
            SolrEventCallback eventCallback = new SolrEventCallback(solrHelper);
            SolrEventStore eventStore = new SolrCloudEventStore(ACCOUNT_ID_1, solrHelper);
            testContactFrequencyCount(timeRange, eventCallback, JOINT_COLLECTION_NAME, eventStore);
        }
    }

    @Test
    public void testGetContactFrequencyGraphForAllTimeRanges() throws Exception {
        for (ContactFrequencyGraphSpec graphSpec : getContactFrequencyGraphSpecs()) {
            testContactFrequencyGraphForAccountCore(graphSpec);
            testContactFrequencyGraphForCombinedCore(graphSpec);
        }
    }

    private void testContactFrequencyGraphForAccountCore(ContactFrequencyGraphSpec graphSpec) throws Exception {
        cleanUp();
        try (SolrEventCallback eventCallback = getAccountCoreCallback()) {
            String collectionName = getAccountCollectionName(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID);
            SolrEventStore eventStore = getAccountEventStore(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID);
            testContactFrequencyGraph(graphSpec, eventCallback, collectionName, eventStore);
        }
    }

    private void testContactFrequencyGraphForCombinedCore(ContactFrequencyGraphSpec graphSpec) throws Exception {
        cleanUp();
        try (SolrEventCallback eventCallback = getCombinedCoreCallback()) {
            SolrEventStore eventStore = getCombinedEventStore(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID);
            testContactFrequencyGraph(graphSpec, eventCallback, JOINT_COLLECTION_NAME, eventStore);
        }
    }

    @FunctionalInterface
    public interface ExecuteTest {
        void execute(SolrEventCallback eventCallback, String collectionName, SolrEventStore eventStore) throws Exception;
    }

    @Test
    public void testPercentageOpenedEmails() throws Exception {
        ExecuteTest percentageOpenedEmails = (ec, cn, es) -> testPercentageOpenedEmails(ec, cn, es);
        testForBothCores(percentageOpenedEmails);
    }

    @Test
    public void testGetAvgTimeToOpenEmailForAcc() throws Exception {
        ExecuteTest getAvgTimeToOpenEmailForAccount = (ec, cn, es) -> testGetAvgTimeToOpenEmailForAccount(ec, cn, es);
        testForBothCores(getAvgTimeToOpenEmailForAccount);
    }

    @Test
    public void testGetAvgTimeToOpenEmailForContact() throws Exception {
        ExecuteTest getAvgTimeToOpenEmail = (ec, cn, es) -> testGetAvgTimeToOpenEmail(ec, cn, es);
        testForBothCores(getAvgTimeToOpenEmail);
    }

    @Test
    public void testPercentageRepliedEmails() throws Exception {
        ExecuteTest getPercentageRepliedEmails = (ec, cn, es) -> testPercentageRepliedEmails(ec, cn, es);
        testForBothCores(getPercentageRepliedEmails);
    }

    private void testForBothCores(ExecuteTest test) throws Exception {
        testForAccountCore(test);
        testForCombinedCore(test);
    }

    private void testForAccountCore(ExecuteTest test) throws Exception {
        cleanUp();
        try (SolrCloudHelper solrHelper = getSolrHelper(new AccountCollectionLocator(JOINT_COLLECTION_NAME))) {
            SolrEventCallback eventCallback = new SolrEventCallback(solrHelper);
            SolrEventStore eventStore = new SolrCloudEventStore(ACCOUNT_ID_1, solrHelper);
            test.execute(eventCallback, getAccountCollectionName(ACCOUNT_ID_1), eventStore);
        }
    }

    private void testForCombinedCore(ExecuteTest test) throws Exception {
        cleanUp();
        try (SolrCloudHelper solrHelper = getSolrHelper(new JointCollectionLocator(JOINT_COLLECTION_NAME))) {
            SolrEventCallback eventCallback = new SolrEventCallback(solrHelper);
            SolrEventStore eventStore = new SolrCloudEventStore(ACCOUNT_ID_1, solrHelper);
            test.execute(eventCallback, JOINT_COLLECTION_NAME, eventStore);
        }
    }
}
