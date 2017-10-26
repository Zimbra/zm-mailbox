package com.zimbra.cs.index.event.logger;

import com.zimbra.cs.index.event.Event;

import java.util.ArrayList;
import java.util.List;

public class InMemoryEventLogger extends EventLogger {
    private List<Event> logs = new ArrayList<>();

    @Override
    public void log(Event event) {
        logs.add(event);
    }

    protected List<Event> getLogs() {
        return logs;
    }
}