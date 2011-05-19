/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.caldav;

import java.util.Iterator;

import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;

public class CalDavUtils {

    // Fixup for ATTENDEE properties set by Apple iCal
    // Don't let the organizer to be also listed as an attendee.
    public static void removeAttendeeForOrganizer(ZComponent comp) {
        if (!DebugConfig.caldavAllowAttendeeForOrganizer &&
            (ICalTok.VEVENT.equals(comp.getTok()) || ICalTok.VTODO.equals(comp.getTok()))) {
            String organizer = comp.getPropVal(ICalTok.ORGANIZER, null);
            if (organizer != null) {
                organizer = organizer.trim();
                for (Iterator<ZProperty> propIter = comp.getPropertyIterator(); propIter.hasNext(); ) {
                    ZProperty prop = propIter.next();
                    if (ICalTok.ATTENDEE.equals(prop.getToken())) {
                        String att = prop.getValue();
                        if (att != null) {
                            att = att.trim();
                            if (att.equalsIgnoreCase(organizer)) {
                                // iCal has a habit of listing the organizer as an ATTENDEE.  Undo it.
                                propIter.remove();
                            }
                        } else {
                            // We haven't seen this case occur, but just in case.
                            propIter.remove();
                        }
                    }
                }
            }
        }
    }

    public static void removeAttendeeForOrganizer(ZVCalendar cal) {
        for (Iterator<ZComponent> compIter = cal.getComponentIterator(); compIter.hasNext(); ) {
            removeAttendeeForOrganizer(compIter.next());
        }
    }
}
