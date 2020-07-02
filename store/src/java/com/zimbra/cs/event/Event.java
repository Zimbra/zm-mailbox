package com.zimbra.cs.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;

public class Event {
    public static final String MULTI_VALUE_SEPARATOR = ",";

    private String accountId;
    private EventType eventType;
    private long timestamp;
    private String dsId;
    private Map<EventContextField, Object> context = new HashMap<>();

    public static enum UniqueOn {
        NONE, MESSAGE, MSG_AND_SENDER, MSG_AND_RECIPIENT, ACCOUNT, DATASOURCE
    }

    public static enum EventType {
        SENT(UniqueOn.MSG_AND_RECIPIENT),
        RECEIVED(UniqueOn.MSG_AND_SENDER),
        READ(UniqueOn.MESSAGE),
        SEEN(UniqueOn.MESSAGE),
        REPLIED(UniqueOn.MESSAGE),
        DELETE_DATASOURCE(UniqueOn.DATASOURCE, true),
        DELETE_ACCOUNT(UniqueOn.ACCOUNT, true),
        //auxiliary event used to allow contact affinity to be calculated from
        //incoming messages
        AFFINITY(UniqueOn.MSG_AND_RECIPIENT);

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

        public static EventType of(String str) throws ServiceException {
            for (EventType type: EventType.values()) {
                if (type.name().equalsIgnoreCase(str)) {
                    return type;
                }
            }
            throw ServiceException.INVALID_REQUEST("invalid event type: " + str, null);
        }
    }

    public enum EventContextField {
        SENDER, RECEIVER, MSG_ID, RECEIVER_TYPE
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
    public static Event generateEvent(String accountId, int messageId, String sender, String recipient, EventType eventType, String dsId, String recipType, Long timestamp) {
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
        Event event = new Event(accountId, eventType, timestamp);
        event.setContextField(EventContextField.MSG_ID, messageId);
        event.setContextField(EventContextField.SENDER, sender);
        event.setContextField(EventContextField.RECEIVER, recipient);
        if (dsId != null) {
            event.setDataSourceId(dsId);
        }
        if (recipType != null) {
            event.setContextField(EventContextField.RECEIVER_TYPE, recipType);
        }
        return event;
    }

    /**
     * Convenience method to generate a single SENT event
     */
    public static Event generateSentEvent(String accountId, int messageId, String sender, String recipient, String dsId, String recipType, Long timestamp) {
        return generateEvent(accountId, messageId, sender, recipient, EventType.SENT, dsId, recipType, timestamp);
    }

    private static boolean ignoreContact(String contact, String recipientToIgnore) {
        return contact.equalsIgnoreCase(recipientToIgnore);
    }

    private static List<String> getContactsByType(MimeMessage mm, RecipientType type, String recipientToIgnore) {
        try {
            Address[] recipients = mm.getRecipients(type);
            if (recipients == null) {
                return Collections.emptyList();
            }
            List<Address> addrs = Arrays.asList(recipients);
            Stream<String> contacts = addrs.stream().map(address->address.toString());
            if (recipientToIgnore != null) {
                contacts = contacts.filter(contact -> !ignoreContact(contact, recipientToIgnore));
            }
            return contacts.collect(Collectors.toList());
        } catch (MessagingException e) {
            ZimbraLog.contact.error("unable to get contacts of type %s for contact affinity", type.toString(), e);
            return Collections.emptyList();
        }
    }

    private static void addToSentEvents(List<Event> eventList, List<String> recipients, String acctId, int msgId, String sender,
            String dsId, String recipType, long timestamp) {
        if (recipients != null && !recipients.isEmpty()) {
            for (String recip: recipients) {
                eventList.add(generateSentEvent(acctId, msgId, sender, recip, dsId, recipType, timestamp));
            }
        }
    }

    private static void addToAffinityEvents(List<Event> eventList, List<String> contacts, String acctId, int msgId, String sender,
            String dsId, String recipType, long timestamp) {
        if (contacts != null && !contacts.isEmpty()) {
            for (String contact: contacts) {
                eventList.add(generateEvent(acctId, msgId, sender, contact, EventType.AFFINITY, dsId, recipType, timestamp));
            }
        }
    }

