package com.zimbra.cs.event.logger;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.Event;
import org.junit.*;
import org.mockito.Mockito;

import java.util.Collections;

public class InMemoryEventLogHandlerTest {
    InMemoryEventLogHandler inMemoryEventLogHandler = new InMemoryEventLogHandler();

    @Before
    public void init() throws ServiceException {
        EventLogger.getEventLogger().unregisterAllEventLogHandlers();
    }

    @Test
    public void testLogging() {
        EventLogger eventLogger = EventLogger.getEventLogger();
        eventLogger.registerEventLogHandler(inMemoryEventLogHandler);

        Event event = new Event("InMemoryEventLoggerTestAccountId", Event.EventType.SENT, System.currentTimeMillis(), Collections.EMPTY_MAP);
        eventLogger.log(event);

        Assert.assertNotNull(inMemoryEventLogHandler.getLogs());
        Assert.assertEquals(1, inMemoryEventLogHandler.getLogs().size());
        Assert.assertNotNull(inMemoryEventLogHandler.getLogs().get(0));
        Assert.assertEquals("InMemoryEventLoggerTestAccountId", inMemoryEventLogHandler.getLogs().get(0).getAccountId());
        Assert.assertEquals(Event.EventType.SENT, inMemoryEventLogHandler.getLogs().get(0).getEventType());
    }

    @AfterClass
    public static void cleanup() {
        EventLogger.getEventLogger().unregisterAllEventLogHandlers();
    }
}
