package com.zimbra.cs.event.logger;

import com.google.common.collect.Maps;
import com.zimbra.cs.event.Event;
import org.junit.*;
import org.mockito.Mockito;

import java.util.HashMap;

public class EventLoggerTest {
    EventLogger eventLogger;

    @After
    public void cleanup() throws InterruptedException {
        eventLogger.shutdownEventLogger();
        EventLogger.unregisterAllHandlerFactories();
    }

    @Test
    public void testEachRegisteredLogHandlerReceivesEvent() throws InterruptedException {
        //Creating a mock config provider to create and instance of event logger with it
        EventLogger.ConfigProvider mockConfigProvider = Mockito.mock(EventLogger.ConfigProvider.class);
        HashMap<String, String> fakeConfigMap = Maps.newHashMap();
        fakeConfigMap.put("MockFactor1", "");
        fakeConfigMap.put("MockFactor2", "");
        Mockito.doReturn(fakeConfigMap).when(mockConfigProvider).getHandlerConfig();

        //Setting number of threads in executor service as 2
        Mockito.doReturn(2).when(mockConfigProvider).getNumThreads();

        eventLogger = EventLogger.getEventLogger(mockConfigProvider);

        //Creating mock log handler factories
        EventLogHandler.Factory mockFactory1 = Mockito.mock(EventLogHandler.Factory.class);
        EventLogHandler.Factory mockFactory2 = Mockito.mock(EventLogHandler.Factory.class);

        //Creating mock log handlers
        EventLogHandler mockHandler1 = Mockito.mock(EventLogHandler.class);
        EventLogHandler mockHandler2 = Mockito.mock(EventLogHandler.class);

        //Setting mockito to return the mock log handlers when createHandler() is called on the mock handlers
        Mockito.doReturn(mockHandler1).when(mockFactory1).createHandler(Mockito.anyString());
        Mockito.doReturn(mockHandler2).when(mockFactory2).createHandler(Mockito.anyString());

        //Registering mock log handler factories
        EventLogger.registerHandlerFactory("MockFactor1", mockFactory1);
        EventLogger.registerHandlerFactory("MockFactor2", mockFactory2);

        eventLogger.startupEventNotifierExecutor();

        Event event = new Event("testEventLoggerId", Event.EventType.SENT, 1L);
        eventLogger.log(event);
        eventLogger.log(event);

        //Verifying that both the mock log handlers are notified about the 2 logged events
        Mockito.verify(mockHandler1, Mockito.times(2)).log(event);
        Mockito.verify(mockHandler1, Mockito.times(2)).log(event);
    }

    @Test
    public void testEventLoggerShutdown() throws InterruptedException {
        //Creating a mock config provider to create and instance of event logger with it
        EventLogger.ConfigProvider mockConfigProvider = Mockito.mock(EventLogger.ConfigProvider.class);
        HashMap<String, String> testConfigMap = Maps.newHashMap();
        testConfigMap.put("mockInMemoryEventLogHandlerFactory", "");
        Mockito.doReturn(testConfigMap).when(mockConfigProvider).getHandlerConfig();

        //Setting number of threads in executor service as 2
        Mockito.doReturn(2).when(mockConfigProvider).getNumThreads();


        //Creating mock InMemoryEventLogHandler.Factory so it can return a spy instance of InMemoryEventLogHandler
        EventLogHandler.Factory mockInMemoryEventLogHandlerFactory = Mockito.mock(InMemoryEventLogHandler.Factory.class);
        InMemoryEventLogHandler spyInMemoryEventLogHandler = Mockito.spy(new InMemoryEventLogHandler());
        Mockito.doReturn(spyInMemoryEventLogHandler).when(mockInMemoryEventLogHandlerFactory).createHandler(Mockito.anyString());
        EventLogger.registerHandlerFactory("mockInMemoryEventLogHandlerFactory", mockInMemoryEventLogHandlerFactory);

        eventLogger = EventLogger.getEventLogger(mockConfigProvider);

        Event event = new Event("testEventLoggerId", Event.EventType.SENT, 1L);
        for (int i = 0; i < 200; i++) {
            eventLogger.log(event);
        }

        eventLogger.startupEventNotifierExecutor();
        eventLogger.shutdownEventLogger();

        /* Verify that log method for spyInMemoryEventLogHandler is called by at least 200 times
        which is equal to number of events logged in the for loop above */
        Mockito.verify(spyInMemoryEventLogHandler, Mockito.atLeast(200)).log(event);

        //Verify that shutdown method for spyInMemoryEventLogHandler is called by each thread once.
        Mockito.verify(spyInMemoryEventLogHandler, Mockito.times(2)).shutdown();

        Assert.assertTrue("At least 200 events should be logged in", spyInMemoryEventLogHandler.getLogs().size() >= 200);
    }
}
