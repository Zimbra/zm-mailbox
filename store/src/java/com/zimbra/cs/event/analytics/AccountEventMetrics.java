package com.zimbra.cs.event.analytics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.analytics.EventMetric.Factory;
import com.zimbra.cs.event.analytics.EventMetric.MetricParams;
import com.zimbra.cs.event.analytics.EventMetric.MetricType;
import com.zimbra.cs.event.analytics.IncrementableMetric.Increment;

/**
 * Class encapsulating all event metrics for an account
 */
public class AccountEventMetrics {

    private String accountId;
    private Map<EventMetric.MetricType, EventMetric.Factory<?, ?, ?>> factories;
    private Map<MetricKey<?, ?, ?>, EventMetric<?, ?, ?>> metrics;

    public AccountEventMetrics(String accountId, Map<EventMetric.MetricType, EventMetric.Factory<?, ?, ?>> factories) {
        this.accountId = accountId;
        this.factories = factories;
        this.metrics = new HashMap<>();
    }

    /**
     * Return the specified EventMetric instance. If not loaded,
     * the metric will be initialized first.
     */
    @SuppressWarnings("unchecked")
    public <T extends IncrementableMetric<S, I>, S, I extends Increment> EventMetric<T, S, I> getMetric(MetricKey<T, S, I> key) throws ServiceException {
        EventMetric<T, S, I> metric = (EventMetric<T, S, I>) metrics.get(key);
        if (metric == null) {
            MetricType type = key.getType();
            EventMetric.Factory<T, S, I> factory = (Factory<T, S, I>) factories.get(type);
            if (factory == null) {
                throw ServiceException.FAILURE(String.format("no EventMetric factory found for metric type %s",  type.name()), null);
            }
            ZimbraLog.event.debug("initializing event metric %s", key);
            metric = factory.buildMetric(accountId, key.getParams());
            metrics.put(key, metric);
        }
        return metric;
    }

    /**
     * Update all loaded EventMetric instances
     */
    public void incrementAll(List<Event> events) throws ServiceException {
        for (EventMetric<?, ?, ?> metric: metrics.values()) {
            metric.increment(events);
        }
    }

    /**
     * Cache key used for accessing an EventMetric instance.
     * If the EventMetric for the given type and parameters does not exist,
     * it is instantiated using the specified MetricParams.
     */
    public static class MetricKey<T extends IncrementableMetric<S, I>, S, I extends Increment> extends Pair<MetricType, MetricParams<T, S, I>> {

        public MetricKey(MetricType type, MetricParams<T, S, I> params) {
            super(type, params);
        }

        public MetricType getType() {
            return getFirst();
        }

        public MetricParams<T, S, I> getParams() {
            return getSecond();
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("type", getType())
                    .add("params", getParams()).toString();
        }
    }

    public void evictMetricCache() {
        metrics.clear();
    }
}