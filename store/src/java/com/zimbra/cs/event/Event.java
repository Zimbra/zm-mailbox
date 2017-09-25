package com.zimbra.cs.event;

import java.util.Map;

public class Event {
    private String accountId;
    private EventType eventType;
    private long timestamp;
    private Map<EventContextField, Object> context;

    public enum EventType {
        SENT, RECEIVED, READ, SEEN
    }

    public enum EventContextField {
        SENDER, RECEIVER
    }

    public Event(String accountId, EventType eventType, long timestamp, Map<EventContextField, Object> context) {
        this.accountId = accountId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.context = context;
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
        this.context = context;
    }

    public Object getContextField(EventContextField field) {
        if(context.containsKey(field)) {
            return context.get(field);
        }
        return null;
    }
}