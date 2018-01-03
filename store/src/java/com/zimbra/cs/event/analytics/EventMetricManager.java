package com.zimbra.cs.event.analytics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.cs.event.analytics.EventMetric.MetricType;

public class EventMetricManager {

    private static Map<EventMetric.MetricType, EventMetric.Factory<?, ?, ?>> factories = new HashMap<>();

    static {
        registerMetricFactory(MetricType.CONTACT_FREQUENCY, new ContactFrequencyMetric.Factory());
        registerMetricFactory(MetricType.EVENT_RATIO, new EventRatioMetric.Factory());
        registerMetricFactory(MetricType.TIME_DELTA, new TimeDeltaMetric.Factory());
    }

    private Map<String, AccountEventMetrics> accountMap;

    private static EventMetricManager instance = new EventMetricManager();

    public static EventMetricManager getInstance() {
        return instance;
    }

    private EventMetricManager() {
        accountMap = new ConcurrentHashMap<>();
    }

    public AccountEventMetrics getMetrics(String accountId) {
        AccountEventMetrics metricSet = accountMap.get(accountId);
        if (metricSet == null) {
            metricSet = new AccountEventMetrics(accountId, factories);
            accountMap.put(accountId, metricSet);
        }
        return metricSet;
    }

    public static void registerMetricFactory(MetricType type, EventMetric.Factory<?, ?, ?> factory) {
        factories.put(type, factory);
    }

    /**
     * Remove all EventMetric instances from the cache.
     */
    public void clearMetrics() {
        accountMap.clear();
    }

    /**
     * Remove all EventMetric instances for an account
     */
    public void clearAccountMetrics(String accountId) {
        if (accountMap.containsKey(accountId)) {
            accountMap.get(accountId).evictMetricCache();
        }
    }
}
