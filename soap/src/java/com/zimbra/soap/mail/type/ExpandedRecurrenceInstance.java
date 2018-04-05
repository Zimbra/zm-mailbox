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
public class ExpandedRecurrenceInstance {

    /**
     * @zm-api-field-tag start-time-millis
     * @zm-api-field-description Start time in milliseconds
     */
    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=false)
    private Long startTime;

    /**
     * @zm-api-field-tag duration-millies
     * @zm-api-field-description Duration in milliseconds
     */
    @XmlAttribute(name=MailConstants.A_CAL_NEW_DURATION /* dur */, required=false)
    private Long duration;

    /**
     * @zm-api-field-tag is-all-day
     * @zm-api-field-description Set if the instance is for an all day appointment
     */
    @XmlAttribute(name=MailConstants.A_CAL_ALLDAY /* allDay */, required=false)
    private ZmBoolean allDay;

    /**
     * @zm-api-field-tag tz-offset-millis
     * @zm-api-field-description GMT offset of start time in milliseconds; returned only when allDay is set
     */
    @XmlAttribute(name=MailConstants.A_CAL_TZ_OFFSET /* tzo */, required=false)
    private Integer tzOffset;

    /**
     * @zm-api-field-tag utc-recurrence-id
     * @zm-api-field-description Recurrence ID string in UTC timezone
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID_Z /* ridZ */, required=false)
    private String recurIdZ;

    public ExpandedRecurrenceInstance() {
    }

    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public void setDuration(Long duration) { this.duration = duration; }
    public void setAllDay(Boolean allDay) { this.allDay = ZmBoolean.fromBool(allDay); }
    public void setTzOffset(Integer tzOffset) { this.tzOffset = tzOffset; }
    public void setRecurIdZ(String recurIdZ) { this.recurIdZ = recurIdZ; }
    public Long getStartTime() { return startTime; }
    public Long getDuration() { return duration; }
    public Boolean getAllDay() { return ZmBoolean.toBool(allDay); }
    public Integer getTzOffset() { return tzOffset; }
    public String getRecurIdZ() { return recurIdZ; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("startTime", startTime)
            .add("duration", duration)
            .add("allDay", allDay)
            .add("tzOffset", tzOffset)
            .add("recurIdZ", recurIdZ);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
