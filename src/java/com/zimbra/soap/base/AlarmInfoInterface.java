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

package com.zimbra.soap.base;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.NONE)
public interface AlarmInfoInterface {
    public AlarmInfoInterface createFromAction(String action);
    public void setDescription(String description);
    public void setSummary(String summary);
    public String getAction();
    public String getDescription();
    public String getSummary();

    public void setTriggerInterface(AlarmTriggerInfoInterface trigger);
    public void setRepeatInterface(DurationInfoInterface repeat);
    public void setAttachInterface(CalendarAttachInterface attach);
    public void setAttendeeInterfaces(
            Iterable<CalendarAttendeeInterface> attendees);
    public void addAttendeeInterface(CalendarAttendeeInterface attendee);
    public void setXPropsInterface(Iterable<XPropInterface> xProps);
    public void addXPropInterface(XPropInterface xProp);
    public AlarmTriggerInfoInterface getTriggerInfo();
    public DurationInfoInterface getRepeatInfo();
    public CalendarAttachInterface getAttachInfo();
    public List<CalendarAttendeeInterface> getAttendeeInterfaces();
    public List<XPropInterface> getXPropInterfaces();
}
