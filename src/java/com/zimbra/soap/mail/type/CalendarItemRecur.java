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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CalendarItemRecur {

    /**
     * @zm-api-field-tag recurrence-id
     * @zm-api-field-description Information for iCalendar RECURRENCE-ID
     */
    @XmlElement(name=MailConstants.E_CAL_EXCEPTION_ID /* exceptId */, required=false)
    private ExceptionRecurIdInfo exceptionId;

    /**
     * @zm-api-field-description Start time
     */
    @XmlElement(name=MailConstants.E_CAL_START_TIME /* s */, required=false)
    private DtTimeInfo dtStart;

    /**
     * @zm-api-field-description End time
     */
    @XmlElement(name=MailConstants.E_CAL_END_TIME /* e */, required=false)
    private DtTimeInfo dtEnd;

    /**
     * @zm-api-field-description Duration information
     */
    @XmlElement(name=MailConstants.E_CAL_DURATION /* dur */, required=false)
    private DurationInfo duration;

    /**
     * @zm-api-field-description Recurrence information
     */
    @XmlElement(name=MailConstants.E_CAL_RECUR /* recur */, required=false)
    private RecurrenceInfo recurrence;

    public CalendarItemRecur() {
    }

    public void setExceptionId(ExceptionRecurIdInfo exceptionId) {
        this.exceptionId = exceptionId;
    }
    public void setDtStart(DtTimeInfo dtStart) { this.dtStart = dtStart; }
    public void setDtEnd(DtTimeInfo dtEnd) { this.dtEnd = dtEnd; }
    public void setDuration(DurationInfo duration) { this.duration = duration; }
    public void setRecurrence(RecurrenceInfo recurrence) {
        this.recurrence = recurrence;
    }
    public ExceptionRecurIdInfo getExceptionId() { return exceptionId; }
    public DtTimeInfo getDtStart() { return dtStart; }
    public DtTimeInfo getDtEnd() { return dtEnd; }
    public DurationInfo getDuration() { return duration; }
    public RecurrenceInfo getRecurrence() { return recurrence; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("exceptionId", exceptionId)
            .add("dtStart", dtStart)
            .add("dtEnd", dtEnd)
            .add("duration", duration)
            .add("recurrence", recurrence);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
