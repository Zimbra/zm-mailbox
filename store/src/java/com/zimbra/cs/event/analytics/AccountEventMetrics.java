package com.zimbra.cs.event.analytics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.MoreObjects;
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
    public <M extends IncrementableMetric<T, I>, T, I extends Increment> EventMetric<M, T, I> getMetric(MetricKey<M, T, I> key) throws ServiceException {
        EventMetric<M, T, I> metric = (EventMetric<M, T, I>) metrics.get(key);
        if (metric == null) {
            MetricType type = key.getType();
            EventMetric.Factory<M, T, I> factory = (Factory<M, T, I>) factories.get(type);
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
    public static class MetricKey<M extends IncrementableMetric<T, I>, T, I extends Increment> extends Pair<MetricType, MetricParams<M, T, I>> {

        public MetricKey(MetricType type, MetricParams<M, T, I> params) {
            super(type, params);
        }

        public MetricType getType() {
            return getFirst();
        }

        public MetricParams<M, T, I> getParams() {
            return getSecond();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("type", getType())
                    .add("params", getParams()).toString();
        }
    }

    public void evictMetricCache() {
        metrics.clear();
    }
}
