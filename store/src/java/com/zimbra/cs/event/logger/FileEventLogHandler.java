package com.zimbra.cs.event.logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.Event.EventType;

/**
 * Event log handler that logs events to a log file in a structured manner.
 * This lets us re-index events or migrate them to a different backend.
 */
public class FileEventLogHandler implements EventLogHandler {

    private EventFormatter formatter;
    private static final Log log = ZimbraLog.eventlog;

    public FileEventLogHandler(EventFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void log(Event event) {
        String logString = formatter.toLogString(event);
        if (logString != null) {
            log.info(logString);
        }
    }

    @Override
    public void shutdown() {
        //nothing to do here
    }

    static interface EventFormatter {
        String toLogString(Event event);
        Event fromLogString(String string);
    }

    static class ZimbraEventFormatter implements EventFormatter {

        private static final String KEY_TYPE = "type";
        private static final String KEY_TIMESTAMP = "timestamp";
        private static final String KEY_ACCT_ID = "acct_id";
        private static final String KEY_DS_ID = "ds_id";


        @Override
        public String toLogString(Event event) {
            String accountId = event.getAccountId();
            String type = event.getEventType().toString();
            String timestamp = String.valueOf(event.getTimestamp());
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> eventMap = new HashMap<>();

            eventMap.put(KEY_ACCT_ID, accountId);
            eventMap.put(KEY_TIMESTAMP, timestamp);
            eventMap.put(KEY_TYPE, type);
            if (event.hasDataSourceId()) {
                eventMap.put(KEY_DS_ID, event.getDataSourceId());
            }
            for (Map.Entry<EventContextField, Object> entry: event.getContext().entrySet()) {
                eventMap.put(entry.getKey().toString(), entry.getValue());
            }
            try {
                return mapper.writeValueAsString(eventMap);
            } catch (IOException e) {
                ZimbraLog.event.error("unable to serialize %s event for account %s", type, accountId, e);
                return null;
            }
        }

        @Override
        public Event fromLogString(String string) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String acctId = null;
                EventType type = null;
                Long timestamp = null;
                String dsId = null;
                Map<EventContextField, Object> contextMap = new HashMap<>();
                Map<String, String> eventDataMap = mapper.readValue(string, new TypeReference<Map<String, String>>() {});
                for(Map.Entry<String, String> entry: eventDataMap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    switch(key) {
                    case KEY_TYPE:
                        type = EventType.valueOf(value);
                        break;
                    case KEY_ACCT_ID:
                        acctId = value;
                        break;
                    case KEY_TIMESTAMP:
                        timestamp = Long.valueOf(value);
                        break;
                    case KEY_DS_ID:
                        dsId = value;
                    default:
                        try {
                            EventContextField ctxtField = EventContextField.valueOf(key);
                            contextMap.put(ctxtField, value);
                        } catch (IllegalArgumentException e) {
                            ZimbraLog.event.warn("unknown event field: %s", key);
                        }
                    }
                }
                if (acctId == null) {
                    ZimbraLog.event.error("unable to deserialize event [%s], no account ID found", string);
                    return null;
                } else if (type == null) {
                    ZimbraLog.event.error("unable to deserialize event [%s], no  event type found", string);
                    return null;
                } else if (timestamp == null) {
                    ZimbraLog.event.error("unable to deserialize event [%s], no  event type found", string);
                    return null;
                }
                Event event = new Event(acctId, type, timestamp);
                event.setContext(contextMap);
                if (dsId != null) {
                    event.setDataSourceId(dsId);
                }
                return event;
            } catch (IOException e) {
                ZimbraLog.event.error("unable to deserialize event [%s]", string, e);
                return null;
            }
        }
    }

    public static class Factory implements EventLogHandler.Factory {

        private FileEventLogHandler instance;

        @Override
        public EventLogHandler createHandler(String config) {
            synchronized (Factory.class) {
                if (instance == null) {
                    instance = new FileEventLogHandler(new ZimbraEventFormatter());
                }
                return instance;
            }
        }
    }
}
