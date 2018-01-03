package com.zimbra.cs.event.logger;

import java.io.IOException;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.analytics.AccountEventMetrics;
import com.zimbra.cs.event.analytics.EventMetricManager;

/**
 * Callback for a BatchingEventLogger used to update in-memory EventMetric instances
 */
public class EventMetricCallback implements BatchingEventLogger.BatchedEventCallback {

    @Override
    public void close() throws IOException {
        //nothing to do here
    }

    @Override
    public void execute(String accountId, List<Event> events) {
        AccountEventMetrics metrics = EventMetricManager.getInstance().getMetrics(accountId);
        try {
            metrics.incrementAll(events);
        } catch (ServiceException e) {
            ZimbraLog.event.error("error incrementing EventMetrics for account %s", accountId);
        }
    }

}
