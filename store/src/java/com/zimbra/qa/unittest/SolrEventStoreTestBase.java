package com.zimbra.qa.unittest;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.SolrEventStore;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Test;

import com.zimbra.cs.event.analytics.contact.ContactAnalytics;
import com.zimbra.cs.event.analytics.contact.ContactFrequencyGraphDataPoint;
import com.zimbra.cs.event.Event;

import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.logger.SolrEventCallback;
import com.zimbra.cs.index.solr.AccountCollectionLocator;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

public abstract class SolrEventStoreTestBase {

    protected static String ACCOUNT_ID_1 = "test-id-1";
    protected static String ACCOUNT_ID_2 = "test-id-2";
    protected static String JOINT_COLLECTION_NAME = "events_test";
    protected static String ACCOUNT_COLLECTION_PREFIX = "events_test";
    protected static String CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID;
    protected SolrClient client;

    protected List<Event> getSentEvents(String accountId, String dsId, int num, int startMsgId) {
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

    protected abstract SolrQuery newQuery(String coreOrCollection);

    protected abstract SolrDocumentList executeRequest(String coreOrCollection, QueryRequest req) throws Exception;

    protected static String getAccountCollectionName(String accountId) {
        return new AccountCollectionLocator(ACCOUNT_COLLECTION_PREFIX).getCoreName(accountId);
    }

    protected SolrDocumentList queryEvents(String collection) throws Exception {
        return queryEvents(collection, null, null);
    }

    protected SolrDocumentList queryEvents(String collection, String accountId) throws Exception {
        return queryEvents(collection, accountId, null);
    }

    protected SolrDocumentList queryEvents(String coreOrCollection, String accountId, String dsId) throws Exception {
        SolrQuery query = newQuery(coreOrCollection);
        query.setQuery("ev_type:*");
        if (accountId != null) {
            query.addFilterQuery("acct_id:"+accountId);
        }
        if (dsId != null) {
            query.addFilterQuery("datasource_id:"+dsId);
        }

        QueryRequest req = new QueryRequest(query, METHOD.POST);
        return executeRequest(coreOrCollection, req);
    }

    protected abstract void commit(String coreOrCollection) throws Exception;

    protected abstract SolrEventCallback getAccountCoreCallback() throws Exception;

    protected abstract SolrEventCallback getCombinedCoreCallback() throws Exception;

    protected abstract SolrEventStore getCombinedEventStore(String accountId) throws Exception;

    protected abstract SolrEventStore getAccountEventStore(String accountId) throws Exception;

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

            SolrEventStore eventStore = getCombinedEventStore(ACCOUNT_ID_1);
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
            SolrEventStore eventStore = getAccountEventStore(ACCOUNT_ID_1);
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

            SolrEventStore eventStore1 = getCombinedEventStore(ACCOUNT_ID_1);
            eventStore1.deleteEvents();
            commit(JOINT_COLLECTION_NAME);
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_1);
            assertEquals("should see 0 results in joint collection for test-id-1", 0, (int) results.getNumFound());
            results = queryEvents(JOINT_COLLECTION_NAME, ACCOUNT_ID_2);
            assertEquals("should see 10 results in joint collection for test-id-2", 10, (int) results.getNumFound());

            SolrEventStore eventStore2 = getCombinedEventStore(ACCOUNT_ID_2);
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

