package com.zimbra.cs.event.analytics;

import java.util.List;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.EventStore;
import com.zimbra.cs.event.analytics.RatioMetric.RatioIncrement;

/**
 * Event metric that calculates the ratio of the count of two event types, either for a given contact or globally
 * @author iraykin
 *
 */
public class EventRatioMetric extends EventDifferenceMetric {

    public EventRatioMetric(String accountId, EventDifferenceParams params) throws ServiceException {
        super(accountId, MetricType.EVENT_RATIO, params);
    }

    @Override
    protected RatioIncrement getIncrement(List<Event> events) throws ServiceException {
        int numeratorInc = (int) events.stream().filter(event -> eventMatchesContactAndType(event, firstEventType)).count();
        int denominatorInc = (int) events.stream().filter(event -> eventMatchesContactAndType(event, secondEventType)).count();
        return new RatioIncrement((double) numeratorInc, denominatorInc);
    }

    public static class EventRatioInitializer extends EventStoreInitializer<RatioMetric, Double, RatioIncrement> {

        private EventDifferenceParams params;

        public EventRatioInitializer(EventStore eventStore, EventDifferenceParams params) {
            super(eventStore);
            this.params = params;
        }

        @Override
        public RatioMetric getInitialData() throws ServiceException {
            String contact = params.getContactEmail();
            if (Strings.isNullOrEmpty(contact)) {
                return getEventStore().getGlobalEventRatio(params.getFirstEventType(), params.getSecondEventType());
            } else {
                return getEventStore().getEventRatio(params.getFirstEventType(), params.getSecondEventType(), contact);
            }
        }

        @Override
        public long getMetricLifetime() {
            return 0;
        }
    }

    public static class Factory implements EventMetric.Factory<RatioMetric, Double, RatioIncrement> {

        @Override
        public EventMetric<RatioMetric, Double, RatioIncrement> buildMetric(String accountId, MetricParams<RatioMetric, Double, RatioIncrement> params) throws ServiceException {
            EventDifferenceParams eventDiffParams = (EventDifferenceParams) params;
            return new EventRatioMetric(accountId, eventDiffParams);
        }
    }
}
