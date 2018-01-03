package com.zimbra.cs.event.analytics;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.EventStore;
import com.zimbra.cs.event.analytics.ValueMetric.IntIncrement;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyEventType;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyTimeRange;
import com.zimbra.cs.mime.ParsedAddress;

/**
 * EventMetric that tracks contact frequency for the given contact.
 * The params specify whether to track incoming/outgoing/both messages, and
 * the time horizon.
 */
public class ContactFrequencyMetric extends EventMetric<ValueMetric, Integer, IntIncrement> {

    private String contactEmail;
    private ContactFrequencyTimeRange freqTimeRange;
    private ContactFrequencyEventType freqEventType;

    private static final long MILLIS_PER_DAY = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
    private static final long MILLIS_PER_WEEK = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS);
    private static final long MILLIS_PER_MONTH = TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS);

    public ContactFrequencyMetric(String accountId, ContactFrequencyParams params) throws ServiceException {
        super(accountId, MetricType.CONTACT_FREQUENCY, params.getInitializer());
        this.contactEmail = params.getContactEmail();
        this.freqTimeRange = params.getFreqTimeRange();
        this.freqEventType = params.getFreqEventType();
    }

    @Override
    protected IntIncrement getIncrement(List<Event> events) throws ServiceException {
        int inc = (int) events.stream().filter(event -> eventAffectsContactFrequency(event)).count();
        return new IntIncrement(inc);
    }

    private boolean eventAffectsContactFrequency(Event event) {
        if (!event.getAccountId().equals(accountId)) {
            return false; //wrong account
        }
        EventType type = event.getEventType();
        if (type != EventType.SENT && type != EventType.RECEIVED) {
            return false;
        }
        if (type == EventType.SENT && freqEventType == ContactFrequencyEventType.RECEIVED) {
            return false; //don't care about SENT events if we're only calculating recieved frequency
        }
        if (type == EventType.RECEIVED && freqEventType == ContactFrequencyEventType.SENT) {
            return false; //don't care about RECEIVED events if we're only calculating sent frequency
        }
        String eventContact = type == EventType.SENT ? (String) event.getContextField(EventContextField.RECEIVER)
                : (String) event.getContextField(EventContextField.SENDER);
        return contactEmail.equalsIgnoreCase(new ParsedAddress(eventContact).emailPart);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("acctId", accountId)
                .add("metricType", type)
                .add("contact", contactEmail)
                .add("eventType", freqEventType)
                .add("timeRange", freqTimeRange)
                .add("value", metricData.getValue()).toString();
    }

    private static class Initializer extends EventStoreInitializer<ValueMetric, Integer, IntIncrement> {

        private ContactFrequencyParams params;

        public Initializer(EventStore eventStore, ContactFrequencyParams params) {
            super(eventStore);
            this.params = params;
        }

        @Override
        public ValueMetric getInitialData() throws ServiceException {
            String contactEmail = params.getContactEmail();
            Long freq = getEventStore().getContactFrequencyCount(contactEmail, params.getFreqEventType(), params.getFreqTimeRange());
            return new ValueMetric(freq.intValue());
        }

        @Override
        public long getMetricLifetime() {
            switch (params.freqTimeRange) {
            case LAST_DAY:
                return MILLIS_PER_DAY;
            case LAST_WEEK:
                return MILLIS_PER_WEEK;
            case LAST_MONTH:
                return MILLIS_PER_MONTH;
            case FOREVER:
            default:
                return 0;
            }
        }
    }

    public static class ContactFrequencyParams extends EventMetric.MetricParams<ValueMetric, Integer, IntIncrement> {

        private String contactEmail;
        private ContactFrequencyTimeRange freqTimeRange;
        private ContactFrequencyEventType freqEventType;


        public ContactFrequencyParams(String contactEmail,
                ContactFrequencyTimeRange freqTimeRange,
                ContactFrequencyEventType freqEventType) {
            this.contactEmail = contactEmail;
            this.freqTimeRange = freqTimeRange;
            this.freqEventType = freqEventType;
        }

        public String getContactEmail() {
            return contactEmail;
        }

        public ContactFrequencyTimeRange getFreqTimeRange() {
            return freqTimeRange;
        }

        public ContactFrequencyEventType getFreqEventType() {
            return freqEventType;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(contactEmail, freqTimeRange, freqEventType);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ContactFrequencyParams) {
                ContactFrequencyParams otherParams = (ContactFrequencyParams) other;
                return contactEmail.equals(otherParams.getContactEmail())
                        && freqEventType == otherParams.getFreqEventType()
                        && freqTimeRange == otherParams.getFreqTimeRange();

            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("contact", contactEmail)
                    .add("timeRange", freqTimeRange)
                    .add("eventType", freqEventType).toString();
        }
    }

    public static class Factory implements EventMetric.Factory<ValueMetric, Integer, IntIncrement> {

        public String contactEmail;

        @Override
        public EventMetric<ValueMetric, Integer, IntIncrement> buildMetric(String accountId, MetricParams<ValueMetric, Integer, IntIncrement> params) throws ServiceException {
            ContactFrequencyParams cfParams = (ContactFrequencyParams) params;
            if (cfParams.getInitializer() == null) {
                EventStore eventStore = EventStore.getFactory().getEventStore(accountId);
                cfParams.setInitializer(new Initializer(eventStore, cfParams));
            }
            return new ContactFrequencyMetric(accountId, cfParams);
        }
    }
}
