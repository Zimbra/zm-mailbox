package com.zimbra.cs.event.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.event.Event;

public class EventLogger {
    private static final Map<String, EventLogHandler.Factory> factoryMap = new HashMap<>();
    private static final LinkedBlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private static final AtomicBoolean executorServiceRunning = new AtomicBoolean(false);
    private static final AtomicBoolean drainQueueBeforeShutdown = new AtomicBoolean(false);
    private ExecutorService executorService;
    private ConfigProvider config;
    private boolean enabled;
    private static EventLogger instance;

    private EventLogger(ConfigProvider config) {
        this.config = config;
    }

    /**
     * Return the EventLogger singleton backed by LDAP config
     */
    public static EventLogger getEventLogger() {
        synchronized (EventLogger.class) {
            if (instance == null) {
                instance = new EventLogger(new LdapConfigProvider());
            }
        }
        return instance;
    }

    @VisibleForTesting
    /**
     * Return an EventLogger singleton, overriding the previous ConfigProvider
     */
    public static EventLogger getEventLogger(ConfigProvider config) {
        synchronized (EventLogger.class) {
            if (instance == null) {
                instance = new EventLogger(config);
            } else {
                instance.setConfigProvider(config);
            }
        }
        return instance;
    }

    private void setConfigProvider(ConfigProvider config) {
        this.config = config;
        restartEventNotifierExecutor();
    }

    public static void registerHandlerFactory(String factoryName, EventLogHandler.Factory factory) {
        if (factoryMap.containsKey(factoryName)) {
            ZimbraLog.event.warn("EventLogHandler Factory %s already registered", factoryName);
        } else {
            factoryMap.put(factoryName, factory);
        }
    }

    public static boolean unregisterHandlerFactory(String factoryName) {
        if (!factoryMap.containsKey(factoryName)) {
            ZimbraLog.event.warn("EventLogHandler Factory %s is not registered", factoryName);
            return false;
        } else {
            factoryMap.remove(factoryName);
            return true;
        }
    }

    @VisibleForTesting
    public static void unregisterAllHandlerFactories() {
        factoryMap.clear();
    }

    public static boolean isFactoryRegistered(String factoryName) {
        return factoryMap.containsKey(factoryName);
    }

    /**
     * Restart the executor service, picking up new configuration data
     */
    public void restartEventNotifierExecutor() {
        shutdownEventNotifierExecutor();
        startupEventNotifierExecutor();
    }

    public void log(List<Event> events) {
        for (Event event : events) {
            log(event);
        }
    }

    public boolean log(Event event) {
        if (!enabled) {
            return false;
        }
        try {
            return eventQueue.add(event);
        } catch (IllegalStateException e) {
            ZimbraLog.event.info("unable to add item for account %s to indexing queue", event.getAccountId());
            return false;
        }
    }

    private Map<String, Collection<String>> getConfigMap() {
        Map<String, Collection<String>> configMap = config.getHandlerConfig();
        configMap.keySet().retainAll(factoryMap.keySet());
        return configMap;
    }

    public void startupEventNotifierExecutor() {
        if (executorServiceRunning.get()) {
            ZimbraLog.event.info("Event logger executor service already running...");
            return;
        }
        int numThreads = config.getNumThreads();
        ZimbraLog.event.info("Starting Event Notifier Logger with %s threads; initial event queue size is %s", numThreads, eventQueue.size());
        drainQueueBeforeShutdown.set(false);

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("EventLogger-Worker-Thread-%d").build();
        executorService = Executors.newFixedThreadPool(numThreads, namedThreadFactory);

        Map<String, Collection<String>> configMap = getConfigMap();
        for (int i = 0; i < numThreads; i++) {
            executorService.execute(new EventNotifier(factoryMap, configMap));
        }
        executorServiceRunning.set(true);
    }

