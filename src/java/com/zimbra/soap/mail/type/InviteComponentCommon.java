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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class InviteComponentCommon {

    @XmlAttribute(name=MailConstants.A_CAL_METHOD /* method */, required=true)
    private final String method;

    @XmlAttribute(name=MailConstants.A_CAL_COMPONENT_NUM /* compNum */,
            required=true)
    private final int componentNum;

    @XmlAttribute(name=MailConstants.A_CAL_RSVP /* rsvp */, required=true)
    private final boolean rsvp;

    @XmlAttribute(name=MailConstants.A_CAL_PRIORITY /* priority */,
            required=false)
    private String priority;

    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    @XmlAttribute(name=MailConstants.A_CAL_LOCATION /* loc */, required=false)
    private String location;

    @XmlAttribute(name=MailConstants.A_TASK_PERCENT_COMPLETE
            /* percentComplete */, required=false)
    private String percentComplete;

    @XmlAttribute(name=MailConstants.A_TASK_COMPLETED /* completed */,
            required=false)
    private String completed;

    @XmlAttribute(name=MailConstants.A_CAL_NO_BLOB /* noBlob */, required=false)
    private Boolean noBlob;

    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY_ACTUAL /* fba */,
            required=false)
    private String freeBusyActual;

    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY /* fb */, required=false)
    private String freeBusy;

    @XmlAttribute(name=MailConstants.A_APPT_TRANSPARENCY /* transp */,
            required=false)
    private String transparency;

    @XmlAttribute(name=MailConstants.A_CAL_ISORG /* isOrg */, required=false)
    private Boolean isOrganizer;

    @XmlAttribute(name="x_uid", required=false)
    private String xUid;

    @XmlAttribute(name=MailConstants.A_UID /* uid */, required=false)
    private String uid;

    @XmlAttribute(name=MailConstants.A_CAL_SEQUENCE /* seq */, required=false)
    private Integer sequence;

    // For zdsync
    @XmlAttribute(name=MailConstants.A_CAL_DATETIME /* d */, required=false)
    private Long dateTime;

    @XmlAttribute(name=MailConstants.A_CAL_ID /* calItemId */, required=false)
    private String calItemId;

    // For backwards compat
    @XmlAttribute(name=MailConstants.A_APPT_ID_DEPRECATE_ME /* apptId */,
            required=false)
    private String deprecatedApptId;

    @XmlAttribute(name=MailConstants.A_CAL_ITEM_FOLDER /* ciFolder */,
            required=false)
    private String calItemFolder;

    @XmlAttribute(name=MailConstants.A_CAL_STATUS /* status */, required=false)
    private String status;

    @XmlAttribute(name=MailConstants.A_CAL_CLASS /* class */, required=false)
    private String calClass;

    @XmlAttribute(name=MailConstants.A_CAL_URL /* url */, required=false)
    private String url;

    @XmlAttribute(name=MailConstants.A_CAL_IS_EXCEPTION /* ex */, required=false)
    private Boolean isException;

    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID_Z /* ridZ */,
            required=false)
    private String recurIdZ;

    @XmlAttribute(name=MailConstants.A_CAL_ALLDAY /* allDay */, required=false)
    private Boolean isAllDay;

    @XmlAttribute(name=MailConstants.A_CAL_DRAFT /* draft */, required=false)
    private Boolean isDraft;

    @XmlAttribute(name=MailConstants.A_CAL_NEVER_SENT /* neverSent */,
            required=false)
    private Boolean neverSent;

    @XmlAttribute(name=MailConstants.A_CAL_CHANGES /* changes */,
            required=false)
    private String changes;

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
        this.rsvp = rsvp;
    }

    public void setPriority(String priority) { this.priority = priority; }
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
    public String getMethod() { return method; }
    public int getComponentNum() { return componentNum; }
    public boolean getRsvp() { return rsvp; }
    public String getPriority() { return priority; }
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
            .add("changes", changes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
