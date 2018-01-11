package com.zimbra.cs.event.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.analytics.AccountEventMetrics.MetricKey;
import com.zimbra.cs.event.analytics.ContactFrequencyMetric.ContactFrequencyParams;
import com.zimbra.cs.event.analytics.EventDifferenceMetric.EventDifferenceParams;
import com.zimbra.cs.event.analytics.EventMetric.MetricInitializer;
import com.zimbra.cs.event.analytics.EventMetric.MetricType;
import com.zimbra.cs.event.analytics.IncrementableMetric.Increment;
import com.zimbra.cs.event.analytics.RatioMetric.RatioIncrement;
import com.zimbra.cs.event.analytics.ValueMetric.IntIncrement;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyEventType;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyTimeRange;
import com.zimbra.cs.event.logger.BatchingEventLogger;
import com.zimbra.cs.event.logger.EventMetricCallback;

public class EventMetricTest {

    private static final String CONTACT_1 = "test1@zimbra.com";
    private static final String CONTACT_2 = "test2@zimbra.com";
    private static final String ACCT_ID = "accountId";
    private BatchingEventLogger logger;
    private AccountEventMetrics metrics;

    @Before
    public void setUp() throws Exception {
        logger = new BatchingEventLogger(10, 0, new EventMetricCallback());
        metrics = EventMetricManager.getInstance().getMetrics(ACCT_ID);
    }

    @Test
    public void testValueMetric() throws Exception {

        MetricInitializer<ValueMetric, Integer, IntIncrement> initializer = new MetricInitializer<ValueMetric, Integer, IntIncrement>() {

            @Override
            public ValueMetric getInitialData() {
                return new ValueMetric(0);
            }

            @Override
            public long getMetricLifetime() {
                return 0;
            }
        };

        EventMetric<ValueMetric, Integer, IntIncrement> metric = new EventMetric<ValueMetric, Integer, IntIncrement>("testAccountID", null, initializer) {

            @Override
            protected IntIncrement getIncrement(List<Event> events) throws ServiceException {
                return new IntIncrement(1);
            }
        };

        assertEquals("initial value should be 0", (Integer)0, metric.getValue());
        metric.increment(null);
        metric.increment(null);
        assertEquals("value after two increments should be 2", (Integer)2, metric.getValue());
    }

    @Test
    public void testRatioMetric() throws Exception {

        MetricInitializer<RatioMetric, Double, RatioIncrement> initializer = new MetricInitializer<RatioMetric, Double, RatioIncrement>() {

            @Override
            public RatioMetric getInitialData() {
                return new RatioMetric(0d, 1);
            }

            @Override
            public long getMetricLifetime() {
                return 0;
            }
        };

        EventMetric<RatioMetric, Double, RatioIncrement> metric = new EventMetric<RatioMetric, Double, RatioIncrement>("testAccountID", null, initializer) {

            @Override
            protected RatioIncrement getIncrement(List<Event> events) throws ServiceException {
                return new RatioIncrement(1d, 2);
            }
        };

        assertEquals("initial value should be 0", new Double(0), metric.getValue());
        metric.increment(null);
        assertEquals("new value should be 1/3", new Double(1d/3), metric.getValue());
        metric.increment(null);
        assertEquals("new value should be 2/5", new Double(2d/5), metric.getValue());
    }

    private void logContactFrequencyEvents(String contactEmail) {
        logger.log(Event.generateEvent(ACCT_ID, 1, contactEmail, "me@zimbra.com", EventType.RECEIVED, null, null, System.currentTimeMillis()));
        logger.log(Event.generateSentEvent(ACCT_ID, 2, "me@zimbra.com", contactEmail, null, null, System.currentTimeMillis()));
        logger.log(Event.generateSentEvent(ACCT_ID, 3, "me@zimbra.com", "other@zimbra.com", null, null, System.currentTimeMillis()));
        logger.log(Event.generateSentEvent("otherAccountId", 4, "me@zimbra.com", contactEmail, null, null, System.currentTimeMillis()));
        logger.sendAllBatched();
    }

