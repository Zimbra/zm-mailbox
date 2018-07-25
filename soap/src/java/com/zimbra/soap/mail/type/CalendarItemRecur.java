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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("exceptionId", exceptionId)
            .add("dtStart", dtStart)
            .add("dtEnd", dtEnd)
            .add("duration", duration)
            .add("recurrence", recurrence);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
