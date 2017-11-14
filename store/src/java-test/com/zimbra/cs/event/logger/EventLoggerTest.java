package com.zimbra.cs.event.logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.filter.IncomingMessageHandler;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;

public class EventLoggerTest {
    EventLogger eventLogger;
    private Mailbox mbox;
    private String USER_NAME = "eventloggertest";
    private String USER_EMAIL = "eventloggertest@zimbra.com";

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount(USER_NAME, "test123", new HashMap<String, Object>());
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
    }

    @After
    public void tearDown() throws Exception {
        if (eventLogger != null) {
            eventLogger.shutdownEventLogger();
        }
        EventLogger.unregisterAllHandlerFactories();
        MailboxTestUtil.clearData();
    }

    @Test
    public void testEachRegisteredLogHandlerReceivesEvent() throws InterruptedException {
        //Creating a mock config provider to create and instance of event logger with it
        EventLogger.ConfigProvider mockConfigProvider = Mockito.mock(EventLogger.ConfigProvider.class);
        Multimap<String, String> mockConfigMap = ArrayListMultimap.create();
        mockConfigMap.put("MockFactor1", "");
        mockConfigMap.put("MockFactor2", "");
        Mockito.doReturn(mockConfigMap.asMap()).when(mockConfigProvider).getHandlerConfig();
        Mockito.doReturn(true).when(mockConfigProvider).isEnabled();
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
        Multimap<String, String> mockConfigMap = ArrayListMultimap.create();
        mockConfigMap.put("mockInMemoryEventLogHandlerFactory", "");
        Mockito.doReturn(mockConfigMap.asMap()).when(mockConfigProvider).getHandlerConfig();
        Mockito.doReturn(true).when(mockConfigProvider).isEnabled();

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

    @Test
    public void testReceivedEvent() throws Exception {
        EventLogHandler.Factory mockFactory = Mockito.mock(EventLogHandler.Factory.class);
        EventLogHandler mockHandler = Mockito.mock(EventLogHandler.class);
        Mockito.doReturn(mockHandler).when(mockFactory).createHandler(Mockito.anyString());

        EventLogger.registerHandlerFactory("testhandler", mockFactory);

        EventLogger.ConfigProvider mockConfigProvider = Mockito.mock(EventLogger.ConfigProvider.class);

        Multimap<String, String> mockConfigMap = ArrayListMultimap.create();
        mockConfigMap.put("testhandler", "");
        Mockito.doReturn(mockConfigMap.asMap()).when(mockConfigProvider).getHandlerConfig();

        Mockito.doReturn(1).when(mockConfigProvider).getNumThreads(); //ensures sequential event processing
        Mockito.doReturn(true).when(mockConfigProvider).isEnabled();

        EventLogger.getEventLogger(mockConfigProvider).startupEventNotifierExecutor();

        MimeMessage mm = new MimeMessage(Session.getInstance(new Properties()));
        mm.setFrom(new InternetAddress("test@zimbra.com"));
        mm.setRecipients(RecipientType.TO, USER_EMAIL);
        mm.saveChanges();
        ParsedMessage pm = new ParsedMessage(mm, false);

        //create an IncomingMessageHandler and trigger it to keep a message
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mbox), new DeliveryContext(),
                mbox, USER_EMAIL, pm, 0, Mailbox.ID_FOLDER_INBOX, true);
        List<ActionFlag> flags = new ArrayList<>();
        handler.explicitKeep(flags, new String[]{});

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(mockHandler, Mockito.times(1)).log(captor.capture());
        Assert.assertNotNull(captor.getAllValues());
        Assert.assertEquals(1, captor.getAllValues().size());
        Assert.assertEquals(Event.EventType.RECEIVED, captor.getAllValues().get(0).getEventType());
        Assert.assertEquals("test@zimbra.com", captor.getAllValues().get(0).getContextField(Event.EventContextField.SENDER));
        Assert.assertEquals(USER_EMAIL, captor.getAllValues().get(0).getContextField(Event.EventContextField.RECEIVER));
    }
}
