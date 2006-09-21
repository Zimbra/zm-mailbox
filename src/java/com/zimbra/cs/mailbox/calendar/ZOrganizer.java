/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
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
