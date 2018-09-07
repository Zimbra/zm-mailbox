package com.zimbra.cs.event.analytics;

import com.google.common.base.Objects;
import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.analytics.RatioMetric.RatioIncrement;
import com.zimbra.cs.mime.ParsedAddress;

/**
 * Abstract EventMetric class used for metrics that are derived from two events
 */
public abstract class EventDifferenceMetric extends EventMetric<RatioMetric, Double, RatioIncrement> {

    protected String contactEmail;
    protected EventType firstEventType;
    protected EventType secondEventType;

    public EventDifferenceMetric(String accountId, MetricType metricType, EventDifferenceParams params) throws ServiceException {
        super(accountId, metricType, params.getInitializer());
        this.contactEmail = params.getContactEmail();
        this.firstEventType = params.getFirstEventType();
        this.secondEventType = params.getSecondEventType();
    }

    protected boolean eventMatchesContactAndType(Event event, EventType type) {
        if (!event.getAccountId().equals(accountId) || event.getEventType() != type) {
            return false;
        }
        if (contactEmail != null) {
            String eventContact = (String) event.getContextField(EventContextField.SENDER);
            return contactEmail.equalsIgnoreCase(new ParsedAddress(eventContact).emailPart);
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("acctId", accountId)
                .add("metricType", type)
                .add("firstEvent", firstEventType)
                .add("secondEvent", secondEventType)
                .add("contact", contactEmail)
                .add("value", metricData.getValue()).toString();
    }

    public static class EventDifferenceParams extends EventMetric.MetricParams<RatioMetric, Double, RatioIncrement> {

        private String contactEmail = null;
        private EventType firstEventType;
        private EventType secondEventType;

        public EventDifferenceParams(EventType first, EventType second) {
            this(first, second, null);
        }

        public EventDifferenceParams(EventType first, EventType second, String contactEmail) {
            this.firstEventType = first;
            this.secondEventType = second;
            this.contactEmail = contactEmail;
        }

        public String getContactEmail() {
            return contactEmail;
        }

        public EventType getFirstEventType() {
            return firstEventType;
        }

        public EventType getSecondEventType() {
            return secondEventType;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(contactEmail);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof EventDifferenceParams) {
                EventDifferenceParams o = (EventDifferenceParams) other;
                if (firstEventType != o.firstEventType || secondEventType != o.secondEventType) {
                    return false;
                }
                return contactEmail == null ? o.contactEmail == null : contactEmail.equals(o.getContactEmail());
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("contact", contactEmail)
                    .add("eventType1", firstEventType)
                    .add("eventType2", secondEventType).toString();
        }
    }
}
