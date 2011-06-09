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

import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class DurationInfo {

    @XmlAttribute(name=MailConstants.A_CAL_DURATION_NEGATIVE, required=false)
    private Boolean durationNegative;

    @XmlAttribute(name=MailConstants.A_CAL_DURATION_WEEKS, required=false)
    private Integer weeks;

    @XmlAttribute(name=MailConstants.A_CAL_DURATION_DAYS, required=false)
    private Integer days;

    @XmlAttribute(name=MailConstants.A_CAL_DURATION_HOURS, required=false)
    private Integer hours;

    @XmlAttribute(name=MailConstants.A_CAL_DURATION_MINUTES, required=false)
    private Integer minutes;

    @XmlAttribute(name=MailConstants.A_CAL_DURATION_SECONDS, required=false)
    private Integer seconds;

    @XmlAttribute(name=MailConstants.A_CAL_ALARM_RELATED, required=false)
    private String related;

    @XmlAttribute(name=MailConstants.A_CAL_ALARM_COUNT, required=false)
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

    private Integer adjustDuration(Integer pdVal) {
        if (pdVal == 0) {
            return null;
        }
        if (pdVal < 0) {
            this.durationNegative = true;
            return -pdVal;
        } else {
            return pdVal;
        }
    }

    public void setDurationNegative(Boolean durationNegative) {
        this.durationNegative = durationNegative;
    }

    public void setWeeks(Integer weeks) { this.weeks = weeks; }
    public void setDays(Integer days) { this.days = days; }
    public void setHours(Integer hours) { this.hours = hours; }
    public void setMinutes(Integer minutes) { this.minutes = minutes; }
    public void setSeconds(Integer seconds) { this.seconds = seconds; }
    public void setRelated(String related) { this.related = related; }
    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public Boolean getDurationNegative() { return durationNegative; }
    public Integer getWeeks() { return weeks; }
    public Integer getDays() { return days; }
    public Integer getHours() { return hours; }
    public Integer getMinutes() { return minutes; }
    public Integer getSeconds() { return seconds; }
    public String getRelated() { return related; }
    public Integer getRepeatCount() { return repeatCount; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
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
