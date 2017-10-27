package com.zimbra.cs.event.logger;

import static org.junit.Assert.*;

import org.junit.Test;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventContextField;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.logger.FileEventLogHandler.EventFormatter;

public class FileEventLoggerTest {

    @Test
    public void testEventFormatter() throws Exception {
        EventFormatter fmt = new FileEventLogHandler.ZimbraEventFormatter();
        long timestamp = System.currentTimeMillis();
        Event event = new Event("testid", EventType.SENT, timestamp);
        event.setContextField(EventContextField.SENDER, "sender");
        event.setContextField(EventContextField.RECEIVER, "receiver");
        String formatted = fmt.toLogString(event);
        ZimbraLog.test.info("FORMATTED: %s", formatted);
        Event deserialized = fmt.fromLogString(formatted);
        ZimbraLog.test.info("DESERIALIZED: %s", deserialized);
        assertEquals("events are different", event, deserialized);
    }
}
