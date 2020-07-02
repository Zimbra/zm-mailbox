/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "categories", "comments", "contacts", "geo",
    "attendees", "alarms", "xProps", "fragment",
    "description", "htmlDescription", "organizer",
    "recurrence", "exceptionId", "dtStart", "dtEnd", "duration" })
public class InviteComponentWithGroupInfo
extends InviteComponentCommon {

    // {@link InviteComponent} and {@link InviteComponentWithGroupInfo} only
    // differ in the object representing E_CAL_ATTENDEE

    /**
     * @zm-api-field-tag invite-comp-category
     * @zm-api-field-description Categories - for iCalendar CATEGORY properties
     */
    @XmlElement(name=MailConstants.E_CAL_CATEGORY /* category */, required=false)
    private List<String> categories = Lists.newArrayList();

    /**
     * @zm-api-field-tag invite-comp-comment
     * @zm-api-field-description Comments - for iCalendar COMMENT properties
     */
    @XmlElement(name=MailConstants.E_CAL_COMMENT /* comment */, required=false)
    private List<String> comments = Lists.newArrayList();

    /**
     * @zm-api-field-tag invite-comp-contact
     * @zm-api-field-description Contacts - for iCalendar CONTACT properties
     */
    @XmlElement(name=MailConstants.E_CAL_CONTACT /* contact */, required=false)
    private List<String> contacts = Lists.newArrayList();

    /**
     * @zm-api-field-description Information for iCalendar GEO property
     */
    @XmlElement(name=MailConstants.E_CAL_GEO /* geo */, required=false)
    private GeoInfo geo;

    /**
     * @zm-api-field-description Attendees
     */
    @XmlElement(name=MailConstants.E_CAL_ATTENDEE /* at */, required=false)
    private List<CalendarAttendeeWithGroupInfo> attendees = Lists.newArrayList();

    /**
     * @zm-api-field-description Alarm information
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM /* alarm */, required=false)
    private List<AlarmInfo> alarms = Lists.newArrayList();

    /**
     * @zm-api-field-description iCalender XPROP properties
     */
    @XmlElement(name=MailConstants.E_CAL_XPROP /* xprop */, required=false)
    private List<XProp> xProps = Lists.newArrayList();

    /**
     * @zm-api-field-tag fragment
     * @zm-api-field-description First few bytes of the message (probably between 40 and 100 bytes)
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    /**
     * @zm-api-field-tag invite-comp-desc
     * @zm-api-field-description Present if noBlob is set and invite has a plain text description
     */
    @XmlElement(name=MailConstants.E_CAL_DESCRIPTION /* desc */, required=false)
    private String description;

    /**
     * @zm-api-field-tag invite-comp-html-desc
     * @zm-api-field-description Present if noBlob is set and invite has an HTML description
     */
    @XmlElement(name=MailConstants.E_CAL_DESC_HTML /* descHtml */, required=false)
    private String htmlDescription;

    /**
     * @zm-api-field-description Organizer
     */
    @XmlElement(name=MailConstants.E_CAL_ORGANIZER /* or */, required=false)
    private CalOrganizer organizer;

    /**
     * @zm-api-field-description Recurrence information
     */
    @XmlElement(name=MailConstants.E_CAL_RECUR /* recur */, required=false)
    private RecurrenceInfo recurrence;

    /**
     * @zm-api-field-description RECURRENCE-ID, if this is an exception
     */
    @XmlElement(name=MailConstants.E_CAL_EXCEPTION_ID /* exceptId */, required=false)
    private ExceptionRecurIdInfo exceptionId;

    /**
     * @zm-api-field-description Start date-time (required)
     */
    @XmlElement(name=MailConstants.E_CAL_START_TIME /* s */, required=false)
    private DtTimeInfo dtStart;

    /**
     * @zm-api-field-description End date-time
     */
    @XmlElement(name=MailConstants.E_CAL_END_TIME /* e */, required=false)
    private DtTimeInfo dtEnd;

    /**
     * @zm-api-field-tag
     * @zm-api-field-description Duration
     */
    @XmlElement(name=MailConstants.E_CAL_DURATION /* dur */, required=false)
    private DurationInfo duration;

    /**
     * no-argument constructor wanted by JAXB
     */
    protected InviteComponentWithGroupInfo() {
        this((String) null, -1, false);
    }

    public InviteComponentWithGroupInfo(String method, int componentNum, boolean rsvp) {
        super(method, componentNum, rsvp);
    }

    public void setCategories(Iterable <String> categories) {
        this.categories.clear();
        if (categories != null) {
            Iterables.addAll(this.categories,categories);
        }
    }

    public void addCategory(String category) {
        this.categories.add(category);
    }

    public void setComments(Iterable <String> comments) {
        this.comments.clear();
        if (comments != null) {
            Iterables.addAll(this.comments,comments);
        }
    }

    public void addComment(String comment) {
        this.comments.add(comment);
    }

    public void setContacts(Iterable <String> contacts) {
        this.contacts.clear();
        if (contacts != null) {
            Iterables.addAll(this.contacts,contacts);
        }
    }

    public void addContact(String contact) {
        this.contacts.add(contact);
    }

    public void setGeo(GeoInfo geo) { this.geo = geo; }
    public void setAttendees(
                Iterable <CalendarAttendeeWithGroupInfo> attendees) {
        this.attendees.clear();
        if (attendees != null) {
            Iterables.addAll(this.attendees,attendees);
        }
    }

    public void addAttendee(CalendarAttendeeWithGroupInfo attendee) {
        this.attendees.add(attendee);
    }

    public void setAlarms(Iterable <AlarmInfo> alarms) {
        this.alarms.clear();
        if (alarms != null) {
            Iterables.addAll(this.alarms,alarms);
        }
    }

    public void addAlarm(AlarmInfo alarm) {
        this.alarms.add(alarm);
    }

    public void setXProps(Iterable <XProp> xProps) {
        this.xProps.clear();
        if (xProps != null) {
            Iterables.addAll(this.xProps,xProps);
        }
    }

    public void addXProp(XProp xProp) {
        this.xProps.add(xProp);
    }

    public void setFragment(String fragment) { this.fragment = fragment; }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setHtmlDescription(String htmlDescription) {
        this.htmlDescription = htmlDescription;
    }
    public void setOrganizer(CalOrganizer organizer) {
        this.organizer = organizer;
    }
    public void setRecurrence(RecurrenceInfo recurrence) {
        this.recurrence = recurrence;
    }
    public void setExceptionId(ExceptionRecurIdInfo exceptionId) {
        this.exceptionId = exceptionId;
    }
    public void setDtStart(DtTimeInfo dtStart) { this.dtStart = dtStart; }
    public void setDtEnd(DtTimeInfo dtEnd) { this.dtEnd = dtEnd; }
    public void setDuration(DurationInfo duration) { this.duration = duration; }
    public List<String> getCategories() {
        return Collections.unmodifiableList(categories);
    }
    public List<String> getComments() {
        return Collections.unmodifiableList(comments);
    }
    public List<String> getContacts() {
        return Collections.unmodifiableList(contacts);
    }
    public GeoInfo getGeo() { return geo; }
    public List<CalendarAttendeeWithGroupInfo> getAttendees() {
        return Collections.unmodifiableList(attendees);
    }
    public List<AlarmInfo> getAlarms() {
        return Collections.unmodifiableList(alarms);
    }
    public List<XProp> getXProps() {
        return Collections.unmodifiableList(xProps);
    }
    public String getFragment() { return fragment; }
    public String getDescription() { return description; }
    public String getHtmlDescription() { return htmlDescription; }
    public CalOrganizer getOrganizer() { return organizer; }
    public RecurrenceInfo getRecurrence() { return recurrence; }
    public ExceptionRecurIdInfo getExceptionId() { return exceptionId; }
    public DtTimeInfo getDtStart() { return dtStart; }
    public DtTimeInfo getDtEnd() { return dtEnd; }
    public DurationInfo getDuration() { return duration; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("categories", categories)
            .add("comments", comments)
            .add("contacts", contacts)
            .add("geo", geo)
            .add("attendees", attendees)
            .add("alarms", alarms)
            .add("xProps", xProps)
            .add("fragment", fragment)
            .add("description", description)
            .add("htmlDescription", htmlDescription)
            .add("organizer", organizer)
            .add("recurrence", recurrence)
            .add("exceptionId", exceptionId)
            .add("dtStart", dtStart)
            .add("dtEnd", dtEnd)
            .add("duration", duration);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
