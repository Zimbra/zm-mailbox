package com.zimbra.cs.event.logger;

import static org.junit.Assert.*;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;

import com.google.common.cache.Cache;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.SolrEventDocument;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchedEvents;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchedEventCallback;;

public class SolrEventLoggerTest {

    private static final String ACCOUNT_ID_1 = "test_id_1";
    private static final String ACCOUNT_ID_2 = "test_id_2";
    private static final String RECIPIENT = "test_recipient";
    private static final String SENDER = "test_sender";
    private static DummyBatchEventCallback callback;

    @Before
    public void setUp() throws Exception {
        callback = new DummyBatchEventCallback();
    }

    private BatchingEventLogger getLogger(int batchSize, long expireMillis) {
        return new BatchingEventLogger(batchSize, expireMillis, callback);
    }

    public Event generateSentEvent(String accountId, long timestamp, String recipient) {
        Map<EventContextField, Object> context = new HashMap<EventContextField, Object>();
        context.put(EventContextField.RECEIVER, recipient);
        return new Event(accountId, EventType.SENT, timestamp, context);
    }

    public Event generateReceivedEvent(String accountId, long timestamp, String sender) {
        Map<EventContextField, Object> context = new HashMap<EventContextField, Object>();
        context.put(EventContextField.SENDER, sender);
        return new Event(accountId, EventType.RECEIVED, timestamp, context);
    }

    private void checkEventType(SolrInputDocument doc, EventType type) throws Exception {
        assertEquals("incorrect ev_type value", type.toString(), doc.getFieldValue("ev_type"));
    }

    private void checkTimestamp(SolrInputDocument doc, long timestamp) throws Exception {
        String fieldValue = (String) doc.getFieldValue("ev_timestamp");
        String formatted = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(timestamp));
        assertEquals("incorrect timestamp", formatted, fieldValue);
    }

    private void checkDynamicFieldValue(SolrInputDocument doc, String field, String value) throws Exception {
        assertEquals(String.format("incorrect %s value", field), value, doc.getFieldValue(field));
    }

    private void checkNumFields(SolrInputDocument doc, int numFields) throws Exception {
        assertEquals("incorrect number of fields", numFields, doc.getFieldNames().size());
    }

    @Test
    public void testSolrEventDocuments() throws Exception {
        long timestamp = System.currentTimeMillis();

        SolrInputDocument sent = new SolrEventDocument(generateSentEvent(ACCOUNT_ID_1, timestamp, RECIPIENT)).getDocument();
        ZimbraLog.test.info(sent);
        checkNumFields(sent, 3);
        checkEventType(sent, EventType.SENT);
        checkTimestamp(sent, timestamp);
        checkDynamicFieldValue(sent, "receiver_s", RECIPIENT);

        SolrInputDocument received = new SolrEventDocument(generateReceivedEvent(ACCOUNT_ID_1, timestamp, SENDER)).getDocument();
        checkNumFields(received, 3);
        checkEventType(received, EventType.RECEIVED);
        checkTimestamp(received, timestamp);
        checkDynamicFieldValue(received, "sender_s", SENDER);
    }

    @Test
    public void testBatchSize() throws Exception {
        int batchSize = 5;
        BatchingEventLogger logger = getLogger(batchSize, 0);
        Cache<String, BatchedEvents> cache = logger.getBatchedEventCache();
        long timestamp = System.currentTimeMillis();
        Event event1 = generateSentEvent(ACCOUNT_ID_1, timestamp, RECIPIENT);
        Event event2 = generateSentEvent(ACCOUNT_ID_2, timestamp, RECIPIENT);
        for (int i = 0; i < batchSize-1; i++) {
            logger.log(event1);
        }
        assertTrue(cache.asMap().containsKey(ACCOUNT_ID_1));
        assertEquals("batch size should be 4", batchSize-1, cache.asMap().get(ACCOUNT_ID_1).getCurBatchSize());
        assertTrue("no requests should have been flushed", callback.sent.isEmpty());
        logger.log(event1); //reach batch size
        assertEquals("one request should have been flushed", 1, callback.sent.size());
        assertEquals("request should be to account1", ACCOUNT_ID_1, callback.sent.get(0).getFirst());
        assertEquals("request should have 5 Solr documents", (Integer) 5, callback.sent.get(0).getSecond());
        assertEquals("batch size should be 0", 0, cache.asMap().get(ACCOUNT_ID_1).getCurBatchSize());
        for (int i = 0; i < batchSize-1; i++) {
            logger.log(event2);
        }
        assertEquals("batch size should be 4", batchSize-1, cache.asMap().get(ACCOUNT_ID_2).getCurBatchSize());
        logger.sendAllBatched();
        assertEquals("two requests should have been flushed", 2, callback.sent.size());
        assertEquals("second request should be to account2", ACCOUNT_ID_2, callback.sent.get(1).getFirst());
        assertEquals("second request should have 4 Solr documents", (Integer) 4, callback.sent.get(1).getSecond());
    }

    @Test
    public void testBatchExpiry() throws Exception {
        int batchSize = 10;
        int timeout = 1000;
        long timestamp = System.currentTimeMillis();
        BatchingEventLogger logger = getLogger(batchSize, timeout);
        Cache<String, BatchedEvents> cache = logger.getBatchedEventCache();
        Event event = generateSentEvent(ACCOUNT_ID_1, timestamp, RECIPIENT);
        for (int i = 0; i < 5; i++) {
            logger.log(event);
        }

        Thread.sleep(timeout);
        logger.cleanCache();
        assertEquals("one request should have been flushed", 1, callback.sent.size());
        assertEquals("request should have 5 Solr documents", (Integer) 5, callback.sent.get(0).getSecond());
        assertFalse("BatchedEvents cache entry should not exist", cache.asMap().containsKey(ACCOUNT_ID_1));
    }

    private static class DummyBatchEventCallback implements BatchedEventCallback {
        private List<Pair<String, Integer>> sent = new ArrayList<Pair<String, Integer>>();

        @Override
        public void execute(String accountId, List<Event> events) {
            ZimbraLog.test.info("executing DummyBatchEventCallback for account %s (%d events)", accountId, events.size());
            sent.add(new Pair<String, Integer>(accountId, events.size()));
        }

        @Override
        public void close() throws IOException {}

    }
}
