package com.zimbra.cs.event.logger;

import com.zimbra.cs.event.Event;

public interface EventLogHandler {

    void log(Event event);

    boolean acceptsInternalEvents();

    void shutdown();

    public static interface Factory {
        public EventLogHandler createHandler(String config);
    }
}
