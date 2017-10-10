package com.zimbra.cs.event.logger;

import com.zimbra.cs.event.Event;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class EventLoggerTest {
    EventLogger eventLogger = EventLogger.getEventLogger();
    EventLogHandler mock2 = Mockito.mock(InMemoryEventLogHandler.class);
    EventLogHandler mock1 = Mockito.mock(InMemoryEventLogHandler.class);

    @Before
    public void init() {
        eventLogger.unregisterAllEventLogHandlers();
    }

    @Test
    public void testEventLogger() throws InterruptedException {
        eventLogger.registerEventLogHandler(new EventLogHandler() {
            private AtomicInteger eventCount = new AtomicInteger(0);
            @Override
            public void log(Event event) {
                eventCount.getAndIncrement();
            }

            @Override
            public void shutdown() {
                System.out.println("Logger 1 " + eventCount.get());
            }
        });

        Event event = new Event("eventLoggerAccountId", Event.EventType.SENT, System.currentTimeMillis());

        Runnable producer = () -> {
            Random generator = new Random(System.currentTimeMillis());
            for (int i = 0; i < 100; i++) {
                eventLogger.log(event);
                if(generator.nextBoolean()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(producer).start();

        //eventLogger.log(event);
        //Mockito.verify(mock1, Mockito.times(1)).log(event);

        eventLogger.registerEventLogHandler(new EventLogHandler() {
            private AtomicInteger eventCount = new AtomicInteger(0);
            @Override
            public void log(Event event) {
                eventCount.getAndIncrement();
            }

            @Override
            public void shutdown() {
                System.out.println("Logger 2 " + eventCount.get());
            }
        });

        new Thread(producer).start();
        //eventLogger.log(event);
        //Mockito.verify(mock1, Mockito.times(2)).log(event);
        //Mockito.verify(mock2, Mockito.times(1)).log(event);
        eventLogger.shutdownEventNotifierExecutor();
        Thread.sleep(5000);
        eventLogger.startupEventNotifierExecutor();
        eventLogger.shutdowEventLogger();
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
