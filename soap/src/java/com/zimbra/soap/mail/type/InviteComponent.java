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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.AlarmInfoInterface;
import com.zimbra.soap.base.CalOrganizerInterface;
import com.zimbra.soap.base.CalendarAttendeeInterface;
import com.zimbra.soap.base.DtTimeInfoInterface;
import com.zimbra.soap.base.DurationInfoInterface;
import com.zimbra.soap.base.ExceptionRecurIdInfoInterface;
import com.zimbra.soap.base.GeoInfoInterface;
import com.zimbra.soap.base.InviteComponentInterface;
import com.zimbra.soap.base.RecurrenceInfoInterface;
import com.zimbra.soap.base.XPropInterface;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "categories", "comments", "contacts", "geo",
    "attendees", "alarms", "xProps", "fragment",
    "description", "htmlDescription", "organizer",
    "recurrence", "exceptionId", "dtStart", "dtEnd", "duration" })
@GraphQLType(name=GqlConstants.CLASS_INVITE_COMPONENT, description="Invitation Component")
public class InviteComponent
extends InviteComponentCommon
implements InviteComponentInterface
{
    // {@link InviteComponent} and {@link InviteComponentWithGroupInfo} only
    // differ in the object representing E_CAL_ATTENDEE

    /**
     * @zm-api-field-tag invite-comp-category
     * @zm-api-field-description Categories - for iCalendar CATEGORY properties
     */
    @XmlElement(name=MailConstants.E_CAL_CATEGORY /* category */, required=false)
    @GraphQLQuery(name=GqlConstants.CATEGORIES, description="for iCalendar CATEGORY properties")
    private final List<String> categories = Lists.newArrayList();

    /**
     * @zm-api-field-tag invite-comp-comment
     * @zm-api-field-description Comments - for iCalendar COMMENT properties
     */
    @XmlElement(name=MailConstants.E_CAL_COMMENT /* comment */, required=false)
    @GraphQLQuery(name=GqlConstants.COMMENTS, description="for iCalendar COMMENT properties")
    private final List<String> comments = Lists.newArrayList();

    /**
     * @zm-api-field-tag invite-comp-contact
     * @zm-api-field-description Contacts - for iCalendar CONTACT properties
     */
    @XmlElement(name=MailConstants.E_CAL_CONTACT /* contact */, required=false)
    @GraphQLQuery(name=GqlConstants.CONTACTS, description="for iCalendar CONTACT properties")
    private final List<String> contacts = Lists.newArrayList();

    /**
     * @zm-api-field-description for iCalendar GEO property
     */
    @XmlElement(name=MailConstants.E_CAL_GEO /* geo */, required=false)
    private GeoInfo geo;

    /**
     * @zm-api-field-description Attendees
     */
    @XmlElement(name=MailConstants.E_CAL_ATTENDEE /* at */, required=false)
    @GraphQLQuery(name=GqlConstants.ATTENDEES, description="List of attendees")
    private final List<CalendarAttendee> attendees = Lists.newArrayList();

    /**
     * @zm-api-field-description Alarm information
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM /* alarm */, required=false)
    @GraphQLQuery(name=GqlConstants.ALARMS, description="Alarm information")
    private final List<AlarmInfo> alarms = Lists.newArrayList();

    /**
     * @zm-api-field-description iCalender XPROP properties
     */
    @XmlElement(name=MailConstants.E_CAL_XPROP /* xprop */, required=false)
    @GraphQLQuery(name=GqlConstants.XPROPS, description="for iCalendar XPROP properties")
    private final List<XProp> xProps = Lists.newArrayList();

    /**
     * @zm-api-field-tag invite-comp-fragment
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
    @GraphQLQuery(name=GqlConstants.EXCEPTION_ID, description="Recurrence id, if this is an exception")
    private ExceptionRecurIdInfo exceptionId;

    // For JSON, wrapped in array because ToXML.encodeDtStart used addElement instead of addUniqueElement :-(
    /**
     * @zm-api-field-description Start date-time (required)
     */
    @XmlElement(name=MailConstants.E_CAL_START_TIME /* s */, required=false)
    private DtTimeInfo dtStart;

    // For JSON, wrapped in array because ToXML.encodeDtEnd used addElement instead of addUniqueElement :-(
    /**
     * @zm-api-field-description End date-time
     */
    @XmlElement(name=MailConstants.E_CAL_END_TIME /* e */, required=false)
    private DtTimeInfo dtEnd;

    /**
     * @zm-api-field-description Duration
     */
    @XmlElement(name=MailConstants.E_CAL_DURATION /* dur */, required=false)
    private DurationInfo duration;

    public InviteComponent() {
    }

    public InviteComponent(String method, int componentNum, boolean rsvp) {
        super(method, componentNum, rsvp);
    }

    @Override
    public InviteComponentInterface createFromMethodComponentNumRsvp(
            String method, int componentNum, boolean rsvp) {
        return new InviteComponent(method, componentNum, rsvp);
    }

    @Override
    @GraphQLInputField(name=GqlConstants.CATEGORIES, description="for iCalendar CATEGORY properties")
    public void setCategories(Iterable <String> categories) {
        this.categories.clear();
        if (categories != null) {
            Iterables.addAll(this.categories,categories);
        }
    }

    @Override
    @GraphQLIgnore
    public void addCategory(String category) {
        this.categories.add(category);
    }

    @Override
    @GraphQLInputField(name=GqlConstants.COMMENTS, description="for iCalendar COMMENT properties")
    public void setComments(Iterable <String> comments) {
        this.comments.clear();
        if (comments != null) {
            Iterables.addAll(this.comments,comments);
        }
    }

    @Override
    @GraphQLIgnore
    public void addComment(String comment) {
        this.comments.add(comment);
    }

    @Override
    @GraphQLInputField(name=GqlConstants.CONTACTS, description="for iCalendar CONTACT properties")
    public void setContacts(Iterable <String> contacts) {
        this.contacts.clear();
        if (contacts != null) {
            Iterables.addAll(this.contacts,contacts);
        }
    }

    @Override
    @GraphQLIgnore
    public void addContact(String contact) {
        this.contacts.add(contact);
    }
    @GraphQLInputField(name=GqlConstants.GEO, description="for iCalendar GEO properties")
    public void setGeo(GeoInfo geo) { this.geo = geo; }
    @GraphQLInputField(name=GqlConstants.ATTENDEES, description="List of attendees")
    public void setAttendees(Iterable <CalendarAttendee> attendees) {
        this.attendees.clear();
        if (attendees != null) {
            Iterables.addAll(this.attendees,attendees);
        }
    }

    @GraphQLIgnore
    public void addAttendee(CalendarAttendee attendee) {
        this.attendees.add(attendee);
    }

    @GraphQLInputField(name=GqlConstants.ALARMS, description="Alarm information")
    public void setAlarms(Iterable <AlarmInfo> alarms) {
        this.alarms.clear();
        if (alarms != null) {
            Iterables.addAll(this.alarms,alarms);
        }
    }

    @GraphQLIgnore
    public void addAlarm(AlarmInfo alarm) {
        this.alarms.add(alarm);
    }

    @GraphQLInputField(name=GqlConstants.XPROPS, description="for iCalendar XPROP properties")
    public void setXProps(Iterable <XProp> xProps) {
        this.xProps.clear();
        if (xProps != null) {
            Iterables.addAll(this.xProps,xProps);
        }
    }

    @GraphQLIgnore
    public void addXProp(XProp xProp) {
        this.xProps.add(xProp);
    }

    @Override
    @GraphQLInputField(name=GqlConstants.FRAGMENT, description="First few bytes of the message (probably between 40 and 100 bytes)")
    public void setFragment(String fragment) { this.fragment = fragment; }
    @Override
    @GraphQLInputField(name=GqlConstants.DESCRIPTION, description="Plain text description")
    public void setDescription(String description) {
        this.description = description;
    }
    @Override
    @GraphQLInputField(name=GqlConstants.HTML_DESCRIPTION, description="HTML description")
    public void setHtmlDescription(String htmlDescription) {
        this.htmlDescription = htmlDescription;
    }
    @GraphQLInputField(name=GqlConstants.ORGANIZER, description="Organizer")
    public void setOrganizer(CalOrganizer organizer) {
        this.organizer = organizer;
    }
    @GraphQLInputField(name=GqlConstants.RECURRENCE, description="Recurrence information")
    public void setRecurrence(RecurrenceInfo recurrence) {
        this.recurrence = recurrence;
    }
    @GraphQLInputField(name=GqlConstants.EXCEPTION_ID, description="Recurrence id, if this is an exception")
    public void setExceptionId(ExceptionRecurIdInfo exceptionId) {
        this.exceptionId = exceptionId;
    }
    @GraphQLInputField(name=GqlConstants.START_DATE, description="Start date-time")
    public void setDtStart(@GraphQLNonNull DtTimeInfo dtStart) { this.dtStart = dtStart; }
    @GraphQLInputField(name=GqlConstants.END_DATE, description="End date-time")
    public void setDtEnd(DtTimeInfo dtEnd) { this.dtEnd = dtEnd; }
    @GraphQLInputField(name=GqlConstants.DURATION, description="Duration")
    public void setDuration(DurationInfo duration) { this.duration = duration; }

    @Override
    @GraphQLQuery(name=GqlConstants.CATEGORIES, description="for iCalendar CATEGORY properties")
    public List<String> getCategories() {
        return Collections.unmodifiableList(categories);
    }
    @Override
    @GraphQLQuery(name=GqlConstants.COMMENTS, description="for iCalendar COMMENT properties")
    public List<String> getComments() {
        return Collections.unmodifiableList(comments);
    }
    @Override
    @GraphQLQuery(name=GqlConstants.CONTACTS, description="for iCalendar CONTACT properties")
    public List<String> getContacts() {
        return Collections.unmodifiableList(contacts);
    }
    @GraphQLQuery(name=GqlConstants.GEO, description="for iCalendar GEO properties")
    public GeoInfo getGeo() { return geo; }
    @GraphQLQuery(name=GqlConstants.ATTENDEES, description="List of attendees")
    public List<CalendarAttendee> getAttendees() {
        return Collections.unmodifiableList(attendees);
    }
    @GraphQLQuery(name=GqlConstants.ALARMS, description="Alarm information")
    public List<AlarmInfo> getAlarms() {
        return Collections.unmodifiableList(alarms);
    }
    @GraphQLQuery(name=GqlConstants.XPROPS, description="for iCalendar XPROP properties")
    public List<XProp> getXProps() {
        return Collections.unmodifiableList(xProps);
    }
    @Override
    @GraphQLQuery(name=GqlConstants.FRAGMENT, description="First few bytes of the message (probably between 40 and 100 bytes)")
    public String getFragment() { return fragment; }
    @Override
    @GraphQLQuery(name=GqlConstants.DESCRIPTION, description="Present if noBlob is set, and message has a plain text description")
    public String getDescription() { return description; }
    @Override
    @GraphQLQuery(name=GqlConstants.HTML_DESCRIPTION, description="Present if noBlob is set, and message has an HTML description")
    public String getHtmlDescription() { return htmlDescription; }
    @GraphQLQuery(name=GqlConstants.ORGANIZER, description="Organizer")
    public CalOrganizer getOrganizer() { return organizer; }
    @Override
    @GraphQLIgnore
    public CalOrganizerInterface getOrganizerInterface() { return organizer; }
    @GraphQLQuery(name=GqlConstants.RECURRENCE, description="Recurrence information")
    public RecurrenceInfo getRecurrence() { return recurrence; }
    @Override
    @GraphQLIgnore
    public RecurrenceInfoInterface getRecurrenceInterface() { return recurrence; }
    @GraphQLQuery(name=GqlConstants.EXCEPTION_ID, description="Recurrence id, if this is an exception")
    public ExceptionRecurIdInfo getExceptionId() { return exceptionId; }
    @GraphQLQuery(name=GqlConstants.START_DATE, description="Start date-time")
    public DtTimeInfo getDtStart() { return dtStart; }
    @GraphQLQuery(name=GqlConstants.END_DATE, description="End date-time")
    public DtTimeInfo getDtEnd() { return dtEnd; }
    @GraphQLQuery(name=GqlConstants.DURATION, description="Duration")
    public DurationInfo getDuration() { return duration; }

    @Override
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

    @Override
    @GraphQLIgnore
    public void setGeoInterface(GeoInfoInterface geo) {
        setGeo((GeoInfo) geo);
    }

    @Override
    @GraphQLIgnore
    public void setAttendeeInterfaces(
            Iterable<CalendarAttendeeInterface> attendees) {
        setAttendees(CalendarAttendee.fromInterfaces(attendees));
    }

    @Override
    @GraphQLIgnore
    public void addAttendeeInterface(CalendarAttendeeInterface attendee) {
        addAttendee((CalendarAttendee) attendee);
    }

    @Override
    @GraphQLIgnore
    public void setAlarmInterfaces(Iterable<AlarmInfoInterface> alarms) {
        setAlarms(AlarmInfo.fromInterfaces(alarms));
    }

    @Override
    @GraphQLIgnore
    public void addAlarmInterface(AlarmInfoInterface alarm) {
        addAlarm((AlarmInfo) alarm);
    }

    @Override
    @GraphQLIgnore
    public void setXPropInterfaces(Iterable<XPropInterface> xProps) {
        setXProps(XProp.fromInterfaces(xProps));
    }

    @Override
    @GraphQLIgnore
    public void addXPropInterface(XPropInterface xProp) {
        addXProp((XProp) xProp);
    }

    @Override
    @GraphQLIgnore
    public void setOrganizerInterface(CalOrganizerInterface organizer) {
        setOrganizer((CalOrganizer) organizer);
    }

    @Override
    @GraphQLIgnore
    public void setRecurrenceInterface(RecurrenceInfoInterface recurrence) {
        setRecurrence((RecurrenceInfo) recurrence);
    }

    @Override
    @GraphQLIgnore
    public void setExceptionIdInterface(
            ExceptionRecurIdInfoInterface exceptionId) {
        setExceptionId((ExceptionRecurIdInfo) exceptionId);
    }

    @Override
    @GraphQLIgnore
    public void setDtStartInterface(DtTimeInfoInterface dtStart) {
        setDtStart((DtTimeInfo) dtStart);
    }

    @Override
    @GraphQLIgnore
    public void setDtEndInterface(DtTimeInfoInterface dtEnd) {
        setDtEnd((DtTimeInfo) dtEnd);
    }

    @Override
    @GraphQLIgnore
    public void setDurationInterface(DurationInfoInterface duration) {
        setDuration((DurationInfo) duration);
    }

    @Override
    @GraphQLIgnore
    public GeoInfoInterface getGeoInterface() {
        return this.geo;
    }

    @Override
    @GraphQLIgnore
    public List<CalendarAttendeeInterface> getAttendeeInterfaces() {
        return CalendarAttendee.toInterfaces(this.attendees);
    }

    @Override
    @GraphQLIgnore
    public List<AlarmInfoInterface> getAlarmInterfaces() {
        return AlarmInfo.toInterfaces(this.alarms);
    }

    @Override
    @GraphQLIgnore
    public List<XPropInterface> getXPropInterfaces() {
        return XProp.toInterfaces(this.xProps);
    }

    @Override
    @GraphQLIgnore
    public ExceptionRecurIdInfoInterface getExceptionIdInterface() {
        return this.exceptionId;
    }

    @Override
    @GraphQLIgnore
    public DtTimeInfoInterface getDtStartInterface() {
        return this.dtStart;
    }

    @Override
    @GraphQLIgnore
    public DtTimeInfoInterface getDtEndInterface() {
        return this.dtEnd;
    }

    @Override
    @GraphQLIgnore
    public DurationInfoInterface getDurationInterface() {
        return this.duration;
    }
}
