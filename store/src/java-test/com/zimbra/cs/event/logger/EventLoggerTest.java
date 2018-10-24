package com.zimbra.cs.event.logger;

import static org.junit.Assert.assertEquals;
import org.junit.Ignore;
import static org.junit.Assert.assertFalse;

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
import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.filter.IncomingMessageHandler;
import com.zimbra.cs.filter.jsieve.ActionFlag;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Message.EventFlag;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class EventLoggerTest {
    private static String USER_EMAIL = "eventloggertest@zimbra.com";
    private static String SENDER_EMAIL = "test@zimbra.com";
    private static String DATASOURCE_ID = "testDataSourceID";
    private Mailbox mbox;
    EventLogger eventLogger;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount(USER_EMAIL, "test123", new HashMap<String, Object>());
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
    public void testEachRegisteredLogHandlerReceivesEvent() throws Exception {
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
    public void testEventLoggerShutdown() throws Exception {
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

    private EventLogHandler getMockHandler() throws Exception {
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
        return mockHandler;
    }

    @Test
    public void testReceivedEvent() throws Exception {
        EventLogHandler mockHandler = getMockHandler();

        ParsedMessage pm = buildIncomingMessage();

        //create an IncomingMessageHandler and trigger it to keep a message
        IncomingMessageHandler handler = new IncomingMessageHandler(new OperationContext(mbox), new DeliveryContext(),
                mbox, USER_EMAIL, pm, 0, Mailbox.ID_FOLDER_INBOX, true);
        List<ActionFlag> flags = new ArrayList<>();
        handler.explicitKeep(flags, new String[]{});

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(mockHandler, Mockito.times(1)).log(captor.capture());
        Assert.assertNotNull(captor.getAllValues());
        Assert.assertEquals(1, captor.getAllValues().size());
        Event event = captor.getAllValues().get(0);
        Assert.assertEquals("incorrect event type logged", Event.EventType.RECEIVED, event.getEventType());
        Assert.assertEquals("incorrect sender logged", SENDER_EMAIL, event.getContextField(Event.EventContextField.SENDER));
        Assert.assertEquals("incorrect receiver logged", USER_EMAIL, event.getContextField(Event.EventContextField.RECEIVER));
    }

    private void verifyEvent(Event event, EventType type) {
        assertEquals("event has incorrect account ID", mbox.getAccountId(), event.getAccountId());
        assertEquals("event has incorrect datasource ID",DATASOURCE_ID, event.getDataSourceId());
        assertEquals("event has incorrect sender", SENDER_EMAIL, event.getContextField(EventContextField.SENDER));
        Assert.assertEquals("event has incorrect EventType", type, event.getEventType());
    }

    private ParsedMessage buildIncomingMessage() throws Exception {
        MimeMessage mm = new MimeMessage(Session.getInstance(new Properties()));
        mm.setFrom(new InternetAddress(SENDER_EMAIL));
        mm.setRecipients(RecipientType.TO, USER_EMAIL);
        mm.saveChanges();
        return new ParsedMessage(mm, false);
    }

    @Test
    public void testMessageEvents() throws Exception {

        EventLogHandler mockHandler = getMockHandler();
        ParsedMessage pm = buildIncomingMessage();

        DeliveryOptions dopt = new DeliveryOptions();
        dopt.setFlags(Flag.BITMASK_UNREAD);
        dopt.setDataSourceId(DATASOURCE_ID);
        dopt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        dopt.setRecipientEmail(USER_EMAIL);
        Message msg = mbox.addMessage(null, pm, dopt, null);

        assertFalse("message is not from me", msg.isFromMe());
        assertEquals("incorrect datasource ID", DATASOURCE_ID, msg.getDataSourceId());

        msg.advanceEventFlag(EventFlag.seen);
        msg.advanceEventFlag(EventFlag.seen);
        msg.advanceEventFlag(EventFlag.seen);

        mbox.alterTag(null, msg.getId(), MailItem.Type.MESSAGE, FlagInfo.UNREAD, false, null);
        mbox.alterTag(null, msg.getId(), MailItem.Type.MESSAGE, FlagInfo.UNREAD, false, null);
        mbox.alterTag(null, msg.getId(), MailItem.Type.MESSAGE, FlagInfo.UNREAD, false, null);

        mbox.alterTag(null, msg.getId(), MailItem.Type.MESSAGE, FlagInfo.REPLIED, true, null);
        mbox.alterTag(null, msg.getId(), MailItem.Type.MESSAGE, FlagInfo.REPLIED, true, null);
        mbox.alterTag(null, msg.getId(), MailItem.Type.MESSAGE, FlagInfo.REPLIED, true, null);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(mockHandler, Mockito.times(3)).log(captor.capture());
        Assert.assertNotNull(captor.getAllValues());
        Assert.assertEquals(3, captor.getAllValues().size());
        List<Event> events = captor.getAllValues();
        verifyEvent(events.get(0), EventType.SEEN);
        verifyEvent(events.get(1), EventType.READ);
        verifyEvent(events.get(2), EventType.REPLIED);
    }
}
