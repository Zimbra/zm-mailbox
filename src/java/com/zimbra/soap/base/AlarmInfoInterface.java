/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
