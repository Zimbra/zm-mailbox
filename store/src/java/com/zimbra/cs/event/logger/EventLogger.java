package com.zimbra.cs.event.logger;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;

import java.util.ArrayList;
import java.util.List;

public class EventLogger {
    private static EventLogger eventLogger = null;
    private final List<EventLogHandler> eventLogHandlers = new ArrayList<>();

    private EventLogger() {}

    public static EventLogger getEventLogger() {
        if(eventLogger == null) {
            eventLogger = new EventLogger();
        }
        return eventLogger;
    }

    public void registerEventLogHandler(EventLogHandler logHandler) {
        if(eventLogHandlers.contains(logHandler)) {
            ZimbraLog.event.warn("Event Log Handler already registered %s", logHandler);
        }
        else {
            eventLogHandlers.add(logHandler);
        }
    }

    public boolean unregisterEventLogHandler(EventLogHandler logHandler) {
        if(eventLogHandlers.contains(logHandler)) {
            return eventLogHandlers.remove(logHandler);
        }
        else {
            ZimbraLog.event.warn("Event Log Handler is not registered %s", logHandler);
            return false;
        }
    }

    @VisibleForTesting
    public void unregisterAllEventLogHandlers() {
        eventLogHandlers.clear();
    }

    public void log(Event event) {
        for (EventLogHandler eventLogHandler : eventLogHandlers) {
            eventLogHandler.log(event);
        }
    }
}