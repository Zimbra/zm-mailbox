package com.zimbra.cs.event.logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;

import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventLogger {
    private static final CopyOnWriteArrayList<EventLogHandler> eventLogHandlers = new CopyOnWriteArrayList<>();
    private static final LinkedBlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private static final AtomicBoolean drainQueueBeforeShutdown = new AtomicBoolean(false);
    private ExecutorService executorService;
    private int NUM_OF_WORKER_THREADS = 2;
    private static final EventLogger eventLogger = new EventLogger();

    private EventLogger() {
        startupEventNotifierExecutor();
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
            ZimbraLog.event.info("unable to add item for account %s to indexing queue", event.getAccountId());
            return false;
        }
    }

    public void startupEventNotifierExecutor() {
        ZimbraLog.event.info("Starting Event Notifier Logger! Initial event queue size " + eventQueue.size());
        drainQueueBeforeShutdown.set(false);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("EventLogger-Worker-Thread-%d").build();
        executorService = Executors.newFixedThreadPool(NUM_OF_WORKER_THREADS, namedThreadFactory);

        for (int i = 0; i < NUM_OF_WORKER_THREADS; i++) {
            executorService.execute(new EventNotifier());
        }
    }

    /**
     * This method shuts down all the event notifier threads. The event queue is left intact.
     */
    public void shutdownEventNotifierExecutor() {
        ZimbraLog.event.warn("Shutdown called for Event Notifier Executor! Initiating shutdown sequence...");
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        finally {
            String message = executorService.isTerminated() ? "Event Notifier Executor shutdown was successful!" : "Event Notifier Executor was not terminated!";
            ZimbraLog.event.info(message);
            ZimbraLog.event.info("Event Queue Size " + eventQueue.size());
        }
    }

    /**
     * This method Drains the event queue and then shuts down all the threads.
     */
    public void shutdowEventLogger() {
        drainQueueBeforeShutdown.set(true);
        shutdownEventNotifierExecutor();
    }

    private static class EventNotifier implements Runnable {

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    consume(eventQueue);
                }
            } catch (InterruptedException e) {
                ZimbraLog.event.debug(Thread.currentThread().getName() + " was interrupted! Shutting it down", e);
                Thread.currentThread().interrupt();
            } finally {
                if(drainQueueBeforeShutdown.get()) {
                    try {
                        drainQueue();
                    } catch (InterruptedException e) {
                        ZimbraLog.event.debug(Thread.currentThread().getName() + " was interrupted! Unable to drain the event queue", e);
                    }
                }
                shutdownEventLogHandlers();
            }
        }

        public void consume(BlockingQueue<Event> events) throws InterruptedException {
            Event event = events.take();
            notifyEventLogHandlers(event);
        }

        private void notifyEventLogHandlers(Event event) {
            Iterator<EventLogHandler> it = eventLogHandlers.iterator();
            while (it.hasNext()) {
                EventLogHandler logHandler = it.next();
                logHandler.log(event);
            }
        }

        private void drainQueue() throws InterruptedException {
            Event event = eventQueue.poll();
            if (event != null) {
                do {
                    notifyEventLogHandlers(event);
                    event = eventQueue.poll();
                } while (event != null);
            }
        }

        public void shutdownEventLogHandlers() {
            Iterator<EventLogHandler> it = eventLogHandlers.iterator();
            while (it.hasNext()) {
                EventLogHandler logHandler = it.next();
                logHandler.shutdown();
            }
        }
    }
}
