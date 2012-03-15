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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ExpandedRecurrenceComponent {

    /**
     * @zm-api-field-description RECURRENCE_ID
     */
    @XmlElement(name=MailConstants.E_CAL_EXCEPTION_ID /* exceptId */, required=false)
    private InstanceRecurIdInfo exceptionId;

    /**
     * @zm-api-field-tag dtstart-millis
     * @zm-api-field-description DTSTART time in milliseconds since the Epoch
     */
    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=false)
    private Long startTime;

    /**
     * @zm-api-field-tag dtend-millis
     * @zm-api-field-description DTEND time in milliseconds since the Epoch
     */
    @XmlAttribute(name=MailConstants.A_CAL_END_TIME /* e */, required=false)
    private Long endTime;

    /**
     * @zm-api-field-description DURATION
     */
    @XmlElement(name=MailConstants.E_CAL_DURATION /* dur */, required=false)
    private DurationInfo duration;

    /**
     * @zm-api-field-description RRULE/RDATE/EXDATE information
     */
    @XmlElement(name=MailConstants.E_CAL_RECUR /* recur */, required=false)
    private RecurrenceInfo recurrence;

    public ExpandedRecurrenceComponent() {
    }

    public void setExceptionId(InstanceRecurIdInfo exceptionId) {
        this.exceptionId = exceptionId;
    }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public void setDuration(DurationInfo duration) { this.duration = duration; }
    public void setRecurrence(RecurrenceInfo recurrence) {
        this.recurrence = recurrence;
    }
    public InstanceRecurIdInfo getExceptionId() { return exceptionId; }
    public Long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }
    public DurationInfo getDuration() { return duration; }
    public RecurrenceInfo getRecurrence() { return recurrence; }

    public Objects.ToStringHelper addToStringInfo( Objects.ToStringHelper helper) {
        return helper
            .add("exceptionId", exceptionId)
            .add("startTime", startTime)
            .add("endTime", endTime)
            .add("duration", duration)
            .add("recurrence", recurrence);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
