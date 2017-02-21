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
public interface InviteComponentInterface
extends InviteComponentCommonInterface {
    public InviteComponentInterface createFromMethodComponentNumRsvp(String method, int componentNum, boolean rsvp);
    public void setCategories(Iterable <String> categories);
    public void addCategory(String category);
    public void setComments(Iterable <String> comments);
    public void addComment(String comment);
    public void setContacts(Iterable <String> contacts);
    public void addContact(String contact);

    public void setFragment(String fragment);
    public void setDescription(String description);
    public void setHtmlDescription(String htmlDescription);
    public List<String> getCategories();
    public List<String> getComments();
    public List<String> getContacts();
    public String getFragment();
    public String getDescription();
    public String getHtmlDescription();

    public void setGeoInterface(GeoInfoInterface geo);
    public void setAttendeeInterfaces(
            Iterable<CalendarAttendeeInterface> attendees);
    public void addAttendeeInterface(CalendarAttendeeInterface attendee);
    public void setAlarmInterfaces(Iterable<AlarmInfoInterface> alarms);
    public void addAlarmInterface(AlarmInfoInterface alarm);
    public void setXPropInterfaces(Iterable<XPropInterface> xProps);
    public void addXPropInterface(XPropInterface xProp);
    public void setOrganizerInterface(CalOrganizerInterface organizer);
    public void setRecurrenceInterface(RecurrenceInfoInterface recurrence);
    public void setExceptionIdInterface(
            ExceptionRecurIdInfoInterface exceptionId);
    public void setDtStartInterface(DtTimeInfoInterface dtStart);
    public void setDtEndInterface(DtTimeInfoInterface dtEnd);
    public void setDurationInterface(DurationInfoInterface duration);
    public GeoInfoInterface getGeoInterface();
    public List<CalendarAttendeeInterface> getAttendeeInterfaces();
    public List<AlarmInfoInterface> getAlarmInterfaces();
    public List<XPropInterface> getXPropInterfaces();
    public CalOrganizerInterface getOrganizerInterface();
    public RecurrenceInfoInterface getRecurrenceInterface();
    public ExceptionRecurIdInfoInterface getExceptionIdInterface();
    public DtTimeInfoInterface getDtStartInterface();
    public DtTimeInfoInterface getDtEndInterface();
    public DurationInfoInterface getDurationInterface();
}