    private void logMsgEvents(String contactEmail, int startingMsgId, int numEvents, EventType eventType) {
        logMsgEvents(contactEmail, startingMsgId, numEvents, eventType, System.currentTimeMillis());
    }

    private void logMsgEvents(String contactEmail, int startingMsgId, int numEvents, EventType eventType, long timestamp) {
        for (int i=0; i<numEvents; i++) {
            logger.log(Event.generateEvent(ACCT_ID, startingMsgId+i, contactEmail, "me@zimbra.com", eventType, null, null, timestamp));
        }
        //log an event for a different account id to make it doesn't affect the metrics
        logger.log(Event.generateEvent("other", startingMsgId, contactEmail, "me@zimbra.com", eventType, null, null, timestamp));
        logger.sendAllBatched();
    }

    @Test
    public void testContactFrequency() throws Exception {

        DummyInitializer<ValueMetric, Integer, IntIncrement> initializer = new DummyValueInitializer();

        ContactFrequencyParams params = new ContactFrequencyParams(CONTACT_1, ContactFrequencyTimeRange.FOREVER, ContactFrequencyEventType.COMBINED);
        params.setInitializer(initializer);
        MetricKey<ValueMetric, Integer, IntIncrement> key = new MetricKey<ValueMetric, Integer, IntIncrement>(MetricType.CONTACT_FREQUENCY, params);
        EventMetric<ValueMetric, Integer, IntIncrement> metric = metrics.getMetric(key);

        assertTrue("initializer.getInitialValue() should have been triggered", initializer.isInitialized());
        assertEquals("initial value should be 0", (Integer)0, metric.getValue());

        logContactFrequencyEvents(CONTACT_1);

        assertEquals("new value should be 2", (Integer)2, metric.getValue());
    }

    @Test
    public void testReadRate() throws Exception {

        DummyInitializer<RatioMetric, Double, RatioIncrement> initializer = new DummyRatioInitializer();

        EventDifferenceParams contactParams = new EventDifferenceParams(EventType.READ, EventType.SEEN, CONTACT_1);
        contactParams.setInitializer(initializer);
        MetricKey<RatioMetric, Double, RatioIncrement> key = new MetricKey<RatioMetric, Double, RatioIncrement>(MetricType.EVENT_RATIO, contactParams);
        EventMetric<RatioMetric, Double, RatioIncrement> contactReadRatio = metrics.getMetric(key);

        EventDifferenceParams globalParams = new EventDifferenceParams(EventType.READ, EventType.SEEN);
        globalParams.setInitializer(initializer);
        key = new MetricKey<RatioMetric, Double, RatioIncrement>(MetricType.EVENT_RATIO, globalParams);
        EventMetric<RatioMetric, Double, RatioIncrement> globalReadRatio = metrics.getMetric(key);

        assertTrue("initializer.getInitialValue() should have been triggered", initializer.isInitialized());
        assertEquals("initial contact1 read ratio should be 0", new Double(0), contactReadRatio.getValue());
        assertEquals("initial global read ratio should be 0", new Double(0), globalReadRatio.getValue());

        logMsgEvents(CONTACT_1, 1, 3, EventType.SEEN);
        logMsgEvents(CONTACT_1, 1, 2, EventType.READ);
        logMsgEvents(CONTACT_2, 10, 3, EventType.SEEN);
        logMsgEvents(CONTACT_2, 10, 1, EventType.READ);

        assertEquals("new contact1 read ratio should be 2/3", new Double(2d/3), contactReadRatio.getValue());
        assertEquals("new global read ratio should be 1/2", new Double(1d/2), globalReadRatio.getValue());
    }

