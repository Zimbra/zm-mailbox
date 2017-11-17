package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.analytics.contact.ContactFrequencyGraph;
import com.zimbra.cs.event.analytics.contact.ContactFrequencyGraphDataPoint;
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
    private static String CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID;
    private static CloudSolrClient client;
    private static String zkHost;

    @BeforeClass
    public static void init() throws Exception {
        String solrUrl = Provisioning.getInstance().getLocalServer().getEventBackendURL();
        Assume.assumeTrue(solrUrl.startsWith("solrcloud"));
        zkHost = solrUrl.substring("solrcloud:".length());
        client = SolrUtils.getCloudSolrClient(zkHost);
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
    protected SolrDocumentList executeRequest(String coreOrCollection,
            QueryRequest req) throws Exception {
        QueryResponse resp = req.process(client);
        return resp.getResults();
    }

    @Test
    public void testContactFrequencyCount() throws Exception {
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

    @Test
    public void testGetContactFrequencyGraphForCurrentMonth() throws Exception {
        testContactFrequencyGraph(ContactFrequencyGraph.TimeRange.CURRENT_MONTH);
    }

    @Test
    public void testGetContactFrequencyGraphForLastSixMonths() throws Exception {
        testContactFrequencyGraph(ContactFrequencyGraph.TimeRange.LAST_SIX_MONTHS);
    }

    @Test
    public void testGetContactFrequencyGraphForCurrentYear() throws Exception {
        testContactFrequencyGraph(ContactFrequencyGraph.TimeRange.CURRENT_YEAR);
    }

    public void testContactFrequencyGraph(ContactFrequencyGraph.TimeRange timeRange) throws Exception {
        List<Timestamp> timestamps = getTimestampsForTest(timeRange);
        List<Event> events = new ArrayList<>(timestamps.size());
        int i = 1;
        for (Timestamp timestamp : timestamps) {
            events.add(Event.generateSentEvent(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID, i++, "testSender@zcs-dev.test", "testRecipient@zcs-dev.test", "testDSId", timestamp.getTime()));
        }
        try(SolrEventCallback callback = getAccountCoreCallback()) {
            callback.execute(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID, events);
            String collectionName = getAccountCollectionName(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID);
            commit(collectionName);
            SolrDocumentList results = queryEvents(collectionName);
            assertEquals("should see " + timestamps.size() + " events in test-id-1 collection", timestamps.size(), (int) results.getNumFound());
            SolrEventStore eventStore = getAccountEventStore(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID);
            List<ContactFrequencyGraphDataPoint> contactFrequencyGraphDataPoints = ContactFrequencyGraph.getContactFrequencyGraph("testRecipient@zcs-dev.test", timeRange, eventStore);
            assertNotNull(contactFrequencyGraphDataPoints);
            assertEquals("The size of the result should be equal to the size of timestamps that we produced", timestamps.size(), contactFrequencyGraphDataPoints.size());
            for (ContactFrequencyGraphDataPoint dataPoint : contactFrequencyGraphDataPoints) {
                assertNotNull(dataPoint.getLabel());
                assertEquals("For each time range we should have 1 event. Failed for range starting " + dataPoint.getLabel(), 1, dataPoint.getValue());
            }
        }
    }

    private List<Timestamp> getTimestampsForTest(ContactFrequencyGraph.TimeRange timeRange) {
        switch (timeRange) {
            case CURRENT_MONTH:
                return getTimestampsForEachDayInCurrentMonth();
            case LAST_SIX_MONTHS:
                return getTimestampsInEachWeekForLast6Months();
            case CURRENT_YEAR:
                return getTimestampsForEachMonthInCurrentYear();
            default:
                return getTimestampsForEachDayInCurrentMonth();
        }
    }

    private List<Timestamp> getTimestampsForEachMonthInCurrentYear() {
        LocalDateTime now = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime firstDayOfCurrentYear = now.with(TemporalAdjusters.firstDayOfYear());
        List<Timestamp> monthsInCurrentYear = new ArrayList<>();
        for (int i = 0; i < ChronoUnit.MONTHS.between(firstDayOfCurrentYear, now) + 1; i++) {
            monthsInCurrentYear.add(Timestamp.valueOf(firstDayOfCurrentYear.plusMonths(i)));
        }
        return monthsInCurrentYear;
    }

    private List<Timestamp> getTimestampsInEachWeekForLast6Months() {
        LocalDateTime firstDayOfCurrentWeek = LocalDateTime.now().with(WeekFields.of(Locale.US).dayOfWeek(), 1);
        LocalDateTime firstDayOfWeek6MonthsBack = firstDayOfCurrentWeek.minusMonths(6).with(WeekFields.of(Locale.US).dayOfWeek(), 1);
        List<Timestamp> weeksIn6Months = new ArrayList<>();
        for (int i = 0; i < ChronoUnit.WEEKS.between(firstDayOfWeek6MonthsBack, firstDayOfCurrentWeek); i++) {
            weeksIn6Months.add(Timestamp.valueOf(firstDayOfWeek6MonthsBack.plusDays(i * 7)));
        }
        return weeksIn6Months;
    }

    private List<Timestamp> getTimestampsForEachDayInCurrentMonth() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        List<Timestamp> daysOfMonth = new ArrayList<>(now.getDayOfMonth());
        for (int i = 0; i < now.getDayOfMonth(); i++) {
            daysOfMonth.add(Timestamp.valueOf(firstDayOfMonth.plusDays(i)));
        }
        return daysOfMonth;
    }
}
