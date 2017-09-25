package com.zimbra.cs.event.logger;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.cs.event.Event;

import java.util.ArrayList;
import java.util.List;

public class InMemoryEventLogger extends EventLogger {
    private List<Event> logs = new ArrayList<>();

    @Override
    public void log(Event event) {
        logs.add(event);
    }

    @VisibleForTesting
    protected List<Event> getLogs() {
        return logs;
    }
}