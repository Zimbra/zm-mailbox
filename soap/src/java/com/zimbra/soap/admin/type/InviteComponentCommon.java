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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.InviteComponentCommonInterface;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class InviteComponentCommon
implements InviteComponentCommonInterface {

    /**
     * @zm-api-field-tag invite-comp-method
     * @zm-api-field-description Method
     */
    @XmlAttribute(name=MailConstants.A_CAL_METHOD /* method */, required=true)
    private final String method;

    /**
     * @zm-api-field-tag invite-comp-num
     * @zm-api-field-description Component number of the invite
     */
    @XmlAttribute(name=MailConstants.A_CAL_COMPONENT_NUM /* compNum */, required=true)
    private final int componentNum;

    /**
     * @zm-api-field-tag rsvp
     * @zm-api-field-description RSVP flag.  Set if response requested, unset if no response requested
     */
    @XmlAttribute(name=MailConstants.A_CAL_RSVP /* rsvp */, required=true)
    private final ZmBoolean rsvp;

    /**
     * @zm-api-field-tag invite-comp-priority-0-9
     * @zm-api-field-description Priority (0 - 9; default = 0)
     */
    @XmlAttribute(name=MailConstants.A_CAL_PRIORITY /* priority */, required=false)
    private String priority;

    /**
     * @zm-api-field-tag invite-comp-name
     * @zm-api-field-description NAME
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag invite-comp-location
     * @zm-api-field-description Location
     */
    @XmlAttribute(name=MailConstants.A_CAL_LOCATION /* loc */, required=false)
    private String location;

    /**
     * @zm-api-field-tag task-percent-complete
     * @zm-api-field-description Percent complete for VTODO (0 - 100; default = 0)
     */
    @XmlAttribute(name=MailConstants.A_TASK_PERCENT_COMPLETE /* percentComplete */, required=false)
    private String percentComplete;

    /**
     * @zm-api-field-tag task-completed-yyyyMMddThhmmssZ
     * @zm-api-field-description VTODO COMPLETED DATE-TIME in format <b>yyyyMMddThhmmssZ</b>
     */
    @XmlAttribute(name=MailConstants.A_TASK_COMPLETED /* completed */, required=false)
    private String completed;

    /**
     * @zm-api-field-tag no-blob-data
     * @zm-api-field-description Set if invite has no blob data, i.e. all data is in db metadata
     */
    @XmlAttribute(name=MailConstants.A_CAL_NO_BLOB /* noBlob */, required=false)
    private ZmBoolean noBlob;

    /**
     * @zm-api-field-tag freebusy-actual
     * @zm-api-field-description The "actual" free-busy status of this invite (ie what the client should display).
     * This is synthesized taking into account our Attendee's PartStat, the Opacity of the appointment, its Status,
     * etc...
     * <br />
     * Valid values - <b>F|B|T|U</b>.  i.e. Free, Busy (default), busy-Tentative, OutOfOffice (busy-unavailable)
     */
    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY_ACTUAL /* fba */, required=false)
    private String freeBusyActual;

    /**
     * @zm-api-field-tag freebusy-status
     * @zm-api-field-description FreeBusy setting <b>F|B|T|U</b>
     * <br />
     * i.e. Free, Busy (default), busy-Tentative, OutOfOffice (busy-unavailable)
     */
    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY /* fb */, required=false)
    private String freeBusy;

    /**
     * @zm-api-field-tag transparency
     * @zm-api-field-description Transparency - <b>O|T</b>.  i.e. Opaque or Transparent
     */
    @XmlAttribute(name=MailConstants.A_APPT_TRANSPARENCY /* transp */, required=false)
    private String transparency;

    /**
     * @zm-api-field-tag is-organizer
     * @zm-api-field-description Am I the organizer?  [default <b>0 (false)</b>]
     */
    @XmlAttribute(name=MailConstants.A_CAL_ISORG /* isOrg */, required=false)
    private ZmBoolean isOrganizer;

    /**
     * @zm-api-field-tag x-uid
     * @zm-api-field-description x_uid
     */
    @XmlAttribute(name="x_uid", required=false)
    private String xUid;

    /**
     * @zm-api-field-tag uid-for-create
     * @zm-api-field-description UID to use when creating appointment.  Optional: client can request the UID to use
     */
    @XmlAttribute(name=MailConstants.A_UID /* uid */, required=false)
    private String uid;

    /**
     * @zm-api-field-tag sequence-num
     * @zm-api-field-description Sequence number (default = 0)
     */
    @XmlAttribute(name=MailConstants.A_CAL_SEQUENCE /* seq */, required=false)
    private Integer sequence;

    // For zdsync
    /**
     * @zm-api-field-tag invite-comp-date
     * @zm-api-field-description Date - used for zdsync
     */
    @XmlAttribute(name=MailConstants.A_CAL_DATETIME /* d */, required=false)
    private Long dateTime;

    /**
     * @zm-api-field-tag mail-item-id-of-appointment
     * @zm-api-field-description Mail item ID of appointment
     */
    @XmlAttribute(name=MailConstants.A_CAL_ID /* calItemId */, required=false)
    private String calItemId;

    // For backwards compat
    /**
     * @zm-api-field-tag deprecated-appt-id
     * @zm-api-field-description Appointment ID (deprecated)
     */
    @XmlAttribute(name=MailConstants.A_APPT_ID_DEPRECATE_ME /* apptId */, required=false)
    private String deprecatedApptId;

    /**
     * @zm-api-field-tag cal-item-folder
     * @zm-api-field-description Folder of appointment
     */
    @XmlAttribute(name=MailConstants.A_CAL_ITEM_FOLDER /* ciFolder */, required=false)
    private String calItemFolder;

    /**
     * @zm-api-field-tag invite-comp-status
     * @zm-api-field-description Status - <b>TENT|CONF|CANC|NEED|COMP|INPR|WAITING|DEFERRED</b>
     * <br />
     * i.e. TENTative, CONFirmed, CANCelled, COMPleted, INPRogress, WAITING, DEFERRED
     * <br />
     * where waiting and Deferred are custom values not found in the iCalendar spec.
     */
    @XmlAttribute(name=MailConstants.A_CAL_STATUS /* status */, required=false)
    private String status;

    /**
     * @zm-api-field-tag invite-comp-class
     * @zm-api-field-description Class = <b>PUB|PRI|CON</b>.  i.e. PUBlic (default), PRIvate, CONfidential
     */
    @XmlAttribute(name=MailConstants.A_CAL_CLASS /* class */, required=false)
    private String calClass;

    /**
     * @zm-api-field-tag invite-comp-url
     * @zm-api-field-description URL
     */
    @XmlAttribute(name=MailConstants.A_CAL_URL /* url */, required=false)
    private String url;

    /**
     * @zm-api-field-tag is-exception
     * @zm-api-field-description Set if this is invite is an exception
     */
    @XmlAttribute(name=MailConstants.A_CAL_IS_EXCEPTION /* ex */, required=false)
    private ZmBoolean isException;

    /**
     * @zm-api-field-tag utc-recurrence-id
     * @zm-api-field-description Recurrence-id string in UTC timezone
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID_Z /* ridZ */, required=false)
    private String recurIdZ;

    /**
     * @zm-api-field-tag is-all-day
     * @zm-api-field-description Set if is an all day appointment
     */
    @XmlAttribute(name=MailConstants.A_CAL_ALLDAY /* allDay */, required=false)
    private ZmBoolean isAllDay;

    /**
     * @zm-api-field-tag is-draft
     * @zm-api-field-description Set if invite has changes that haven't been sent to attendees; for organizer only
     */
    @XmlAttribute(name=MailConstants.A_CAL_DRAFT /* draft */, required=false)
    private ZmBoolean isDraft;

    /**
     * @zm-api-field-tag attendees-never-notified
     * @zm-api-field-description Set if attendees were never notified of this invite; for organizer only
     */
    @XmlAttribute(name=MailConstants.A_CAL_NEVER_SENT /* neverSent */, required=false)
    private ZmBoolean neverSent;

    /**
     * @zm-api-field-tag comma-sep-changed-data
     * @zm-api-field-description Comma-separated list of changed data in an updated invite.
     * <br />
     * Possible values are "subject", "location", "time" (start time, end time, or duration), and "recurrence".
     */
    @XmlAttribute(name=MailConstants.A_CAL_CHANGES /* changes */, required=false)
    private String changes;

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

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private InviteComponentCommon() {
        this((String) null, -1, false);
    }

    public InviteComponentCommon(String method, int componentNum,
                    boolean rsvp) {
        this.method = method;
        this.componentNum = componentNum;
        this.rsvp = ZmBoolean.fromBool(rsvp);
    }

    @Override
    public InviteComponentCommonInterface create(String method,
            int componentNum, boolean rsvp) {
        return new InviteComponentCommon(method, componentNum, rsvp);
    }

    @Override
    public void setPriority(String priority) { this.priority = priority; }
    @Override
    public void setName(String name) { this.name = name; }
    @Override
    public void setLocation(String location) { this.location = location; }
    @Override
    public void setPercentComplete(String percentComplete) {
        this.percentComplete = percentComplete;
    }
    @Override
    public void setCompleted(String completed) { this.completed = completed; }
    @Override
    public void setNoBlob(Boolean noBlob) { this.noBlob = ZmBoolean.fromBool(noBlob); }
    @Override
    public void setFreeBusyActual(String freeBusyActual) {
        this.freeBusyActual = freeBusyActual;
    }
    @Override
    public void setFreeBusy(String freeBusy) { this.freeBusy = freeBusy; }
    @Override
    public void setTransparency(String transparency) {
        this.transparency = transparency;
    }
    @Override
    public void setIsOrganizer(Boolean isOrganizer) { this.isOrganizer = ZmBoolean.fromBool(isOrganizer); }
    @Override
    public void setXUid(String xUid) { this.xUid = xUid; }
    @Override
    public void setUid(String uid) { this.uid = uid; }
    @Override
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    @Override
    public void setDateTime(Long dateTime) { this.dateTime = dateTime; }
    @Override
    public void setCalItemId(String calItemId) { this.calItemId = calItemId; }
    @Override
    public void setDeprecatedApptId(String deprecatedApptId) {
        this.deprecatedApptId = deprecatedApptId;
    }
    @Override
    public void setCalItemFolder(String calItemFolder) {
        this.calItemFolder = calItemFolder;
    }
    @Override
    public void setStatus(String status) { this.status = status; }
    @Override
    public void setCalClass(String calClass) { this.calClass = calClass; }
    @Override
    public void setUrl(String url) { this.url = url; }
    @Override
    public void setIsException(Boolean isException) { this.isException = ZmBoolean.fromBool(isException); }
    @Override
    public void setRecurIdZ(String recurIdZ) { this.recurIdZ = recurIdZ; }
    @Override
    public void setIsAllDay(Boolean isAllDay) { this.isAllDay = ZmBoolean.fromBool(isAllDay); }
    @Override
    public void setIsDraft(Boolean isDraft) { this.isDraft = ZmBoolean.fromBool(isDraft); }
    @Override
    public void setNeverSent(Boolean neverSent) { this.neverSent = ZmBoolean.fromBool(neverSent); }
    @Override
    public void setChanges(String changes) { this.changes = changes; }
    @Override
    public void setColor(Byte color) { this.color = color; }
    @Override
    public void setRgb(String rgb) { this.rgb = rgb; }
    @Override
    public String getMethod() { return method; }
    @Override
    public int getComponentNum() { return componentNum; }
    @Override
    public boolean getRsvp() { return ZmBoolean.toBool(rsvp); }
    @Override
    public String getPriority() { return priority; }
    @Override
    public String getName() { return name; }
    @Override
    public String getLocation() { return location; }
    @Override
    public String getPercentComplete() { return percentComplete; }
    @Override
    public String getCompleted() { return completed; }
    @Override
    public Boolean getNoBlob() { return ZmBoolean.toBool(noBlob); }
    @Override
    public String getFreeBusyActual() { return freeBusyActual; }
    @Override
    public String getFreeBusy() { return freeBusy; }
    @Override
    public String getTransparency() { return transparency; }
    @Override
    public Boolean getIsOrganizer() { return ZmBoolean.toBool(isOrganizer); }
    @Override
    public String getXUid() { return xUid; }
    @Override
    public String getUid() { return uid; }
    @Override
    public Integer getSequence() { return sequence; }
    @Override
    public Long getDateTime() { return dateTime; }
    @Override
    public String getCalItemId() { return calItemId; }
    @Override
    public String getDeprecatedApptId() { return deprecatedApptId; }
    @Override
    public String getCalItemFolder() { return calItemFolder; }
    @Override
    public String getStatus() { return status; }
    @Override
    public String getCalClass() { return calClass; }
    @Override
    public String getUrl() { return url; }
    @Override
    public Boolean getIsException() { return ZmBoolean.toBool(isException); }
    @Override
    public String getRecurIdZ() { return recurIdZ; }
    @Override
    public Boolean getIsAllDay() { return ZmBoolean.toBool(isAllDay); }
    @Override
    public Boolean getIsDraft() { return ZmBoolean.toBool(isDraft); }
    @Override
    public Boolean getNeverSent() { return ZmBoolean.toBool(neverSent); }
    @Override
    public String getChanges() { return changes; }
    @Override
    public Byte getColor() { return color; }
    @Override
    public String getRgb() { return rgb; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
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
            .add("color", color)
            .add("rgb", rgb);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
