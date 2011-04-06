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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "categories", "comments", "contacts", "geo",
    "attendees", "alarms", "xProps", "fragment",
    "description", "htmlDescription", "organizer",
    "recurrence", "exceptionId", "dtStart", "dtEnd", "duration" })
public class InviteComponent {

    @XmlAttribute(name=MailConstants.A_CAL_METHOD, required=true)
    private final String method;

    @XmlAttribute(name=MailConstants.A_CAL_COMPONENT_NUM, required=true)
    private final int componentNum;

    @XmlAttribute(name=MailConstants.A_CAL_RSVP, required=true)
    private final boolean rsvp;

    @XmlAttribute(name=MailConstants.A_CAL_PRIORITY, required=false)
    private boolean priority;

    @XmlAttribute(name=MailConstants.A_NAME, required=false)
    private String name;

    @XmlAttribute(name=MailConstants.A_CAL_LOCATION, required=false)
    private String location;

    @XmlAttribute(name=MailConstants.A_TASK_PERCENT_COMPLETE, required=false)
    private String percentComplete;

    @XmlAttribute(name=MailConstants.A_TASK_COMPLETED, required=false)
    private String completed;

    @XmlAttribute(name=MailConstants.A_CAL_NO_BLOB, required=false)
    private Boolean noBlob;

    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY_ACTUAL, required=false)
    private String freeBusyActual;

    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY, required=false)
    private String freeBusy;

    @XmlAttribute(name=MailConstants.A_APPT_TRANSPARENCY, required=false)
    private String transparency;

    @XmlAttribute(name=MailConstants.A_CAL_ISORG, required=false)
    private Boolean isOrganizer;

    @XmlAttribute(name="x_uid", required=false)
    private String xUid;

    @XmlAttribute(name=MailConstants.A_UID, required=false)
    private String uid;

    @XmlAttribute(name=MailConstants.A_CAL_SEQUENCE, required=false)
    private Integer sequence;

    // For zdsync
    @XmlAttribute(name=MailConstants.A_CAL_DATETIME, required=false)
    private Long dateTime;

    @XmlAttribute(name=MailConstants.A_CAL_ID, required=false)
    private String calItemId;

    // For backwards compat
    @XmlAttribute(name=MailConstants.A_APPT_ID_DEPRECATE_ME, required=false)
    private String deprecatedApptId;

    @XmlAttribute(name=MailConstants.A_CAL_ITEM_FOLDER, required=false)
    private String calItemFolder;

    @XmlAttribute(name=MailConstants.A_CAL_STATUS, required=false)
    private String status;

    @XmlAttribute(name=MailConstants.A_CAL_CLASS, required=false)
    private String calClass;

    @XmlAttribute(name=MailConstants.A_CAL_URL, required=false)
    private String url;

    @XmlAttribute(name=MailConstants.A_CAL_IS_EXCEPTION, required=false)
    private Boolean isException;

    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID_Z, required=false)
    private String recurIdZ;

    @XmlAttribute(name=MailConstants.A_CAL_ALLDAY, required=false)
    private Boolean isAllDay;

    @XmlAttribute(name=MailConstants.A_CAL_DRAFT, required=false)
    private Boolean isDraft;

    @XmlAttribute(name=MailConstants.A_CAL_NEVER_SENT, required=false)
    private Boolean neverSent;

    @XmlAttribute(name=MailConstants.A_CAL_CHANGES, required=false)
    private String changes;

    @XmlElement(name=MailConstants.E_CAL_CATEGORY, required=false)
    private List<String> categories = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_COMMENT, required=false)
    private List<String> comments = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_CONTACT, required=false)
    private List<String> contacts = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_GEO, required=false)
    private GeoInfo geo;

    @XmlElement(name=MailConstants.E_CAL_ATTENDEE, required=false)
    private List<CalendarAttendee> attendees = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_ALARM, required=false)
    private List<AlarmInfo> alarms = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_XPROP, required=false)
    private List<XProp> xProps = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_FRAG, required=false)
    private String fragment;

    @XmlElement(name=MailConstants.E_CAL_DESCRIPTION, required=false)
    private String description;

    @XmlElement(name=MailConstants.E_CAL_DESC_HTML, required=false)
    private String htmlDescription;

    @XmlElement(name=MailConstants.E_CAL_ORGANIZER, required=false)
    private CalOrganizer organizer;

    @XmlElement(name=MailConstants.E_CAL_RECUR, required=false)
    private RecurrenceInfo recurrence;

    @XmlElement(name=MailConstants.E_CAL_EXCEPTION_ID, required=false)
    private ExceptionRecurIdInfo exceptionId;

    @XmlElement(name=MailConstants.E_CAL_START_TIME, required=false)
    private DtTimeInfo dtStart;

    @XmlElement(name=MailConstants.E_CAL_END_TIME, required=false)
    private DtTimeInfo dtEnd;

    @XmlElement(name=MailConstants.E_CAL_DURATION, required=false)
    private DurationInfo duration;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private InviteComponent() {
        this((String) null, -1, false);
    }

    public InviteComponent(String method, int componentNum, boolean rsvp) {
        this.method = method;
        this.componentNum = componentNum;
        this.rsvp = rsvp;
    }

    public void setPriority(boolean priority) { this.priority = priority; }
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }
    public void setPercentComplete(String percentComplete) {
        this.percentComplete = percentComplete;
    }
    public void setCompleted(String completed) { this.completed = completed; }
    public void setNoBlob(Boolean noBlob) { this.noBlob = noBlob; }
    public void setFreeBusyActual(String freeBusyActual) {
        this.freeBusyActual = freeBusyActual;
    }
    public void setFreeBusy(String freeBusy) { this.freeBusy = freeBusy; }
    public void setTransparency(String transparency) {
        this.transparency = transparency;
    }
    public void setIsOrganizer(Boolean isOrganizer) {
        this.isOrganizer = isOrganizer;
    }
    public void setXUid(String xUid) { this.xUid = xUid; }
    public void setUid(String uid) { this.uid = uid; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    public void setDateTime(Long dateTime) { this.dateTime = dateTime; }
    public void setCalItemId(String calItemId) { this.calItemId = calItemId; }
    public void setDeprecatedApptId(String deprecatedApptId) {
        this.deprecatedApptId = deprecatedApptId;
    }
    public void setCalItemFolder(String calItemFolder) {
        this.calItemFolder = calItemFolder;
    }
    public void setStatus(String status) { this.status = status; }
    public void setCalClass(String calClass) { this.calClass = calClass; }
    public void setUrl(String url) { this.url = url; }
    public void setIsException(Boolean isException) {
        this.isException = isException;
    }
    public void setRecurIdZ(String recurIdZ) { this.recurIdZ = recurIdZ; }
    public void setIsAllDay(Boolean isAllDay) { this.isAllDay = isAllDay; }
    public void setIsDraft(Boolean isDraft) { this.isDraft = isDraft; }
    public void setNeverSent(Boolean neverSent) { this.neverSent = neverSent; }
    public void setChanges(String changes) { this.changes = changes; }
    public void setCategories(Iterable <String> categories) {
        this.categories.clear();
        if (categories != null) {
            Iterables.addAll(this.categories,categories);
        }
    }

    public InviteComponent addCategory(String category) {
        this.categories.add(category);
        return this;
    }

    public void setComments(Iterable <String> comments) {
        this.comments.clear();
        if (comments != null) {
            Iterables.addAll(this.comments,comments);
        }
    }

    public InviteComponent addComment(String comment) {
        this.comments.add(comment);
        return this;
    }

    public void setContacts(Iterable <String> contacts) {
        this.contacts.clear();
        if (contacts != null) {
            Iterables.addAll(this.contacts,contacts);
        }
    }

    public InviteComponent addContact(String contact) {
        this.contacts.add(contact);
        return this;
    }

    public void setGeo(GeoInfo geo) { this.geo = geo; }
    public void setAttendees(Iterable <CalendarAttendee> attendees) {
        this.attendees.clear();
        if (attendees != null) {
            Iterables.addAll(this.attendees,attendees);
        }
    }

    public InviteComponent addAttende(CalendarAttendee attende) {
        this.attendees.add(attende);
        return this;
    }

    public void setAlarms(Iterable <AlarmInfo> alarms) {
        this.alarms.clear();
        if (alarms != null) {
            Iterables.addAll(this.alarms,alarms);
        }
    }

    public InviteComponent addAlarm(AlarmInfo alarm) {
        this.alarms.add(alarm);
        return this;
    }

    public void setXProps(Iterable <XProp> xProps) {
        this.xProps.clear();
        if (xProps != null) {
            Iterables.addAll(this.xProps,xProps);
        }
    }

    public InviteComponent addXProp(XProp xProp) {
        this.xProps.add(xProp);
        return this;
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
    public String getMethod() { return method; }
    public int getComponentNum() { return componentNum; }
    public boolean getRsvp() { return rsvp; }
    public boolean getPriority() { return priority; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getPercentComplete() { return percentComplete; }
    public String getCompleted() { return completed; }
    public Boolean getNoBlob() { return noBlob; }
    public String getFreeBusyActual() { return freeBusyActual; }
    public String getFreeBusy() { return freeBusy; }
    public String getTransparency() { return transparency; }
    public Boolean getIsOrganizer() { return isOrganizer; }
    public String getXUid() { return xUid; }
    public String getUid() { return uid; }
    public Integer getSequence() { return sequence; }
    public Long getDateTime() { return dateTime; }
    public String getCalItemId() { return calItemId; }
    public String getDeprecatedApptId() { return deprecatedApptId; }
    public String getCalItemFolder() { return calItemFolder; }
    public String getStatus() { return status; }
    public String getCalClass() { return calClass; }
    public String getUrl() { return url; }
    public Boolean getIsException() { return isException; }
    public String getRecurIdZ() { return recurIdZ; }
    public Boolean getIsAllDay() { return isAllDay; }
    public Boolean getIsDraft() { return isDraft; }
    public Boolean getNeverSent() { return neverSent; }
    public String getChanges() { return changes; }
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
    public List<CalendarAttendee> getAttendees() {
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

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("method", method)
            .add("componentNum", componentNum)
            .add("rsvp", rsvp)
            .add("priority", priority)
            .add("name", name)
            .add("location", location)
            .add("percentComplete", percentComplete)
            .add("completed", completed)
            .add("noBlob", noBlob)
            .add("freeBusyActual", freeBusyActual)
            .add("freeBusy", freeBusy)
            .add("transparency", transparency)
            .add("isOrganizer", isOrganizer)
            .add("xUid", xUid)
            .add("uid", uid)
            .add("sequence", sequence)
            .add("dateTime", dateTime)
            .add("calItemId", calItemId)
            .add("deprecatedApptId", deprecatedApptId)
            .add("calItemFolder", calItemFolder)
            .add("status", status)
            .add("calClass", calClass)
            .add("url", url)
            .add("isException", isException)
            .add("recurIdZ", recurIdZ)
            .add("isAllDay", isAllDay)
            .add("isDraft", isDraft)
            .add("neverSent", neverSent)
            .add("changes", changes)
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
            .add("duration", duration)
            .toString();
    }
}
