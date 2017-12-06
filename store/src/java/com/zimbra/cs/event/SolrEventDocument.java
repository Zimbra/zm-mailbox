package com.zimbra.cs.event;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;

import com.zimbra.common.util.UUIDUtil;
import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.Event.UniqueOn;
import com.zimbra.cs.index.LuceneFields;

public class SolrEventDocument {
    // see schema.xml for static/dynamic field definitions
    private static String FIELD_EVENT_ID = LuceneFields.L_EVENT_ID;
    private static String FIELD_EVENT_TYPE= LuceneFields.L_EVENT_TYPE;
    private static String FIELD_EVENT_TIME= LuceneFields.L_EVENT_TIME;
    private static String FIELD_MSG_ID = LuceneFields.L_EVENT_MESSAGE_ID;
    private static String DYNAMIC_FIELD_FORMAT = "%s_%s";
    private static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private Event event;
    private SolrInputDocument document;

    public SolrEventDocument(Event event) {
        this.event = event;
        document = new SolrInputDocument();
        setId();
        setEventType();
        setTimestamp();
        setDataSourceId();
        setContextFields();
    }

    public SolrInputDocument getDocument() {
        return document;
    }

    private String generateId() {
        UniqueOn uniqueOn = event.getEventType().getUniqueOn();
        String eventType = event.getEventType().name();
        Integer msgId;
        String accountLevelIdentifier;
        switch (uniqueOn) {
        case ACCOUNT:
            accountLevelIdentifier = event.getEventType().name();
            break;
        case DATASOURCE:
            accountLevelIdentifier = String.format("%s:%s", eventType, event.getDataSourceId());
            break;
        case MESSAGE:
            msgId = (Integer) event.getContextField(EventContextField.MSG_ID);
            accountLevelIdentifier = String.format("%s:%d", eventType, msgId);
            break;
        case MSG_AND_SENDER:
            msgId = (Integer) event.getContextField(EventContextField.MSG_ID);
            String sender = (String) event.getContextField(EventContextField.SENDER);
            accountLevelIdentifier = String.format("%s:%d:%s", eventType, msgId, sender);
            break;
        case MSG_AND_RECIPIENT:
            msgId = (Integer) event.getContextField(EventContextField.MSG_ID);
            String recipient = (String) event.getContextField(EventContextField.RECEIVER);
            accountLevelIdentifier = String.format("%s:%d:%s", eventType, msgId, recipient);
            break;
        case NONE:
        default:
            accountLevelIdentifier = UUIDUtil.generateUUID();
        }
        return String.format("%s:%s", event.getAccountId(), accountLevelIdentifier);
    }

    private void setId() {
        document.setField(FIELD_EVENT_ID, generateId());
    }

    private void setEventType() {
        document.setField(FIELD_EVENT_TYPE, event.getEventType().toString());
    }

    private void setTimestamp() {
        String formatted = DATE_FORMAT.format(Instant.ofEpochMilli(event.getTimestamp()));
        document.setField(FIELD_EVENT_TIME, formatted);
    }

    private void setDataSourceId() {
        if (event.hasDataSourceId()) {
            document.setField(LuceneFields.L_DATASOURCE_ID, event.getDataSourceId());
        }
    }

    private void setContextFields() {
        for (Map.Entry<EventContextField, Object> entry: event.getContext().entrySet()) {
            document.setField(getSolrQueryField(entry.getKey()), entry.getValue());
        }
    }

    /**
     * Convert a known EventContextField to a dynamic Solr field. This is necessary to avoid
     * having to update the Solr schema every time a new event type is added.
     */
    public static String getSolrQueryField(EventContextField field) {
        SolrFieldType fieldType;
        switch (field) {
        case MSG_ID:
            return FIELD_MSG_ID;
        case RECEIVER:
        case SENDER:
            fieldType = SolrFieldType.ADDRESS;
            break;
        case RECEIVER_TYPE:
        default:
            fieldType = SolrFieldType.STRING;
            break;
        }
        return String.format(DYNAMIC_FIELD_FORMAT, field.toString().toLowerCase(), fieldType.suffix);
    }

    public static String getSolrStoredField(EventContextField field) {
        SolrFieldType fieldType;
        switch (field) {
        case MSG_ID:
            return FIELD_MSG_ID;
        case RECEIVER:
        case SENDER:
        case RECEIVER_TYPE:
        default:
            fieldType = SolrFieldType.STRING;
            break;
        }
        return String.format(DYNAMIC_FIELD_FORMAT, field.toString().toLowerCase(), fieldType.suffix);
    }

    private static enum SolrFieldType {

        /*
         * These suffixes specify dynamic field types defined in schema.xml for the "events" Solr config set.
         * Numeric fields are stored as point numerics for efficiency.
         */
        STRING("s"),
        STRINGS("ss"),
        INTEGER("pi"),
        INTEGERS("pis"),
        FLOAT("pf"),
        FLOATS("pfs"),
        LONG("pl"),
        LONGS("pls"),
        BOOLEAN("b"),
        BOOLEANS("bs"),
        DATE("pdt"),
        DATES("pdts"),
        ADDRESS("zaddr");

        private String suffix;

        private SolrFieldType(String suffix) {
            this.suffix = suffix;
        }
    }
}