    @Test
    public void testTimeToOpen() throws Exception {
        DummyInitializer<RatioMetric, Double, RatioIncrement> initializer = new DummyRatioInitializer();

        EventDifferenceParams contactParams = new EventDifferenceParams(EventType.SEEN, EventType.READ, CONTACT_1);
        contactParams.setInitializer(initializer);
        MetricKey<RatioMetric, Double, RatioIncrement> key = new MetricKey<RatioMetric, Double, RatioIncrement>(MetricType.TIME_DELTA, contactParams);
        EventMetric<RatioMetric, Double, RatioIncrement> contactTTO = metrics.getMetric(key);

        EventDifferenceParams globalParams = new EventDifferenceParams(EventType.SEEN, EventType.READ);
        globalParams.setInitializer(initializer);
        key = new MetricKey<RatioMetric, Double, RatioIncrement>(MetricType.TIME_DELTA, globalParams);
        EventMetric<RatioMetric, Double, RatioIncrement> globalTTO = metrics.getMetric(key);

        assertTrue("initializer.getInitialValue() should have been triggered", initializer.isInitialized());
        assertEquals("initial contact1 TTO should be 0", new Double(0), contactTTO.getValue());
        assertEquals("initial global TTO should be 0", new Double(0), globalTTO.getValue());

        long timestamp = System.currentTimeMillis();
        //1 second difference for contact1
        logMsgEvents(CONTACT_1, 1, 1, EventType.SEEN, timestamp-1000);
        logMsgEvents(CONTACT_1, 1, 1, EventType.READ, timestamp);

        //2 second difference for contact2
        logMsgEvents(CONTACT_2, 2, 1, EventType.SEEN, timestamp-2000);
        logMsgEvents(CONTACT_2, 2, 1, EventType.READ, timestamp);

        assertEquals("new contact1 TTO should be 1 second", new Double(1), contactTTO.getValue());
        assertEquals("new global TTO should be 1.5 seconds", new Double(1.5), globalTTO.getValue());
    }

    @Test
    public void testReplyRate() throws Exception {
        DummyInitializer<RatioMetric, Double, RatioIncrement> initializer = new DummyRatioInitializer();

        EventDifferenceParams contactParams = new EventDifferenceParams(EventType.REPLIED, EventType.SEEN, CONTACT_1);
        contactParams.setInitializer(initializer);
        MetricKey<RatioMetric, Double, RatioIncrement> key = new MetricKey<RatioMetric, Double, RatioIncrement>(MetricType.EVENT_RATIO, contactParams);
        EventMetric<RatioMetric, Double, RatioIncrement> contactReplyRate = metrics.getMetric(key);

        EventDifferenceParams globalParams = new EventDifferenceParams(EventType.REPLIED, EventType.SEEN);
        globalParams.setInitializer(initializer);
        key = new MetricKey<RatioMetric, Double, RatioIncrement>(MetricType.EVENT_RATIO, globalParams);
        EventMetric<RatioMetric, Double, RatioIncrement> globalReplyRate = metrics.getMetric(key);

        assertTrue("initializer.getInitialValue() should have been triggered", initializer.isInitialized());
        assertEquals("initial contact1 reply rate should be 0", new Double(0), contactReplyRate.getValue());
        assertEquals("initial global reply rate should be 0", new Double(0), globalReplyRate.getValue());

        logMsgEvents(CONTACT_1, 1, 6, EventType.SEEN);
        logMsgEvents(CONTACT_1, 1, 1, EventType.REPLIED);
        logMsgEvents(CONTACT_2, 10, 2, EventType.SEEN);
        logMsgEvents(CONTACT_2, 10, 1, EventType.REPLIED);

        assertEquals("new contact1 reply rate should be 1/6", new Double(1d/6), contactReplyRate.getValue());
        assertEquals("new global reply rate should be 1/4", new Double(1d/4), globalReplyRate.getValue());
    }

    private static abstract class DummyInitializer<M extends IncrementableMetric<T, I>, T, I extends Increment> extends MetricInitializer<M, T, I> {

        protected boolean initialized = false;

        public boolean isInitialized() {
            return initialized;
        }
    }

    private static class DummyValueInitializer extends DummyInitializer<ValueMetric, Integer, IntIncrement> {

        @Override
        public ValueMetric getInitialData() throws ServiceException {
            initialized = true;
            return new ValueMetric(0);
        }

        @Override
        public long getMetricLifetime() {
            return 0;
        }
    }

    private static class DummyRatioInitializer extends DummyInitializer<RatioMetric, Double, RatioIncrement> {

        @Override
        public RatioMetric getInitialData() throws ServiceException {
            initialized = true;
            return new RatioMetric(0d, 0);
        }

        @Override
        public long getMetricLifetime() {
            return 0;
        }
    }
}
