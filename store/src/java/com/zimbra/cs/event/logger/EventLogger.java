package com.zimbra.cs.event.logger;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;

public abstract class EventLogger {
    public abstract void log(Event event);

    protected static EventLogger eventLogger;

    public static final void setEventLogger(Class<? extends EventLogger> loggerClass) throws ServiceException {
        String className = loggerClass.getName();
        ZimbraLog.event.info("setting EventLogger class %s", className);
        try {
            eventLogger = loggerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw ServiceException.FAILURE(String.format("unable to initialize EventLogger setEventLogger %s", className), e);
        }
    }

    public static EventLogger getEventLogger() {
        return eventLogger;
    }
}