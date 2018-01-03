package com.zimbra.cs.event.logger;

import com.zimbra.cs.event.logger.BatchingEventLogger.BatchedEventCallback;
import com.zimbra.cs.event.logger.BatchingEventLogger.BatchingHandlerFactory;

public class EventMetricUpdateFactory extends BatchingHandlerFactory {

    @Override
    protected BatchedEventCallback createCallback(String config) {
        return new EventMetricCallback();
    }

}