package com.zimbra.cs.event.analytics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.EventStore;
import com.zimbra.cs.event.analytics.RatioMetric.RatioIncrement;

/**
 * Metric that calculates the average time delta between two message event types.
 * @author iraykin
 *
 */
public class TimeDeltaMetric extends EventDifferenceMetric {

    private Map<Integer, Long> timestampMap;

    public TimeDeltaMetric(String accountId, EventDifferenceParams params) throws ServiceException {
        super(accountId, MetricType.TIME_DELTA, params);
        timestampMap = new HashMap<>();
    }

    @Override
    protected RatioIncrement getIncrement(List<Event> events) throws ServiceException {
        double cumulativeDelta = 0d;
        int numMsgs = 0;
        for (Event event: events) {
            if (eventMatchesContactAndType(event, firstEventType)) {
                timestampMap.put((Integer) event.getContextField(EventContextField.MSG_ID), event.getTimestamp());
            }
         }
        for (Event event: events) {
            if (eventMatchesContactAndType(event, secondEventType)) {
                Long firstEventTimestamp = timestampMap.get(event.getContextField(EventContextField.MSG_ID));
                if (firstEventTimestamp != null) {
                    long delta = event.getTimestamp() - firstEventTimestamp;
                    if (delta >= 500) {
                        cumulativeDelta += delta;
                        numMsgs++;
                    }
                }
            }
         }
        return new RatioIncrement(cumulativeDelta / 1000, numMsgs);
    }

    public static class TimeDeltaInitializer extends EventStoreInitializer<RatioMetric, Double, RatioIncrement> {

        private EventDifferenceParams params;

        public TimeDeltaInitializer(EventStore eventStore, EventDifferenceParams params) {
            super(eventStore);
            this.params = params;
        }

        @Override
        public RatioMetric getInitialData() throws ServiceException {
            String contact = params.getContactEmail();
            if (Strings.isNullOrEmpty(contact)) {
                return getEventStore().getGlobalEventTimeDelta(params.getFirstEventType(), params.getSecondEventType());
            } else {
                return getEventStore().getEventTimeDelta(params.getFirstEventType(), params.getSecondEventType(), contact);
            }
        }

        @Override
        public long getMetricLifetime() {
            return 0;
        }
    }


    public static class Factory implements EventMetric.Factory<RatioMetric, Double, RatioIncrement> {

        public String contactEmail;

        @Override
        public EventMetric<RatioMetric, Double, RatioIncrement> buildMetric(String accountId, MetricParams<RatioMetric, Double, RatioIncrement> params) throws ServiceException {
            EventDifferenceParams rrParams = (EventDifferenceParams) params;
            return new TimeDeltaMetric(accountId, rrParams);
        }
    }
}
