package com.zimbra.cs.event;

import javax.mail.Address;

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
    private Map<EventContextField, Object> context = new HashMap<>();

    public enum EventType {
        SENT, RECEIVED, READ, SEEN
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
        if(context.containsKey(field)) {
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
     * Convenience method to generate a single SENT event
     */
    public static Event generateSentEvent(String accountId, int messageId, String sender, String recipient) {
        Event event = new Event(accountId, EventType.SENT, System.currentTimeMillis());
        event.setContextField(EventContextField.MSG_ID, messageId);
        event.setContextField(EventContextField.SENDER, new ParsedAddress(sender.toString()).emailPart);
        event.setContextField(EventContextField.RECEIVER, new ParsedAddress(recipient.toString()).emailPart);
        return event;
    }

    /**
     * Convenience method to generate a SENT event for every recipient of the email
     */
    public static List<Event> generateSentEvents(String accountId, int messageId, Address sender, Address[] allRecipients) {
        List<Event> sentEvents = new ArrayList<>(allRecipients.length);
        for (Address address : allRecipients) {
            sentEvents.add(generateSentEvent(accountId, messageId, sender.toString(), address.toString()));
        }
        return sentEvents;
    }
}
