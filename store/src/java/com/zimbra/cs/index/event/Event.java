package com.zimbra.cs.index.event;

import java.util.Map;

public class Event {
    private String userIdentifier;
    private EventType eventType;
    private long timestamp;
    private String sender;
    private String recipient;
    private Map<String, Object> context;

    public enum EventType {
        SENT, RECEIVED, READ, SEEN
    }

    public Event(String userIdentifier, EventType eventType, long timestamp, String sender, String recipient, Map<String, Object> context) {
        this.userIdentifier = userIdentifier;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.sender = sender;
        this.recipient = recipient;
        this.context = context;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
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

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
