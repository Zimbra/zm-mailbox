package com.zimbra.cs.event.logger;

import com.zimbra.cs.event.Event;

import java.util.ArrayList;
import java.util.List;

public class EventLoggerUtils {
    public static List<Event> getEventForEachRecipient(Event event) {
        List<Event> events = new ArrayList<>();
        String receiver = (String) event.getContextField(Event.EventContextField.RECEIVER);
        String[] recipients = receiver.split(Event.MULTI_VALUE_SEPARATOR);
        if(recipients.length > 1) {
            for (String recipient : recipients) {
                Event singleRecipientEvent = event.copy();
                singleRecipientEvent.setContextField(Event.EventContextField.RECEIVER, recipient);
                events.add(singleRecipientEvent);
            }
        }
        else {
            events.add(event);
        }
        return events;
    }
}
