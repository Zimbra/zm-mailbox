/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar;

import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;

public class ZOrganizer extends CalendarUser {
    public ZOrganizer(String address, String cn) {
        super(address, cn, null, null, null);
    }

    public ZOrganizer(String address,
                      String cn,
                      String sentBy,
                      String dir,
                      String language) {
        super(address, cn, sentBy, dir, language);
    }

    public ZOrganizer(ZOrganizer other) {
        this(other.getAddress(), other.getCn(), other.getSentBy(),
             other.getDir(), other.getLanguage());
    }

    public ZOrganizer(ZProperty prop) {
        super(prop);
    }

    public ZOrganizer(Metadata meta) {
        super(meta);
    }

    protected ICalTok getPropertyName() {
        return ICalTok.ORGANIZER;
    }
}