    /**
     * This method shuts down all the event notifier threads. The event queue is left intact.
     */
    public void shutdownEventNotifierExecutor() {
        if (!executorServiceRunning.get()) {
            ZimbraLog.event.info("Event logger executor service is not running...");
            return;
        }

        ZimbraLog.event.warn("Shutdown called for Event Notifier Executor; initiating shutdown sequence...");
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            String message = executorService.isTerminated() ? "Event Notifier Executor shutdown was successful" : "Event Notifier Executor was not terminated";
            ZimbraLog.event.info(message);
            ZimbraLog.event.info("Event Queue Size " + eventQueue.size());
            executorServiceRunning.set(false);
        }
    }

    /**
     * This method Drains the event queue and then shuts down all the threads.
     */
    public void shutdownEventLogger() {
        drainQueueBeforeShutdown.set(true);
        shutdownEventNotifierExecutor();
    }

    private static class EventNotifier implements Runnable {

        private List<EventLogHandler> handlers;

        private EventNotifier(Map<String, EventLogHandler.Factory> knownFactories, Map<String, Collection<String>> handlerConfigs) {
            handlers = new ArrayList<>(handlerConfigs.size());
            for (Map.Entry<String, Collection<String>> entry: handlerConfigs.entrySet()) {
                String factoryName = entry.getKey();
                EventLogHandler.Factory factory = knownFactories.get(factoryName);
                if (factory != null) {
                    for (String config: entry.getValue()) {
                        //create an instance of the handler for each config string
                        handlers.add(factory.createHandler(config));
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    consume(eventQueue);
                }
            } catch (InterruptedException e) {
                ZimbraLog.event.debug("%s was interrupted, Shutting it down", Thread.currentThread().getName(), e);
                Thread.currentThread().interrupt();
            } finally {
                if (drainQueueBeforeShutdown.get()) {
                    try {
                        drainQueue();
                    } catch (InterruptedException e) {
                        ZimbraLog.event.debug("%s was interrupted; unable to drain the event queue", Thread.currentThread().getName(), e);
                    }
                }
                shutdownEventLogHandlers();
            }
        }

        private void consume(BlockingQueue<Event> events) throws InterruptedException {
            Event event = events.take();
            notifyEventLogHandlers(event);
        }

        private void notifyEventLogHandlers(Event event) {
            for (EventLogHandler logHandler: handlers) {
                if (!event.isInternal() || logHandler.acceptsInternalEvents()) {
                    logHandler.log(event);
                }
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

        private void shutdownEventLogHandlers() {
            for (EventLogHandler logHandler: handlers) {
                logHandler.shutdown();
            }
        }
    }

    public static interface ConfigProvider {
        int getNumThreads();
        Map<String, Collection<String>> getHandlerConfig();
    }

    static class LdapConfigProvider implements ConfigProvider {

        private static final int DEFAULT_NUM_THREADS = 10;
        private static String[] DEFAULT_HANDLERS = new String[] {"file://default"};

        private Server getServer() throws ServiceException {
            try {
                return Provisioning.getInstance().getLocalServer();
            } catch (ServiceException e) {
                ZimbraLog.event.error("unable to instantiate EventLogger LdapConfigProvider", e);
                throw e;
            }
        }

        @Override
        public int getNumThreads() {
            try {
                return getServer().getEventLoggingNumThreads();
            } catch (ServiceException e) {
                return DEFAULT_NUM_THREADS;
            }
        }

        @Override
        public Map<String, Collection<String>> getHandlerConfig() {
            Multimap<String, String> configInfoMap = ArrayListMultimap.create();
            String[] backendConfigs;
            try {
                backendConfigs = getServer().getEventLoggingBackends();
            } catch (ServiceException e) {
                backendConfigs = DEFAULT_HANDLERS;
            }
            for (String configStr: backendConfigs) {
                String[] tokens = configStr.split(":", 2);
                String handlerFactoryName = tokens[0];
                String handlerConfig = tokens.length == 2 ? tokens[1] : "";
                configInfoMap.put(handlerFactoryName, handlerConfig);
            }
            return configInfoMap.asMap();
        }
    }

    @VisibleForTesting
    public void clearQueue() {
        eventQueue.clear();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
