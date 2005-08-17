/*
 * Created on 2005. 7. 11.
 */
package com.liquidsys.coco.account;

import com.liquidsys.coco.mailbox.calendar.ICalTimeZone;

/**
 * @author jhahm
 */
public interface WellKnownTimeZone extends NamedEntry {

    public ICalTimeZone toTimeZone();

    public String getStandardDtStart();

    public String getStandardOffset();

    public String getStandardRecurrenceRule();

    public String getDaylightDtStart();

    public String getDaylightOffset();

    public String getDaylightRecurrenceRule();
}
