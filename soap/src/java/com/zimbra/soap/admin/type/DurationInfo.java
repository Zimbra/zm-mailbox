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

import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.DurationInfoInterface;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class DurationInfo implements DurationInfoInterface {

    /**
     * @zm-api-field-tag duration-negative
     * @zm-api-field-description Set if the duration is negative.
     */
    @XmlAttribute(name=MailConstants.A_CAL_DURATION_NEGATIVE /* neg */, required=false)
    private ZmBoolean durationNegative;

    /**
     * @zm-api-field-tag duration-weeks
     * @zm-api-field-description Weeks component of the duration
     * <br />
     * <b>Special note: if WEEKS are specified, NO OTHER OFFSET MAY BE SPECIFIED (weeks must be alone, per RFC2445)</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_DURATION_WEEKS /* w */, required=false)
    private Integer weeks;

    /**
     * @zm-api-field-tag duration-days
     * @zm-api-field-description Days component of the duration
     */
    @XmlAttribute(name=MailConstants.A_CAL_DURATION_DAYS /* d */, required=false)
    private Integer days;

    /**
     * @zm-api-field-tag duration-hours
     * @zm-api-field-description Hours component of the duration
     */
    @XmlAttribute(name=MailConstants.A_CAL_DURATION_HOURS /* h */, required=false)
    private Integer hours;

    /**
     * @zm-api-field-tag duration-minutes
     * @zm-api-field-description Minutes component of the duration
     */
    @XmlAttribute(name=MailConstants.A_CAL_DURATION_MINUTES /* m */, required=false)
    private Integer minutes;

    /**
     * @zm-api-field-tag duration-seconds
     * @zm-api-field-description Seconds component of the duration
     */
    @XmlAttribute(name=MailConstants.A_CAL_DURATION_SECONDS /* s */, required=false)
    private Integer seconds;

    // added by Alarm.toXml after mTriggerRelative.toXml
    /**
     * @zm-api-field-tag alarm-related
     * @zm-api-field-description Specifies whether the alarm is related to the start of end.
     * <br />
     * Valid values are : <b>START|END</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_ALARM_RELATED /* related */, required=false)
    private String related;

    // added by Alarm.toXml after mRepeatDuration.toXml
    /**
     * @zm-api-field-tag alarm-repeat-count
     * @zm-api-field-description Alarm repeat count
     */
    @XmlAttribute(name=MailConstants.A_CAL_ALARM_COUNT /* count */, required=false)
    private Integer repeatCount;

    public DurationInfo() {
    }

    public DurationInfo(ParsedDuration parsedDuration) {
        this.weeks = adjustDuration(parsedDuration.getWeeks());
        if (this.weeks == null) {
            this.days = adjustDuration(parsedDuration.getDays());
            this.hours = adjustDuration(parsedDuration.getHours());
            this.minutes = adjustDuration(parsedDuration.getMins());
            this.seconds = adjustDuration(parsedDuration.getSecs());
        }
    }

    @Override
    public DurationInfoInterface create(ParsedDuration parsedDuration) {
        return new DurationInfo(parsedDuration);
    }

    private Integer adjustDuration(Integer pdVal) {
        if (pdVal == 0) {
            return null;
        }
        if (pdVal < 0) {
            this.durationNegative = ZmBoolean.ONE /* true */;
            return -pdVal;
        } else {
            return pdVal;
        }
    }

    @Override
    public void setDurationNegative(Boolean durationNegative) {
        this.durationNegative = ZmBoolean.fromBool(durationNegative);
    }

    @Override
    public void setWeeks(Integer weeks) { this.weeks = weeks; }
    @Override
    public void setDays(Integer days) { this.days = days; }
    @Override
    public void setHours(Integer hours) { this.hours = hours; }
    @Override
    public void setMinutes(Integer minutes) { this.minutes = minutes; }
    @Override
    public void setSeconds(Integer seconds) { this.seconds = seconds; }
    @Override
    public void setRelated(String related) { this.related = related; }
    @Override
    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    @Override
    public Boolean getDurationNegative() { return ZmBoolean.toBool(durationNegative); }
    @Override
    public Integer getWeeks() { return weeks; }
    @Override
    public Integer getDays() { return days; }
    @Override
    public Integer getHours() { return hours; }
    @Override
    public Integer getMinutes() { return minutes; }
    @Override
    public Integer getSeconds() { return seconds; }
    @Override
    public String getRelated() { return related; }
    @Override
    public Integer getRepeatCount() { return repeatCount; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("durationNegative", durationNegative)
            .add("weeks", weeks)
            .add("days", days)
            .add("hours", hours)
            .add("minutes", minutes)
            .add("seconds", seconds)
            .add("related", related)
            .add("repeatCount", repeatCount)
            .toString();
    }
}
