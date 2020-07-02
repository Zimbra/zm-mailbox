package com.zimbra.cs.ml.feature;

import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.analytics.EventMetric.MetricInitializer;
import com.zimbra.cs.event.analytics.EventMetricManager;
import com.zimbra.cs.event.analytics.RatioMetric;
import com.zimbra.cs.event.analytics.RatioMetric.RatioIncrement;
import com.zimbra.cs.event.analytics.ValueMetric;
import com.zimbra.cs.event.analytics.ValueMetric.IntIncrement;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyEventType;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyTimeRange;
import com.zimbra.cs.event.logger.BatchingEventLogger;
import com.zimbra.cs.event.logger.EventMetricCallback;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.ml.feature.FeatureParam.ParamKey;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;
import com.zimbra.cs.ml.feature.NumRecipientsFeatureFactory.RecipientCountType;
import com.zimbra.cs.util.JMSession;
import com.zimbra.qa.unittest.TestUtil;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class FeatureFactoryTest {

    private final String USER = "testFeatureFactories@zimbra.com";
    private Mailbox mbox;
    private BatchingEventLogger logger;
    private static final String contactEmail = "test@zimbra.com";

    @Before
    public void setUp() throws Exception {

        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount(USER, "test123", new HashMap<String, Object>());
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        logger = new BatchingEventLogger(10, 0, new EventMetricCallback());
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
        EventMetricManager.getInstance().clearMetrics();
    }

    private void logReadTimeDelta(List<Event> eventList, String acctId, int msgId, long delta) {
        long readTime = System.currentTimeMillis();
        long seenTime = readTime - delta;
        eventList.add(Event.generateEvent(acctId, msgId, contactEmail, USER, EventType.SEEN, null, null, seenTime));
        eventList.add(Event.generateEvent(acctId, msgId, contactEmail, USER, EventType.READ, null, null, readTime));
    }

    private void logMsgEvents(String acctId) {
        List<Event> events = new ArrayList<Event>();
        //log 10 received events, 5 sent events, 8 seen events, 4 read events, 2 replied events
        for (int i=0; i<10; i++) {
            events.add(Event.generateEvent(acctId, i, contactEmail, USER, EventType.RECEIVED, null, null, System.currentTimeMillis()));
            if (i < 2) {
                logReadTimeDelta(events, acctId, 1, 1000L);
                events.add(Event.generateEvent(acctId, i, contactEmail, USER, EventType.REPLIED, null, null, System.currentTimeMillis()));
            } else if (i < 4){
                logReadTimeDelta(events, acctId, 0, 2000L);
            } else if (i < 8) {
                events.add(Event.generateEvent(acctId, i, contactEmail, USER, EventType.SEEN, null, null, System.currentTimeMillis()));
            }
        }

        for (int i=0; i<5; i++) {
            events.add(Event.generateSentEvent(acctId, i+20, USER, contactEmail, null, null, System.currentTimeMillis()));
        }

        for (Event event: events) {
            logger.log(event);
        }
        logger.sendAllBatched();
    }

    @Test
    public void testConversationFeature() throws Exception {
        Message msg1 = TestUtil.addMessage(mbox, "a message");
        ConversationFeatureFactory factory = new ConversationFeatureFactory();
        Feature<Boolean> feature = factory.buildFeature(msg1);
        assertFalse("message 1 should not be part of conversation", feature.getFeatureValue());
        Message msg2 = TestUtil.addMessage(mbox, "a message");
        assertFalse("message 1 should be part of conversation", factory.buildFeature(msg1).getFeatureValue());
        assertFalse("message 2 should be part of conversation", factory.buildFeature(msg2).getFeatureValue());
    }

    @Test
    public void testContactFrequencyFeature() throws Exception {

        ContactFrequencyFeatureFactory receivedFactory = new ContactFrequencyFeatureFactory(ContactFrequencyTimeRange.FOREVER, ContactFrequencyEventType.RECEIVED);
        ContactFrequencyFeatureFactory sentFactory = new ContactFrequencyFeatureFactory(ContactFrequencyTimeRange.FOREVER, ContactFrequencyEventType.SENT);
        ContactFrequencyFeatureFactory combinedFactory = new ContactFrequencyFeatureFactory(ContactFrequencyTimeRange.FOREVER, ContactFrequencyEventType.COMBINED);

        //initialize to 0, bypassing event store
        MetricInitializer<ValueMetric, Integer, IntIncrement> initializer = new DummyValueInitializer();
        receivedFactory.setInitializer(initializer);
        sentFactory.setInitializer(initializer);
        combinedFactory.setInitializer(initializer);

        Message msg = TestUtil.addMessage(mbox, USER, contactEmail, "test msg", "msg body", System.currentTimeMillis());

        Feature<Integer> receivedFeature = receivedFactory.buildFeature(msg);
        Feature<Integer> sentFeature = sentFactory.buildFeature(msg);
        Feature<Integer> combinedFeature = combinedFactory.buildFeature(msg);
        assertEquals("initial recieved value should be 0", (Integer)0, receivedFeature.getFeatureValue());
        assertEquals("initial sent value should be 0", (Integer)0, sentFeature.getFeatureValue());
        assertEquals("initial combined value should be 0", (Integer)0, combinedFeature.getFeatureValue());

        logMsgEvents(msg.getAccountId());

        assertEquals("new received value should be 10", (Integer)10, receivedFactory.buildFeature(msg).getFeatureValue());
        assertEquals("new sent value should be 5", (Integer)5, sentFactory.buildFeature(msg).getFeatureValue());
        assertEquals("new combined value should be 15", (Integer)15, combinedFactory.buildFeature(msg).getFeatureValue());
    }

    @Test
    public void testReadRatioFeature() throws Exception {

        EventRatioFeatureFactory factory = new EventRatioFeatureFactory(EventType.READ, EventType.SEEN);
        factory.setInitializer(new DummyRatioInitializer());

        Message msg = TestUtil.addMessage(mbox, USER, contactEmail, "test msg", "msg body", System.currentTimeMillis());

        Feature<Double> feature = factory.buildFeature(msg);
        assertEquals("initial value should be 0", new Double(0), feature.getFeatureValue());

        logMsgEvents(msg.getAccountId());

        assertEquals("new value should be .5", new Double(0.5), factory.buildFeature(msg).getFeatureValue());
    }

    @Test
    public void testTimeToOpenFeature() throws Exception {
        EventTimeDeltaFeatureFactory factory = new EventTimeDeltaFeatureFactory(EventType.SEEN, EventType.READ);
        factory.setInitializer(new DummyRatioInitializer());

        Message msg = TestUtil.addMessage(mbox, USER, contactEmail, "test msg", "msg body", System.currentTimeMillis());

        Feature<Double> feature = factory.buildFeature(msg);
        assertEquals("initial value should be 0", new Double(0), feature.getFeatureValue());

        logMsgEvents(msg.getAccountId());

        assertEquals("new value should be 1500", new Double(1.5), factory.buildFeature(msg).getFeatureValue());
    }

    @Test
    public void testReplyRateFeature() throws Exception {
        EventRatioFeatureFactory factory = new EventRatioFeatureFactory(EventType.REPLIED, EventType.SEEN);
        factory.setInitializer(new DummyRatioInitializer());

        Message msg = TestUtil.addMessage(mbox, USER, contactEmail, "test msg", "msg body", System.currentTimeMillis());

        Feature<Double> feature = factory.buildFeature(msg);
        assertEquals("initial value should be 0", new Double(0), feature.getFeatureValue());

        logMsgEvents(msg.getAccountId());

        assertEquals("new value should be 2/3", new Double(0.25), factory.buildFeature(msg).getFeatureValue());
    }

    private Message generateIncomingMessage(String fieldUserIsOn) throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "test@zimbra.com");
        if (fieldUserIsOn != null) {
            mm.setHeader(fieldUserIsOn, mbox.getAccount().getName());
        }
        mm.setHeader("Subject", "test message");
        ParsedMessage pm = new ParsedMessage(mm, false);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        return mbox.addMessage(null, pm, dopt, null);
    }

    @Test
    public void testRecipientFieldFeature() throws Exception {
        RecipientFieldFeatureFactory factory = new RecipientFieldFeatureFactory();
        Message msg1 = generateIncomingMessage("To");
        Message msg2 = generateIncomingMessage("Cc");
        Message msg3 = generateIncomingMessage("Bcc");
        Message msg4 = generateIncomingMessage(null);
        assertEquals(new Integer(0), factory.buildFeature(msg1).getFeatureValue());
        assertEquals(new Integer(1), factory.buildFeature(msg2).getFeatureValue());
        assertEquals(new Integer(2), factory.buildFeature(msg3).getFeatureValue());
        assertEquals(new Integer(3), factory.buildFeature(msg4).getFeatureValue());
    }

    @Test
    public void testNumRecipientFieldFeature() throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "test@zimbra.com");
        mm.addHeader("To", "recip1@zimbra.com");
        mm.addHeader("To", "recip2@zimbra.com");
        mm.addHeader("Cc", "recip3@zimbra.com");
        mm.addHeader("Cc", "recip4@zimbra.com");
        mm.addHeader("Cc", "recip5@zimbra.com");
        mm.setHeader("Subject", "test message");
        ParsedMessage pm = new ParsedMessage(mm, false);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        Message msg = mbox.addMessage(null, pm, dopt, null);
        assertEquals("TO recipient count should be 2", (Integer) 2, new NumRecipientsFeatureFactory(RecipientCountType.TO).buildFeature(msg).getFeatureValue());
        assertEquals("CC recipient count should be 3", (Integer) 3, new NumRecipientsFeatureFactory(RecipientCountType.CC).buildFeature(msg).getFeatureValue());
        assertEquals("total recipient count should be 5", (Integer) 5, new NumRecipientsFeatureFactory(RecipientCountType.ALL).buildFeature(msg).getFeatureValue());
    }

    private void encodeDecodeFeatureSpec(FeatureSpec<Classifiable> spec) throws Exception {
        int numParams = spec.getParams().getNumParams();
        String encoded = spec.encode();
        FeatureSpec<Message> decoded = new FeatureSpec<>(encoded);
        assertEquals("wrong KnownFeature on decoded FeatureSpec", spec.getFeature(), decoded.getFeature());
        assertEquals("wrong number of params on decoded FeatureSpec", numParams, decoded.getParams().getNumParams());
        for (FeatureParam<?> param: spec.getParams().getParams()) {
            assertEquals("wrong FeatureParam value", param.getValue(), decoded.getParams().get(param.getKey(), null));
        }
    }

    @Test
    public void testEncodeFeatureSpecs() throws Exception {
        encodeDecodeFeatureSpec(new FeatureSpec<>(KnownFeature.NUM_RECIPIENTS));
        encodeDecodeFeatureSpec(new FeatureSpec<>(KnownFeature.IS_PART_OF_CONVERSATION));

        encodeDecodeFeatureSpec(new FeatureSpec<>(KnownFeature.COMBINED_FREQUENCY)
                .addParam(new FeatureParam<>(ParamKey.TIME_RANGE, ContactFrequencyTimeRange.LAST_DAY)));

        encodeDecodeFeatureSpec(new FeatureSpec<>(KnownFeature.RECEIVED_FREQUENCY)
                .addParam(new FeatureParam<>(ParamKey.TIME_RANGE, ContactFrequencyTimeRange.LAST_WEEK)));

        encodeDecodeFeatureSpec(new FeatureSpec<>(KnownFeature.SENT_FREQUENCY)
                .addParam(new FeatureParam<>(ParamKey.TIME_RANGE, ContactFrequencyTimeRange.LAST_MONTH)));

        encodeDecodeFeatureSpec(new FeatureSpec<>(KnownFeature.EVENT_RATIO)
                .addParam(new FeatureParam<>(ParamKey.NUMERATOR, EventType.READ))
                .addParam(new FeatureParam<>(ParamKey.DENOMINATOR, EventType.SEEN)));

        encodeDecodeFeatureSpec(new FeatureSpec<>(KnownFeature.TIME_DELTA)
                .addParam(new FeatureParam<>(ParamKey.FROM_EVENT, EventType.READ))
                .addParam(new FeatureParam<>(ParamKey.TO_EVENT, EventType.REPLIED)));
    }

    @Test
    public void testFeatureSet() throws Exception {
        Message msg = TestUtil.addMessage(mbox, USER, contactEmail, "one two three four five", "hello darkness my old friend", System.currentTimeMillis());
        FeatureSet<Message> fs = new FeatureSet<>();

        FeatureParam<ContactFrequencyTimeRange> allTimeFreqParam = new FeatureParam<ContactFrequencyTimeRange>(ParamKey.TIME_RANGE, ContactFrequencyTimeRange.FOREVER);
        MetricInitializer<ValueMetric, Integer, IntIncrement> dummyValueInitializer = new DummyValueInitializer();
        MetricInitializer<RatioMetric, Double, RatioIncrement> dummyRatioInitializer = new DummyRatioInitializer();

        //set dummy metric initializers on the MetricFeature factories so we avoid going to the EventStore for data
        FeatureParam<MetricInitializer<ValueMetric, Integer, IntIncrement>> valueInitParam = new FeatureParam<>(ParamKey.METRIC_INITIALIZER, dummyValueInitializer);
        FeatureParam<MetricInitializer<RatioMetric, Double, RatioIncrement>> ratioInitParam = new FeatureParam<>(ParamKey.METRIC_INITIALIZER, dummyRatioInitializer);

        //message is part of conversation
        fs.addFeatureSpec(new FeatureSpec<Message>(KnownFeature.IS_PART_OF_CONVERSATION));

        //# received messages from sender
        fs.addFeatureSpec(new FeatureSpec<Message>(KnownFeature.RECEIVED_FREQUENCY)
                .addParam(allTimeFreqParam)
                .addParam(valueInitParam));

        //# sent messages from sender
        fs.addFeatureSpec(new FeatureSpec<Message>(KnownFeature.SENT_FREQUENCY)
                .addParam(allTimeFreqParam)
                .addParam(valueInitParam));

        //read ratio
        fs.addFeatureSpec(new FeatureSpec<Message>(KnownFeature.EVENT_RATIO)
                .addParam(ratioInitParam)
                .addParam(new FeatureParam<>(ParamKey.NUMERATOR, EventType.READ))
                .addParam(new FeatureParam<>(ParamKey.DENOMINATOR, EventType.SEEN)));

        //reply rate
        fs.addFeatureSpec(new FeatureSpec<Message>(KnownFeature.EVENT_RATIO)
                .addParam(ratioInitParam)
                .addParam(new FeatureParam<>(ParamKey.NUMERATOR, EventType.REPLIED))
                .addParam(new FeatureParam<>(ParamKey.DENOMINATOR, EventType.SEEN)));

        //avg time to open
        fs.addFeatureSpec(new FeatureSpec<Message>(KnownFeature.TIME_DELTA)
                .addParam(ratioInitParam)
                .addParam(new FeatureParam<>(ParamKey.FROM_EVENT, EventType.SEEN))
                .addParam(new FeatureParam<>(ParamKey.TO_EVENT, EventType.READ)));

        assertEquals("wrong number of FeatureSpecs", 6, fs.getAllFeatureSpecs().size());
        assertEquals("wrong number of FeatureFactories generated by FeatureSet", 6, fs.buildFactories().size());

        List<Feature<?>> features = fs.getFeatures(msg).getFeatures();

        assertFalse("wrong conv feature", (Boolean) features.get(0).getFeatureValue());
        assertEquals("wrong recipient frequency value", 0, features.get(1).getFeatureValue());
        assertEquals("wrong sent frequency value", 0, features.get(2).getFeatureValue());
        assertEquals("wrong read ratio value", new Double(0), features.get(3).getFeatureValue());
        assertEquals("wrong reply rate value", new Double(0), features.get(4).getFeatureValue());
        assertEquals("wrong time to open value", new Double(0), features.get(5).getFeatureValue());

        //update metric features
        logMsgEvents(msg.getAccountId());

        features = fs.getFeatures(msg).getFeatures();
        //sanity check on values that shouldn't have changed
        assertFalse("wrong conv feature", (Boolean) features.get(0).getFeatureValue());
        //updated event metrics!
        assertEquals("wrong updated recipient frequency value", 10, features.get(1).getFeatureValue());
        assertEquals("wrong updated sent frequency value", 5, features.get(2).getFeatureValue());
        assertEquals("wrong updated read ratio value", new Double(0.5), features.get(3).getFeatureValue());
        assertEquals("wrong updated reply rate value", new Double(0.25), features.get(4).getFeatureValue());
        assertEquals("wrong updated time to open value", new Double(1.5), features.get(5).getFeatureValue());
    }

    private static class DummyValueInitializer extends MetricInitializer<ValueMetric, Integer, IntIncrement> {
        @Override
        public ValueMetric getInitialData() {
            return new ValueMetric(0);
        }

        @Override
        public long getMetricLifetime() {
            return 0;
        }
    };

    private static class DummyRatioInitializer extends MetricInitializer<RatioMetric, Double, RatioIncrement> {

        @Override
        public RatioMetric getInitialData() {
            return new RatioMetric(0d, 0);
        }

        @Override
        public long getMetricLifetime() {
            return 0;
        }
    };
}
