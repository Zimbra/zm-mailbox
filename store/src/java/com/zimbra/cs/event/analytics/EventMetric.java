package com.zimbra.cs.event.analytics;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.EventStore;
import com.zimbra.cs.event.analytics.IncrementableMetric.Increment;

/**
 * Representation of a metric calculated from event data.
 * A metric can be be incrementally updated in-memory to avoid having to
 * query the event store every time.
 * @author iraykin
 *
 * @param <T> the type of the underlying {@link IncrementableMetric}
 * @param <S> the type returned by getValue()
 * @param <I> the type of the {@link Increment} used to update the internal state of the metric.
 */
public abstract class EventMetric<T extends IncrementableMetric<S, I>, S, I extends Increment> {

    public static enum MetricType {
        CONTACT_FREQUENCY,
        EVENT_RATIO,
        TIME_DELTA,
    }

    protected String accountId;
    private MetricInitializer<T, S, I> initializer;
    protected MetricType type;
    protected T metricData;
    protected long timeInitialized;
    protected long metricLifetime;

    public EventMetric(String accountId, MetricType type, MetricInitializer<T, S, I> initializer) throws ServiceException {
        this.accountId = accountId;
        this.type = type;
        this.initializer = initializer;
        init();
    }

    public void init() throws ServiceException {
        this.metricData = initializer.getInitialData();
        this.timeInitialized = initializer.getTimestamp();
    }

    /**
     * Increment the metric value based on one or more events.
     * It is up to the implementation to determine which events apply the metric
     * and filter appropriately
     */
    public void increment(List<Event> events) throws ServiceException {
        I inc = getIncrement(events);
        ZimbraLog.event.debug("incrementing %s by %s", this, inc);
        metricData.increment(inc);
    }

    /**
     * Return the {@link IncrementableMetric.Increment}  from the list of events
     */
    protected abstract I getIncrement(List<Event> events) throws ServiceException;

    /**
     * Get the value of this metric
     */
    public S getValue() {
        long lifetime = initializer.getMetricLifetime();
        if (lifetime > 0 && timeInitialized + lifetime < System.currentTimeMillis()) {
            try {
                ZimbraLog.event.info("re-initializing metric %s", type.name());
                init();
            } catch (ServiceException e) {
                ZimbraLog.event.error("error re-initializing event metric %s, will continue to use known value", type.name(), e);
            }
        }
        return metricData.getValue();
    }


    /**
     * Helper interface used to initialize EventMetric
     */
    public static abstract class MetricInitializer<T extends IncrementableMetric<S, I>, S, I extends Increment> {

        public abstract T getInitialData() throws ServiceException;

        public long getTimestamp() {
            return System.currentTimeMillis();
        }

        public abstract long getMetricLifetime();
    }

    /**
     * Abstract class used to initialize EventMetric values from an EventStore
     */
    public static abstract class EventStoreInitializer<T extends IncrementableMetric<S, I>, S, I extends Increment> extends MetricInitializer<T, S, I> {

        private EventStore eventStore;

        public EventStoreInitializer(EventStore eventStore) {
            this.eventStore = eventStore;
        }

        protected EventStore getEventStore() {
            return eventStore;
        }
    }

    /**
     * Class defining parameters for an EventMetric.
     * The base class lets the caller specify a custom MetricInitializer
     * and the maximum length of time
     * subclasses can provide further arguments
     */
    public static abstract class MetricParams<T extends IncrementableMetric<S, I>, S, I extends Increment> {
        private MetricInitializer<T, S, I> initializer;

        public void setInitializer(MetricInitializer<T, S, I> initializer) {
            this.initializer = initializer;
        }

        public MetricInitializer<T, S, I> getInitializer() {
            return initializer;
        }
    }

    /**
     * Factory interface for building EventMetric instances
     */
    public static interface Factory<T extends IncrementableMetric<S, I>, S, I extends Increment> {

        /**
         * Return an EventMetric instance for the specified account ID with the given parameters
         */
        public abstract EventMetric<T, S, I> buildMetric(String accountId, MetricParams<T, S, I> params) throws ServiceException;
    }
}
