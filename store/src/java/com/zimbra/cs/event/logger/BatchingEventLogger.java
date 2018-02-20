package com.zimbra.cs.event.logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.event.Event;

/**
 * Event logging backend that batches events. The handling of batched events is delegated to
 * BatchedEventCallback implementations.
 */
public class BatchingEventLogger implements EventLogHandler {

    private BatchedEventCallback callback;
    private int batchSize;

    /**
     * This cache accumulates events on a per-account basis.
     */
    private Cache<String, BatchedEvents> batchedEvents;


    public BatchingEventLogger(int batchSize, long expireMillis, BatchedEventCallback callback) {
        this.batchSize = batchSize;
        setCallback(callback);
        initCache(batchSize, expireMillis);
    }

    protected void setCallback(BatchedEventCallback callback) {
        this.callback = callback;
    }

    private void initCache(int batchSize, long expireMillis) {
        CacheBuilder<String, BatchedEvents> builder = CacheBuilder.newBuilder()
        .removalListener(new RemovalListener<String, BatchedEvents>() {
            @Override
            public void onRemoval(RemovalNotification<String, BatchedEvents> notification) {
                BatchedEvents batchedEvents = notification.getValue();
                batchedEvents.send();
            }
        });
        if (expireMillis > 0) {
            builder.expireAfterAccess(expireMillis, TimeUnit.MILLISECONDS);
        }
        batchedEvents = builder.build();
    }

    @Override
    public void log(Event event) {
        String accountId = event.getAccountId();
        try {
            BatchedEvents batch = batchedEvents.get(accountId, new Callable<BatchedEvents>() {

                @Override
                public BatchedEvents call() throws Exception {
                    return new BatchedEvents(accountId, batchSize, callback);
                }
            });
            batch.addEvent(event);
        } catch (ExecutionException e) {}
    }

    @Override
    public void shutdown() {
        sendAllBatched();
        try {
            callback.close();
        } catch (IOException e) {
            ZimbraLog.misc.error("Cought an exception trying to close BatchingEventLogger callback", e);
        }
    }

    @VisibleForTesting
    protected Cache<String, BatchedEvents> getBatchedEventCache() {
        return batchedEvents;
    }

    /**
     * Callback interface for processing a batch of events for a given accountId.
     */
    static interface BatchedEventCallback extends Closeable {
        void execute(String accountId, List<Event> events);
    }

    /**
     * Class encapsulating a set of batched events. When the batch limit is reached,
     * the event documents are grouped into an UpdateRequest and passed to the callback.
     */
    static class BatchedEvents {
        private List<Event> events = new ArrayList<Event>();
        private String accountId;
        private int batchSize;
        private BatchedEventCallback callback;

        public BatchedEvents(String accountId, int batchSize, BatchedEventCallback callback) {
            this.accountId = accountId;
            this.batchSize = batchSize;
            this.callback = callback;
        }

        public void addEvent(Event event) {
            events.add(event);
            if (events.size() >= batchSize) {
                send();
            }
        }

        private void send() {
            if (events.isEmpty()) {
                return;
            }
            callback.execute(accountId, events);
            events.clear();
        }

        public int getCurBatchSize() {
            return events.size();
        }
    }

    public void sendAllBatched() {
        batchedEvents.invalidateAll();
    }

    @VisibleForTesting
    void cleanCache() {
        batchedEvents.cleanUp();
    }

    static abstract class BatchingHandlerFactory implements EventLogHandler.Factory {

        private static final int DEFAULT_BATCH_SIZE = 100;
        private static final long DEFAULT_BATCH_EXPIRY = 1000 * 60 * 5;

        protected Server getLocalServer() throws ServiceException {
            return Provisioning.getInstance().getLocalServer();
        }

        protected int getBatchSize() {
            try {
                return getLocalServer().getEventBatchMaxSize();
            } catch (ServiceException e) {
                ZimbraLog.event.error("Unable to determine event batch size; defaulting to %d", DEFAULT_BATCH_SIZE, e);
                return DEFAULT_BATCH_SIZE;
            }
        }

        protected long getCacheExpiryMillis() {
            try {
                return getLocalServer().getEventBatchLifetime();
            } catch (ServiceException e) {
                ZimbraLog.event.error("Unable to determine event batch expiry; defaulting to %d", DEFAULT_BATCH_EXPIRY, e);
                return DEFAULT_BATCH_EXPIRY;
            }
        }

        protected abstract BatchedEventCallback createCallback(String config) throws ServiceException;

        @Override
        public EventLogHandler createHandler(String config) throws ServiceException {
            return new BatchingEventLogger(getBatchSize(), getCacheExpiryMillis(), createCallback(config));
        }
    }

    @Override
    public boolean acceptsInternalEvents() {
        return false;
    }
}
