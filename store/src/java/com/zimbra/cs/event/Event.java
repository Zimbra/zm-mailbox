package com.zimbra.cs.event;

import javax.mail.Address;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Strings;
import com.zimbra.cs.mime.ParsedAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Event {
    public static final String MULTI_VALUE_SEPARATOR = ",";

    private String accountId;
    private EventType eventType;
    private long timestamp;
    private String dsId;
    private Map<EventContextField, Object> context = new HashMap<>();

    public static enum UniqueOn {
        NONE, MESSAGE, ACCOUNT, DATASOURCE
    }

    public static enum EventType {
        SENT(UniqueOn.MESSAGE),
        RECEIVED(UniqueOn.MESSAGE),
        READ(UniqueOn.MESSAGE),
        SEEN(UniqueOn.MESSAGE),
        DELETE_DATASOURCE(UniqueOn.ACCOUNT, true),
        DELETE_ACCOUNT(UniqueOn.DATASOURCE, true);

        private boolean internal;
        private UniqueOn uniqueOn;

        private EventType(UniqueOn uniqueness) {
            this(uniqueness, false);
        }
        private EventType(UniqueOn uniqueOn, boolean isInternal) {
            this.uniqueOn = uniqueOn;
            this.internal = isInternal;
        }

        public boolean isInternal() {
            return internal;
        }

        public UniqueOn getUniqueOn() {
            return uniqueOn;
        }
    }

    public enum EventContextField {
        SENDER, RECEIVER, MSG_ID
    }

    public Event(String accountId, EventType eventType, long timestamp) {
        this.accountId = accountId;
        this.eventType = eventType;
        this.timestamp = timestamp;
    }

    public Event(String accountId, EventType eventType, long timestamp, Map<EventContextField, Object> context) {
        this.accountId = accountId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.context.putAll(context);
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public boolean hasDataSourceId() {
        return !Strings.isNullOrEmpty(dsId);
    }

    public String getDataSourceId() {
        return dsId;
    }

    public void setDataSourceId(String dsId) {
        this.dsId = dsId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<EventContextField, Object> getContext() {
        return context;
    }

    public void setContext(Map<EventContextField, Object> context) {
        this.context = new HashMap<>(context.size());
        this.context.putAll(context);
    }

    public Object getContextField(EventContextField field) {
        if (context.containsKey(field)) {
            return context.get(field);
        }
        return null;
    }

    public void setContextField(EventContextField field, Object value) {
        context.put(field, value);
    }

    public Event copy() {
        return new Event(this.accountId, this.eventType, this.timestamp, this.context);
    }

    /**
     * Base method that encapsulated the logic to create an event with commonly used fields
     */
    private static Event generateEvent(String accountId, int messageId, String sender, String recipient, EventType eventType, String dsId) {
        Event event = new Event(accountId, eventType, System.currentTimeMillis());
        event.setContextField(EventContextField.MSG_ID, messageId);
        event.setContextField(EventContextField.SENDER, new ParsedAddress(sender.toString()).emailPart);
        event.setContextField(EventContextField.RECEIVER, new ParsedAddress(recipient.toString()).emailPart);
        if (dsId != null) {
            event.setDataSourceId(dsId);
        }
        return event;
    }

    /**
     * Convenience method to generate a single SENT event
     */
    public static Event generateSentEvent(String accountId, int messageId, String sender, String recipient, String dsId) {
        return generateEvent(accountId, messageId, sender, recipient, EventType.SENT, dsId);
    }

    /**
     * Convenience method to generate a SENT event for every recipient of the email
     */
    public static List<Event> generateSentEvents(String accountId, int messageId, Address sender, Address[] allRecipients, String dsId) {
        List<Event> sentEvents = new ArrayList<>(allRecipients.length);
        for (Address address : allRecipients) {
            sentEvents.add(generateSentEvent(accountId, messageId, sender.toString(), address.toString(), dsId));
        }
        return sentEvents;
    }

    /**
     * Convenience method to generate a single RECEIVED event
     */
    public static Event generateReceivedEvent(String accountId, int messageId, String sender, String recipient, String dsId) {
        return generateEvent(accountId, messageId, sender, recipient, EventType.RECEIVED, dsId);
    }

    /**
     * Generate an internal event representing the deletion of a datasource
     */
    public static Event generateDeleteDataSourceEvent(String accountId, String dsId) {
        Event event = new Event(accountId, EventType.DELETE_DATASOURCE, System.currentTimeMillis());
        event.setDataSourceId(dsId);
        return event;
    }

    /**
     * Generate an internal event representing the deletion of an account
     */
    public static Event generateDeleteAccountEvent(String accountId) {
        return new Event(accountId, EventType.DELETE_ACCOUNT, System.currentTimeMillis());
    }


    @Override
    public boolean equals(Object other) {
        if (other instanceof Event) {
            Event otherEvent = (Event) other;
            return accountId.equals(otherEvent.accountId) &&
                    eventType == otherEvent.eventType &&
                    timestamp == otherEvent.timestamp &&
                    dsId == otherEvent.dsId &&
                    context.equals(otherEvent.context);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        ToStringHelper helper = Objects.toStringHelper(this)
                .add("acctId", accountId)
                .add("type", eventType)
                .add("timestamp", timestamp);
        for (Map.Entry<EventContextField, Object> entry: context.entrySet()) {
            helper.add(entry.getKey().toString(), entry.getValue().toString());
        }
        return helper.toString();
    }

    public boolean isInternal() {
        return eventType.isInternal();
    }
}
