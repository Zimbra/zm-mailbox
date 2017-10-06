package com.zimbra.cs.event.logger;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class EventLogger {
    private static final EventLogger eventLogger = new EventLogger();
    private static final CopyOnWriteArrayList<EventLogHandler> eventLogHandlers = new CopyOnWriteArrayList<>();
    private static final LinkedBlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    private EventLogger() {
        executorService.submit(new EventNotifier());
    }

    public static EventLogger getEventLogger() {
        return eventLogger;
    }

    public void registerEventLogHandler(EventLogHandler logHandler) {
        boolean present = eventLogHandlers.addIfAbsent(logHandler);
        if(!present) {
            ZimbraLog.event.warn("Event Log Handler already registered %s", logHandler);
        }
    }

    public boolean unregisterEventLogHandler(EventLogHandler logHandler) {
        boolean removed = eventLogHandlers.remove(logHandler);
        if(!removed) {
            ZimbraLog.event.warn("Event Log Handler is not registered %s", logHandler);
        }
        return removed;
    }

    @VisibleForTesting
    public void unregisterAllEventLogHandlers() {
        eventLogHandlers.clear();
    }

    public boolean log(Event event) {
        try {
            return eventQueue.add(event);
        } catch (IllegalStateException e) {
            ZimbraLog.event.debug("unable to add item for account %s to indexing queue", event.toString());
            return false;
        }
    }

    private static class EventNotifier implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    Event event = eventQueue.take();
                    Iterator<EventLogHandler> it = eventLogHandlers.iterator();
                    while (it.hasNext()) {
                        EventLogHandler logHandler = it.next();
                        logHandler.log(event);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
