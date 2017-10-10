package com.zimbra.cs.event.logger;

import com.zimbra.cs.event.Event;

import java.util.ArrayList;
import java.util.List;

public class InMemoryEventLogHandler implements EventLogHandler {
    private List<Event> logs = new ArrayList<>();

    @Override
    public synchronized void log(Event event) {
        logs.add(event);
    }

    @Override
    public void shutdown() {

    }

    protected List<Event> getLogs() {
        return logs;
    }
}
