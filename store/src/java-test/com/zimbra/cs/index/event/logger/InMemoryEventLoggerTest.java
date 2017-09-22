package com.zimbra.cs.index.event.logger;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.event.Event;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class InMemoryEventLoggerTest {
    private EventLogger logger;

    @Before
    public void setup() throws ServiceException {
        EventLogger.setEventLogger(InMemoryEventLogger.class);
        logger = EventLogger.getEventLogger();
    }

    @Test
    public void testLogging() {
        Event event = new Event(Event.EventType.SENT, System.currentTimeMillis(), Collections.EMPTY_MAP);
        logger.log(event);
        InMemoryEventLogger inMemoryEventLogger = (InMemoryEventLogger) logger;
        Assert.assertNotNull(inMemoryEventLogger.getLogs());
        Assert.assertEquals(1, inMemoryEventLogger.getLogs().size());
        Assert.assertNotNull(inMemoryEventLogger.getLogs().get(0));
        Assert.assertEquals(Event.EventType.SENT, inMemoryEventLogger.getLogs().get(0).getEventType());
    }
}
