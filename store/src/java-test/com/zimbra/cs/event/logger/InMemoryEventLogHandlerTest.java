package com.zimbra.cs.event.logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.Event;

import org.junit.*;
import org.mockito.Mockito;

import java.util.HashMap;

public class InMemoryEventLogHandlerTest {

    @Before
    public void init() throws ServiceException {
        EventLogger.unregisterAllHandlerFactories();
    }

    @Test
    public void testInMemoryEventLogHandler() {
        EventLogger.ConfigProvider mockConfigProvider = Mockito.mock(EventLogger.ConfigProvider.class);
        Multimap<String, String> mockConfigMap = ArrayListMultimap.create();
        mockConfigMap.put("MockFactor1", "");
        mockConfigMap.put("MockFactor2", "");
        Mockito.doReturn(mockConfigMap.asMap()).when(mockConfigProvider).getHandlerConfig();
        mockConfigMap.put("inMemoryEventLogHandlerFactory", "");
        Mockito.doReturn(mockConfigMap.asMap()).when(mockConfigProvider).getHandlerConfig();

        //Setting number of threads in executor service as 2
        Mockito.doReturn(2).when(mockConfigProvider).getNumThreads();


        //Creating mock InMemoryEventLogHandler.Factory so it can return a spy instance of InMemoryEventLogHandler
        EventLogHandler.Factory inMemoryEventLogHandlerFactory = new InMemoryEventLogHandler.Factory();
        EventLogger.registerHandlerFactory("inMemoryEventLogHandlerFactory", inMemoryEventLogHandlerFactory);

        EventLogger eventLogger = EventLogger.getEventLogger(mockConfigProvider);
        Event event = new Event("InMemoryEventLoggerTestAccountId", Event.EventType.SENT, System.currentTimeMillis());
        eventLogger.log(event);

        eventLogger.startupEventNotifierExecutor();
        eventLogger.shutdownEventLogger();

        InMemoryEventLogHandler inMemoryEventLogHandler = (InMemoryEventLogHandler) inMemoryEventLogHandlerFactory.createHandler("");

        Assert.assertNotNull(inMemoryEventLogHandler.getLogs());
        Assert.assertEquals(1, inMemoryEventLogHandler.getLogs().size());
        Assert.assertNotNull(inMemoryEventLogHandler.getLogs().get(0));
        Assert.assertEquals("InMemoryEventLoggerTestAccountId", inMemoryEventLogHandler.getLogs().get(0).getAccountId());
        Assert.assertEquals(Event.EventType.SENT, inMemoryEventLogHandler.getLogs().get(0).getEventType());
    }

    @AfterClass
    public static void cleanup() {
        EventLogger.unregisterAllHandlerFactories();
    }
}
