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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class CommonInstanceDataAttrs {

    /**
     * @zm-api-field-tag participation-status
     * @zm-api-field-description <b>Your</b> iCalendar PTST (Participation status)
     * <br />
     * Valid values: <b>NE|AC|TE|DE|DG|CO|IN|WE|DF</b>
     * <br />
     * Meanings:
     * <br />
     * "NE"eds-action, "TE"ntative, "AC"cept, "DE"clined, "DG" (delegated), "CO"mpleted (todo), "IN"-process (todo),
     * "WA"iting (custom value only for todo), "DF" (deferred; custom value only for todo)
     */
    @XmlAttribute(name=MailConstants.A_CAL_PARTSTAT /* ptst */, required=false)
    private String partStat;

    /**
     * @zm-api-field-tag utc-recurrence-id-YYYYMMDD[ThhmmssZ]
     * @zm-api-field-description RECURRENCE-ID in "Z" (UTC) timezone, if this is an exception.
     * <br />
     * Format : YYYYMMDD[ThhmmssZ]
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID_Z /* ridZ */, required=false)
    private String recurIdZ;

    /**
     * @zm-api-field-tag tz-offset
     * @zm-api-field-description Offset from GMT in milliseconds for start time in the time zone of the instance; this
     * is useful because the instance time zone may not be the same as the time zone of the requesting client; when
     * rendering an all-day appointment, the client must shift the appointment by the difference between the instance
     * time zone and its local time zone to determine the correct date to render the all-day block
     */
    @XmlAttribute(name=MailConstants.A_CAL_TZ_OFFSET /* tzo */, required=false)
    private Long tzOffset;

    /**
     * @zm-api-field-tag actual-freebusy-status
     * @zm-api-field-description Actual free-busy status: Free, Busy, busy-Tentative, busy-Unavailable
     * (a.k.a. OutOfOffice)
     * <br />
     * While free-busy status is simply a property of an event that is set during creation/update, "actual" free-busy
     * status is the true free-busy state that depends on appt/invite free-busy, event scheduling status
     * (confirmed vs. tentative vs. cancel), and more importantly, the attendee's participation status.  For example,
     * actual free-busy is busy-Tentative for an event with Busy free-busy value until the attendee has acted on the
     * invite.
     */
    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY_ACTUAL /* fba */, required=false)
    private String freeBusyActual;

    /**
     * @zm-api-field-tag task-percent-complete
     * @zm-api-field-description Percent complete - only for tasks
     */
    @XmlAttribute(name=MailConstants.A_TASK_PERCENT_COMPLETE /* percentComplete */, required=false)
    private String taskPercentComplete;

    /**
     * @zm-api-field-tag is-recurring
     * @zm-api-field-description If set, this is a recurring appointment
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECUR /* recur */, required=false)
    private ZmBoolean isRecurring;

    // The hasEx attribute should be set at <appt>/<task> level only and should not be overridden at <inst> level.
    // This is because the presence of exceptions instances is a property of the entire appointment/task.
    /**
     * @zm-api-field-tag has-exceptions
     * @zm-api-field-description If set, this is a recurring appointment with exceptions
     */
    @XmlAttribute(name=MailConstants.A_CAL_HAS_EXCEPTIONS /* hasEx */, required=false)
    private ZmBoolean hasExceptions;

    /**
     * @zm-api-field-tag priority
     * @zm-api-field-description Priority
     */
    @XmlAttribute(name=MailConstants.A_CAL_PRIORITY /* priority */, required=false)
    private String priority;

    /**
     * @zm-api-field-tag intended-freebusy
     * @zm-api-field-description Intended Free/Busy
     */
    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY /* fb */, required=false)
    private String freeBusyIntended;

    /**
     * @zm-api-field-tag transparency-O|T
     * @zm-api-field-description Transparency - <b>O|T</b>
     */
    @XmlAttribute(name=MailConstants.A_APPT_TRANSPARENCY /* transp */, required=false)
    private String transparency;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag location
     * @zm-api-field-description Location
     */
    @XmlAttribute(name=MailConstants.A_CAL_LOCATION /* loc */, required=false)
    private String location;

    /**
     * @zm-api-field-tag has-other-attendees
     * @zm-api-field-description If set, this appointment has other attendees
     */
    @XmlAttribute(name=MailConstants.A_CAL_OTHER_ATTENDEES /* otherAtt */, required=false)
    private ZmBoolean hasOtherAttendees;

    /**
     * @zm-api-field-tag has-alarm
     * @zm-api-field-description Set if has alarm
     */
    @XmlAttribute(name=MailConstants.A_CAL_ALARM /* alarm */, required=false)
    private ZmBoolean hasAlarm;

    /**
     * @zm-api-field-tag default-am-i-organizer-flag
     * @zm-api-field-description Default invite "am I organizer" flag
     */
    @XmlAttribute(name=MailConstants.A_CAL_ISORG /* isOrg */, required=false)
    private ZmBoolean isOrganizer;

    /**
     * @zm-api-field-tag default-invite-mail-item-id
     * @zm-api-field-description Default invite mail item ID
     */
    @XmlAttribute(name=MailConstants.A_CAL_INV_ID /* invId */, required=false)
    private String invId;

    /**
     * @zm-api-field-tag default-invite-component-number
     * @zm-api-field-description Default invite component number
     */
    @XmlAttribute(name=MailConstants.A_CAL_COMPONENT_NUM /* compNum */, required=false)
    private Integer componentNum;

    /**
     * @zm-api-field-tag status-TENT|CONF|CANC
     * @zm-api-field-description Status - <b>TENT|CONF|CANC</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_STATUS /* status */, required=false)
    private String status;

    /**
     * @zm-api-field-tag class
     * @zm-api-field-description Class
     */
    @XmlAttribute(name=MailConstants.A_CAL_CLASS /* class */, required=false)
    private String calClass;

    /**
     * @zm-api-field-tag all-day
     * @zm-api-field-description If set, this is an "all day" appointment
     */
    @XmlAttribute(name=MailConstants.A_CAL_ALLDAY /* allDay */, required=false)
    private ZmBoolean allDay;

    /**
     * @zm-api-field-tag is-draft
     * @zm-api-field-description Set if invite has changes that haven't been sent to attendees; for organizer only
     */
    @XmlAttribute(name=MailConstants.A_CAL_DRAFT /* draft */, required=false)
    private ZmBoolean draft;

    /**
     * @zm-api-field-tag never-sent
     * @zm-api-field-description Set if attendees have never been notified for this invite; for organizer only,
     * used in SetAppointmentRequest only
     */
    @XmlAttribute(name=MailConstants.A_CAL_NEVER_SENT /* neverSent */, required=false)
    private ZmBoolean neverSent;

    /**
     * @zm-api-field-tag task-due-date
     * @zm-api-field-description Due date in milliseconds.  For tasks only
     */
    @XmlAttribute(name=MailConstants.A_TASK_DUE_DATE /* dueDate */, required=false)
    private Long taskDueDate;

    /**
     * @zm-api-field-tag due-tz-offset
     * @zm-api-field-description Similar to the "tzo" attribute but for "dueDate".  "tzoDue" can be different from
     * "tzo" if start date and due date lie on different sides of a daylight savings transition
     */
    @XmlAttribute(name=MailConstants.A_CAL_TZ_OFFSET_DUE /* tzoDue */, required=false)
    private Integer taskTzOffsetDue;

    /**
     * @zm-api-field-tag color
     * @zm-api-field-description color numeric; range 0-127; defaults to 0 if not present; client can display only 0-9
     */
    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    private Byte color;

    /**
     * @zm-api-field-tag rgb-color
     * @zm-api-field-description RGB color in format #rrggbb where r,g and b are hex digits
     */
    @XmlAttribute(name=MailConstants.A_RGB /* rgb */, required=false)
    private String rgb;

    public CommonInstanceDataAttrs() {
    }

    public void setPartStat(String partStat) { this.partStat = partStat; }
    public void setRecurIdZ(String recurIdZ) { this.recurIdZ = recurIdZ; }
    public void setTzOffset(Long tzOffset) { this.tzOffset = tzOffset; }
    public void setFreeBusyActual(String freeBusyActual) {
        this.freeBusyActual = freeBusyActual;
    }
    public void setTaskPercentComplete(String taskPercentComplete) {
        this.taskPercentComplete = taskPercentComplete;
    }
    public void setIsRecurring(Boolean isRecurring) { this.isRecurring = ZmBoolean.fromBool(isRecurring); }
    public void setHasExceptions(Boolean hasExceptions) { this.hasExceptions = ZmBoolean.fromBool(hasExceptions); }
    public void setPriority(String priority) { this.priority = priority; }
    public void setFreeBusyIntended(String freeBusyIntended) {
        this.freeBusyIntended = freeBusyIntended;
    }
    public void setTransparency(String transparency) {
        this.transparency = transparency;
    }
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }
    public void setHasOtherAttendees(Boolean hasOtherAttendees) {
        this.hasOtherAttendees = ZmBoolean.fromBool(hasOtherAttendees);
    }
    public void setHasAlarm(Boolean hasAlarm) { this.hasAlarm = ZmBoolean.fromBool(hasAlarm); }
    public void setIsOrganizer(Boolean isOrganizer) { this.isOrganizer = ZmBoolean.fromBool(isOrganizer); }
    public void setInvId(String invId) { this.invId = invId; }
    public void setComponentNum(Integer componentNum) {
        this.componentNum = componentNum;
    }
    public void setStatus(String status) { this.status = status; }
    public void setCalClass(String calClass) { this.calClass = calClass; }
    public void setAllDay(Boolean allDay) { this.allDay = ZmBoolean.fromBool(allDay); }
    public void setDraft(Boolean draft) { this.draft = ZmBoolean.fromBool(draft); }
    public void setNeverSent(Boolean neverSent) { this.neverSent = ZmBoolean.fromBool(neverSent); }
    public void setTaskDueDate(Long taskDueDate) {
        this.taskDueDate = taskDueDate;
    }
    public void setTaskTzOffsetDue(Integer taskTzOffsetDue) {
        this.taskTzOffsetDue = taskTzOffsetDue;
    }
    public void setColor(Byte color) { this.color= color; }
    public void setRgb(String rgb) { this.rgb= rgb; }
    public String getPartStat() { return partStat; }
    public String getRecurIdZ() { return recurIdZ; }
    public Long getTzOffset() { return tzOffset; }
    public String getFreeBusyActual() { return freeBusyActual; }
    public String getTaskPercentComplete() { return taskPercentComplete; }
    public Boolean getIsRecurring() { return ZmBoolean.toBool(isRecurring); }
    public Boolean getHasExceptions() { return ZmBoolean.toBool(hasExceptions); }
    public String getPriority() { return priority; }
    public String getFreeBusyIntended() { return freeBusyIntended; }
    public String getTransparency() { return transparency; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public Boolean getHasOtherAttendees() { return ZmBoolean.toBool(hasOtherAttendees); }
    public Boolean getHasAlarm() { return ZmBoolean.toBool(hasAlarm); }
    public Boolean getIsOrganizer() { return ZmBoolean.toBool(isOrganizer); }
    public String getInvId() { return invId; }
    public Integer getComponentNum() { return componentNum; }
    public String getStatus() { return status; }
    public String getCalClass() { return calClass; }
    public Boolean getAllDay() { return ZmBoolean.toBool(allDay); }
    public Boolean getDraft() { return ZmBoolean.toBool(draft); }
    public Boolean getNeverSent() { return ZmBoolean.toBool(neverSent); }
    public Long getTaskDueDate() { return taskDueDate; }
    public Integer getTaskTzOffsetDue() { return taskTzOffsetDue; }
    public Byte getColor() { return color; }
    public String getRgb() { return rgb; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("partStat", partStat)
            .add("recurIdZ", recurIdZ)
            .add("tzOffset", tzOffset)
            .add("freeBusyActual", freeBusyActual)
            .add("taskPercentComplete", taskPercentComplete)
            .add("isRecurring", isRecurring)
            .add("hasExceptions", hasExceptions)
            .add("priority", priority)
            .add("freeBusyIntended", freeBusyIntended)
            .add("transparency", transparency)
            .add("name", name)
            .add("location", location)
            .add("hasOtherAttendees", hasOtherAttendees)
            .add("hasAlarm", hasAlarm)
            .add("isOrganizer", isOrganizer)
            .add("invId", invId)
            .add("componentNum", componentNum)
            .add("status", status)
            .add("calClass", calClass)
            .add("allDay", allDay)
            .add("draft", draft)
            .add("neverSent", neverSent)
            .add("taskDueDate", taskDueDate)
            .add("taskTzOffsetDue", taskTzOffsetDue)
            .add("color", color)
            .add("rgb", rgb);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
