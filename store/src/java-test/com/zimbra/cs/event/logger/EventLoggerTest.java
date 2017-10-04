package com.zimbra.cs.event.logger;

import com.zimbra.cs.event.Event;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class EventLoggerTest {
    EventLogger eventLogger = EventLogger.getEventLogger();
    EventLogHandler mock2 = Mockito.mock(EventLogHandler.class);
    EventLogHandler mock1 = Mockito.mock(EventLogHandler.class);

    @Before
    public void init() {
        eventLogger.unregisterAllEventLogHandlers();
    }

    @Test
    public void testEventLogger() {
        eventLogger.registerEventLogHandler(mock1);

        Event event = new Event("eventLoggerAccountId", Event.EventType.SENT, System.currentTimeMillis());

        eventLogger.log(event);
        Mockito.verify(mock1, Mockito.times(1)).log(event);

        eventLogger.registerEventLogHandler(mock2);
        eventLogger.log(event);
        Mockito.verify(mock1, Mockito.times(2)).log(event);
        Mockito.verify(mock2, Mockito.times(1)).log(event);
    }

    @Test
    public void testUnregisterEventLogger() {
        eventLogger.registerEventLogHandler(mock1);
        Assert.assertTrue(eventLogger.unregisterEventLogHandler(mock1));
        Assert.assertFalse(eventLogger.unregisterEventLogHandler(mock2));
    }

    @AfterClass
    public static void cleanup() {
        EventLogger.getEventLogger().unregisterAllEventLogHandlers();
    }
}