            SolrEventStore eventStore = getAccountEventStore(ACCOUNT_ID_1);
            eventStore.deleteEvents();
            try {
                results = queryEvents(collectionName);
                fail("collection should be deleted");
            } catch (Exception e) {
                String msg = e.getMessage().toLowerCase();
                assertTrue(msg.contains("not found"));
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

    void testContactFrequencyCount(ContactAnalytics.ContactFrequencyTimeRange timeRange, SolrEventCallback eventCallback, String collectionName, SolrEventStore eventStore) throws Exception {
        List<Timestamp> timestamps = getTimestampsForContactFrequencyCountTest(timeRange);
        List<Event> events = new ArrayList<>(4);
        int i = 1;
        for (Timestamp timestamp : timestamps) {
            events.add(Event.generateSentEvent(ACCOUNT_ID_1, i++, "testSender@zcs-dev.test", "testRecipient@zcs-dev.test", "testDSId", null, timestamp.getTime()));
            events.add(Event.generateReceivedEvent(ACCOUNT_ID_1, i++, "testRecipient@zcs-dev.test","testSender@zcs-dev.test", "testDSId", timestamp.getTime()));
        }

        eventCallback.execute(ACCOUNT_ID_1, events);
        commit(collectionName);
        SolrDocumentList results = queryEvents(collectionName);
        assertEquals("should see " + timestamps.size() * 2 + " events in test-id-1 collection", timestamps.size() * 2, (int) results.getNumFound());

        Long expectedCountForEachEventType = ContactAnalytics.ContactFrequencyTimeRange.FOREVER.equals(timeRange) ? Long.valueOf(timestamps.size()) : Long.valueOf(timestamps.size() / 2);

        Long contactFrequencyCountForSentEmails = ContactAnalytics.getContactFrequency("testRecipient@zcs-dev.test", eventStore, ContactAnalytics.ContactFrequencyEventType.SENT, timeRange);
        assertEquals("Mismatch in frequency count for timeRange " + timeRange + " and eventType SENT", expectedCountForEachEventType, contactFrequencyCountForSentEmails);

        Long contactFrequencyCountForReceivedEmails = ContactAnalytics.getContactFrequency("testRecipient@zcs-dev.test", eventStore, ContactAnalytics.ContactFrequencyEventType.RECEIVED, timeRange);
        assertEquals("Mismatch in frequency count for timeRange " + timeRange + " and eventType RECEIVED", expectedCountForEachEventType, contactFrequencyCountForReceivedEmails);

        Long contactFrequencyCountCombined = ContactAnalytics.getContactFrequency("testRecipient@zcs-dev.test", eventStore, ContactAnalytics.ContactFrequencyEventType.COMBINED, timeRange);
        assertEquals("Mismatch in frequency count for timeRange " + timeRange + " and eventType COMBINED", Long.valueOf(expectedCountForEachEventType * 2), contactFrequencyCountCombined);
    }

    protected void testContactFrequencyGraph(ContactAnalytics.ContactFrequencyGraphTimeRange timeRange, SolrEventCallback eventCallback, String collectionName, SolrEventStore eventStore) throws Exception {
        List<Timestamp> timestamps = getTimestampForContactFrequencyGraphTest(timeRange, ZoneId.of("GMT-0800")); //LA time zone
        List<Event> events = new ArrayList<>(timestamps.size());
        int i = 1;
        for (Timestamp timestamp : timestamps) {
            events.add(Event.generateSentEvent(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID, i++, "testSender@zcs-dev.test", "testRecipient@zcs-dev.test", "testDSId", null, timestamp.getTime()));
        }

        eventCallback.execute(CONTACT_FREQUENCY_GRAPH_TEST_ACCOUNT_ID, events);
        commit(collectionName);
        SolrDocumentList results = queryEvents(collectionName);
        assertEquals("should see " + timestamps.size() + " events in test-id-1 collection", timestamps.size(), (int) results.getNumFound());

        List<ContactFrequencyGraphDataPoint> contactFrequencyGraphDataPoints = ContactAnalytics.getContactFrequencyGraph("testRecipient@zcs-dev.test", timeRange, eventStore, -8 * 60);
        assertNotNull(contactFrequencyGraphDataPoints);
        assertEquals("The size of the result should be equal to the size of timestamps that we produced", timestamps.size(), contactFrequencyGraphDataPoints.size());
        for (ContactFrequencyGraphDataPoint dataPoint : contactFrequencyGraphDataPoints) {
            assertNotNull(dataPoint.getLabel());
            assertEquals("For each time range we should have 1 event. Failed for range starting " + dataPoint.getLabel(), 1, dataPoint.getValue());
        }
    }

    protected void testPercentageOpenedEmails(SolrEventCallback eventCallback, String collectionName, SolrEventStore eventStore) throws Exception {
        List<Timestamp> timestamps = getTimestampsForContactFrequencyCountTest(ContactAnalytics.ContactFrequencyTimeRange.LAST_WEEK);
        List<Event> events = new ArrayList<>(timestamps.size());
        //generate 15 received events
        for (int i = 0; i < timestamps.size(); i++) {
            events.add(Event.generateReceivedEvent(ACCOUNT_ID_1, i, "testSender@zcs-dev.test", "testRecipient@zcs-dev.test", "testDSId", timestamps.get(i).getTime()));
        }
        //generate 9 read events
        for (int i = 0; i < timestamps.size() - 6; i++) {
            events.add(Event.generateReadEvent(ACCOUNT_ID_1, i, "testSender@zcs-dev.test", "testDSId", timestamps.get(i).getTime()));
        }

        eventCallback.execute(ACCOUNT_ID_1, events);
        commit(collectionName);
        SolrDocumentList results = queryEvents(collectionName);
        assertEquals("should see 24 events in test-id-1 collection", 24, (int) results.getNumFound());

        Double percentageOpenedEmails = ContactAnalytics.getPercentageOpenedEmails("testSender@zcs-dev.test", eventStore);
        assertNotNull(percentageOpenedEmails);
        assertEquals("Mismatch in percentage opened emails", Double.valueOf(0.6), percentageOpenedEmails);
    }

    protected void testGetAvgTimeToOpenEmailForAccount(SolrEventCallback eventCallback, String collectionName, SolrEventStore eventStore) throws Exception {
        eventCallback.execute(ACCOUNT_ID_1, getTestEventsForOpenEmails());
        commit(collectionName);
        SolrDocumentList results = queryEvents(collectionName);
        assertEquals("should see 8 events in test-id-1 collection", 8, (int) results.getNumFound());

        Double avgTimeToOpenEmails = ContactAnalytics.getAvgTimeToOpenEmailForAccount(eventStore);
        assertNotNull(avgTimeToOpenEmails);
        assertEquals("Mismatch in average time to opened emails", Double.valueOf(250), avgTimeToOpenEmails);
    }

    protected void testGetAvgTimeToOpenEmail(SolrEventCallback eventCallback, String collectionName, SolrEventStore eventStore) throws Exception {
        eventCallback.execute(ACCOUNT_ID_1, getTestEventsForOpenEmails());
        commit(collectionName);
        SolrDocumentList results = queryEvents(collectionName);
        assertEquals("should see 8 events in test-id-1 collection", 8, (int) results.getNumFound());

        Double avgTimeToOpenEmails = ContactAnalytics.getAvgTimeToOpenEmail("test1@zcs-dev.test", eventStore);
        assertNotNull(avgTimeToOpenEmails);
        assertEquals("Mismatch in average time to opened emails", Double.valueOf(150), avgTimeToOpenEmails);
    }

    protected void testGetRatioOfAvgTimeToOpenEmailToGlobalAvg(SolrEventCallback eventCallback, String collectionName, SolrEventStore eventStore) throws Exception {
        eventCallback.execute(ACCOUNT_ID_1, getTestEventsForOpenEmails());
        commit(collectionName);
        SolrDocumentList results = queryEvents(collectionName);
        assertEquals("should see 8 events in test-id-1 collection", 8, (int) results.getNumFound());

        Double avgTimeToOpenEmails = ContactAnalytics.getRatioOfAvgTimeToOpenEmailToGlobalAvg("test1@zcs-dev.test", eventStore);
        assertNotNull(avgTimeToOpenEmails);
        assertEquals("Mismatch in average time to opened emails", Double.valueOf(0.6), avgTimeToOpenEmails);
    }

    protected void testPercentageRepliedEmails(SolrEventCallback eventCallback, String collectionName, SolrEventStore eventStore) throws Exception {
        List<Event> events = new ArrayList<>(6);
        long timestamp = Timestamp.valueOf(LocalDateTime.now()).getTime();
        events.add(Event.generateReceivedEvent(ACCOUNT_ID_1, 1, "testSender@zcs-dev.test", "testRecipient@zcs-dev.test", "testDSId", timestamp));
        events.add(Event.generateRepliedEvent(ACCOUNT_ID_1, 1, "testSender@zcs-dev.test", "testDSId", timestamp));
        events.add(Event.generateReceivedEvent(ACCOUNT_ID_1, 2, "testSender@zcs-dev.test", "testRecipient@zcs-dev.test", "testDSId", timestamp));
        events.add(Event.generateRepliedEvent(ACCOUNT_ID_1, 2, "testSender@zcs-dev.test", "testDSId", timestamp));
        events.add(Event.generateReceivedEvent(ACCOUNT_ID_1, 3, "testSender@zcs-dev.test", "testRecipient@zcs-dev.test", "testDSId", timestamp));
        events.add(Event.generateReceivedEvent(ACCOUNT_ID_1, 4, "testSender@zcs-dev.test", "testRecipient@zcs-dev.test", "testDSId", timestamp));

        eventCallback.execute(ACCOUNT_ID_1, events);
        commit(collectionName);
        SolrDocumentList results = queryEvents(collectionName);
        assertEquals("should see 6 events in test-id-1 collection", 6, (int) results.getNumFound());

        Double percentageRepliedEmails = ContactAnalytics.getPercentageRepliedEmails("testSender@zcs-dev.test", eventStore);
        assertNotNull(percentageRepliedEmails);
        assertEquals("Mismatch in average time to opened emails", Double.valueOf(0.5), percentageRepliedEmails);
    }

    private List<Event> getTestEventsForOpenEmails() {
        List<Event> events = new ArrayList<>(6);
        events.add(Event.generateSeenEvent(ACCOUNT_ID_1, 1, "test1@zcs-dev.test", "testDSId", 100000L));
        events.add(Event.generateReadEvent(ACCOUNT_ID_1, 1, "test1@zcs-dev.test", "testDSId", 200000L));
        events.add(Event.generateSeenEvent(ACCOUNT_ID_1, 2, "test1@zcs-dev.test", "testDSId", 100000L));
        events.add(Event.generateReadEvent(ACCOUNT_ID_1, 2, "test1@zcs-dev.test", "testDSId", 300000L));
        events.add(Event.generateSeenEvent(ACCOUNT_ID_1, 3, "test2@zcs-dev.test", "testDSId", 100000L));
        events.add(Event.generateReadEvent(ACCOUNT_ID_1, 3, "test2@zcs-dev.test", "testDSId", 400000L));
        events.add(Event.generateSeenEvent(ACCOUNT_ID_1, 4, "test3@zcs-dev.test", "testDSId", 100000L));
        events.add(Event.generateReadEvent(ACCOUNT_ID_1, 4, "test3@zcs-dev.test", "testDSId", 500000L));
        return events;
    }

    private List<Timestamp> getTimestampsForContactFrequencyCountTest(ContactAnalytics.ContactFrequencyTimeRange timeRange) throws ServiceException {
        switch (timeRange) {
        case LAST_DAY:
            return getTimestampsForLast2Days();
        case LAST_WEEK:
            return getTimestampsForLast2Weeks();
        case LAST_MONTH:
            return getTimestampsForLast2Months();
        case FOREVER:
            return getTimestampsForEachMonthInCurrentYear(ZonedDateTime.now().getZone());
        default:
            throw ServiceException.INVALID_REQUEST("Time range not supported " + timeRange, null);
        }
    }

    private List<Timestamp> getTimestampForContactFrequencyGraphTest(ContactAnalytics.ContactFrequencyGraphTimeRange timeRange, ZoneId zoneId) throws ServiceException {
        switch (timeRange) {
        case CURRENT_MONTH:
            return getTimestampsForEachDayInCurrentMonth(zoneId);
        case LAST_SIX_MONTHS:
            return getTimestampsInEachWeekForLast6Months(zoneId);
        case CURRENT_YEAR:
            return getTimestampsForEachMonthInCurrentYear(zoneId);
        default:
            throw ServiceException.INVALID_REQUEST("Time range not supported " + timeRange, null);
        }
    }

    private List<Timestamp> getTimestampsForLast2Days() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoDaysBack = now.minusDays(1);
        List<Timestamp> last2Days = new ArrayList<>();
        for (int i = 0; i < ChronoUnit.DAYS.between(twoDaysBack, now) + 1; i++) {
            last2Days.add(Timestamp.valueOf(twoDaysBack.plusDays(i)));
        }
        return last2Days;
    }