    /**
     * Convenience method to generate a SENT event for every recipient of the email
     */
    public static List<Event> generateSentEvents(Message msg, Long timestamp) {
        try {
            String acctId = msg.getAccountId();
            int msgId = msg.getId();
            String dsId = msg.getDataSourceId();
            ParsedMessage pm = msg.getParsedMessage();
            MimeMessage mm = pm.getMimeMessage();
            String sender = pm.getSender();
            if (sender == null) {
                return Collections.emptyList();
            }
            List<String> to = getContactsByType(mm, RecipientType.TO, null);
            List<String> cc = getContactsByType(mm, RecipientType.CC, null);
            List<String> bcc = getContactsByType(mm, RecipientType.BCC, null);
            List<Event> sentEvents = new ArrayList<>(to.size() + cc.size() + bcc.size());
            addToSentEvents(sentEvents, to, acctId, msgId, sender, dsId, "to", timestamp);
            addToSentEvents(sentEvents, cc, acctId, msgId, sender, dsId, "cc", timestamp);
            addToSentEvents(sentEvents, bcc, acctId, msgId, sender, dsId, "bcc", timestamp);
            return sentEvents;
        } catch (ServiceException e) {
            ZimbraLog.event.warn("unable to generate SENT events for message %d", msg.getId());
            return Collections.emptyList();
        }
    }

    /**
     * Convenience method to generate a single RECEIVED event
     */
    public static Event generateReceivedEvent(String accountId, int messageId, String sender, String recipient, String dsId, Long timestamp) {
        return generateEvent(accountId, messageId, sender, recipient, EventType.RECEIVED, dsId, null, timestamp);
    }

    /**
     * Convenience method to generate a single RECEIVED event
     */
    public static Event generateReceivedEvent(Message msg, String recipient, Long timestamp) {
        ParsedMessage pm;
        try {
            pm = msg.getParsedMessage();
            String sender = pm.getSender();
            return generateEvent(msg.getAccountId(), msg.getId(), sender, recipient, EventType.RECEIVED, msg.getDataSourceId(), null, timestamp);
        } catch (ServiceException e) {
            ZimbraLog.event.warn("unable to generate RECEIVED events for message %d", msg.getId());
            return null;
        }
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

    public static Event generateMsgEvent(Message msg, EventType eventType) {
        Event event = new Event(msg.getAccountId(), eventType, System.currentTimeMillis());
        event.setContextField(EventContextField.MSG_ID, msg.getId());
        String sender = msg.getSender();
        if (sender != null) {
            event.setContextField(EventContextField.SENDER, sender);
        }
        String dsId = msg.getDataSourceId();
        if (dsId != null) {
            event.setDataSourceId(dsId);
        }
        return event;
    }

    /**
     * Generate AFFINITY events for an incoming message
     */
    public static List<Event> generateAffinityEvents(Message msg, String recipientToIgnore, Long timestamp) {
        try {
            ParsedMessage pm = msg.getParsedMessage();
            MimeMessage mm = pm.getMimeMessage();
            String acctId = msg.getAccountId();
            int msgId = msg.getId();
            String dsId = msg.getDataSourceId();
            String sender = pm.getSender();
            if (sender == null) {
                return Collections.emptyList();
            }
            List<String> to = getContactsByType(mm, RecipientType.TO, recipientToIgnore);
            List<String> cc = getContactsByType(mm, RecipientType.CC, recipientToIgnore);
            List<Event> cooccurrEvents = new ArrayList<>(to.size() + cc.size());
            addToAffinityEvents(cooccurrEvents, to, acctId, msgId, sender, dsId, "to", timestamp);
            addToAffinityEvents(cooccurrEvents, cc, acctId, msgId, sender, dsId, "cc", timestamp);
            return cooccurrEvents;
        } catch (ServiceException e) {
            ZimbraLog.event.warn("unable to generate AFFINITY events for message %d", msg.getId());
            return Collections.emptyList();
        }
    }

    /**
     * Convenience method to generate a single READ event
     */
    public static Event generateReadEvent(String accountId, int messageId, String sender, String dsId, Long timestamp) {
        return generateEvent(accountId, messageId, sender, null, EventType.READ, dsId, null, timestamp);
    }

    /**
     * Convenience method to generate a single SEEN event
     */
    public static Event generateSeenEvent(String accountId, int messageId, String sender, String dsId, Long timestamp) {
        return generateEvent(accountId, messageId, sender, null, EventType.SEEN, dsId, null, timestamp);
    }

    /**
     * Convenience method to generate a single REPLIED event
     */
    public static Event generateRepliedEvent(String accountId, int messageId, String sender, String dsId, Long timestamp) {
        return generateEvent(accountId, messageId, sender, null, EventType.REPLIED, dsId, null, timestamp);
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
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this)
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