    private List<Timestamp> getTimestampsForLast2Weeks() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoWeeksBack = now.minusDays(14);
        List<Timestamp> last2Weeks = new ArrayList<>();
        for (int i = 0; i < ChronoUnit.DAYS.between(twoWeeksBack, now) + 1; i++) {
            last2Weeks.add(Timestamp.valueOf(twoWeeksBack.plusDays(i)));
        }
        return last2Weeks;
    }

    private List<Timestamp> getTimestampsForLast2Months() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthBack = now.minusMonths(1);
        //Need to make sure even number of days are returned
        long numberOfDaysBetween1Month = ChronoUnit.DAYS.between(oneMonthBack, now);
        LocalDateTime twoMonthsBack = oneMonthBack.minusDays(numberOfDaysBetween1Month);
        List<Timestamp> last2Months = new ArrayList<>();
        for (int i = 0; i < ChronoUnit.DAYS.between(twoMonthsBack, now) + 1; i++) {
            last2Months.add(Timestamp.valueOf(twoMonthsBack.plusDays(i)));
        }
        return last2Months;
    }

    private List<Timestamp> getTimestampsForEachMonthInCurrentYear(ZoneId zoneId) {
        ZonedDateTime now = ZonedDateTime.now(zoneId).with(TemporalAdjusters.firstDayOfMonth());
        ZonedDateTime firstDayOfCurrentYear = now.with(TemporalAdjusters.firstDayOfYear());
        List<Timestamp> monthsInCurrentYear = new ArrayList<>();
        for (int i = 0; i < ChronoUnit.MONTHS.between(firstDayOfCurrentYear, now) + 1; i++) {
            monthsInCurrentYear.add(Timestamp.from(firstDayOfCurrentYear.plusMonths(i).toInstant()));
        }
        return monthsInCurrentYear;
    }

    private List<Timestamp> getTimestampsInEachWeekForLast6Months(ZoneId zoneId) {
        ZonedDateTime firstDayOfCurrentWeek = ZonedDateTime.now(zoneId).with(WeekFields.of(Locale.US).dayOfWeek(), 1);
        ZonedDateTime firstDayOfWeek6MonthsBack = firstDayOfCurrentWeek.minusMonths(6).with(WeekFields.of(Locale.US).dayOfWeek(), 1);
        List<Timestamp> weeksIn6Months = new ArrayList<>();
        for (int i = 0; i < ChronoUnit.WEEKS.between(firstDayOfWeek6MonthsBack, ZonedDateTime.now(zoneId)) + 1; i++) {
            weeksIn6Months.add(Timestamp.from(firstDayOfWeek6MonthsBack.plusDays(i * 7).toInstant()));
        }
        return weeksIn6Months;
    }

    private List<Timestamp> getTimestampsForEachDayInCurrentMonth(ZoneId zoneId) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        List<Timestamp> daysOfMonth = new ArrayList<>(now.getDayOfMonth());
        for (int i = 0; i < now.getDayOfMonth(); i++) {
            daysOfMonth.add(Timestamp.from(firstDayOfMonth.plusDays(i).toInstant()));
        }
        return daysOfMonth;
    }

    protected List<ContactAnalytics.ContactFrequencyGraphTimeRange> getContactFrequencyGraphTimeRanges() {
        List<ContactAnalytics.ContactFrequencyGraphTimeRange> timeRanges = new ArrayList<>(3);
        timeRanges.add(ContactAnalytics.ContactFrequencyGraphTimeRange.CURRENT_MONTH);
        timeRanges.add(ContactAnalytics.ContactFrequencyGraphTimeRange.LAST_SIX_MONTHS);
        timeRanges.add(ContactAnalytics.ContactFrequencyGraphTimeRange.CURRENT_YEAR);
        return timeRanges;
    }

    protected List<ContactAnalytics.ContactFrequencyTimeRange> getContactFrequencyCountTimeRanges() {
        List<ContactAnalytics.ContactFrequencyTimeRange> timeRanges = new ArrayList<>(3);
        timeRanges.add(ContactAnalytics.ContactFrequencyTimeRange.LAST_DAY);
        timeRanges.add(ContactAnalytics.ContactFrequencyTimeRange.LAST_WEEK);
        timeRanges.add(ContactAnalytics.ContactFrequencyTimeRange.LAST_MONTH);
        timeRanges.add(ContactAnalytics.ContactFrequencyTimeRange.FOREVER);
        return timeRanges;
    }
}
