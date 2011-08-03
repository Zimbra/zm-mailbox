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
import com.zimbra.soap.base.DtTimeInfoInterface;
import com.zimbra.soap.base.DurationInfoInterface;
import com.zimbra.soap.base.SingleDatesInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class SingleDates
implements RecurRuleBase, SingleDatesInterface {

    @XmlAttribute(name=MailConstants.A_CAL_TIMEZONE, required=false)
    private String timezone;

    @XmlElement(name=MailConstants.E_CAL_START_TIME, required=false)
    private DtTimeInfo startTime;

    @XmlElement(name=MailConstants.E_CAL_END_TIME, required=false)
    private DtTimeInfo endTime;

    @XmlElement(name=MailConstants.E_CAL_DURATION, required=false)
    private DurationInfo duration;

    public SingleDates() {
    }

    @Override
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public void setStartTime(DtTimeInfo startTime) {
        this.startTime = startTime;
    }
    public void setEndTime(DtTimeInfo endTime) { this.endTime = endTime; }
    public void setDuration(DurationInfo duration) { this.duration = duration; }
    @Override
    public String getTimezone() { return timezone; }
    public DtTimeInfo getStartTime() { return startTime; }
    public DtTimeInfo getEndTime() { return endTime; }
    public DurationInfo getDuration() { return duration; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("timezone", timezone)
            .add("startTime", startTime)
            .add("endTime", endTime)
            .add("duration", duration);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }

    @Override
    public void setStartTimeInterface(DtTimeInfoInterface startTime) {
        setStartTime((DtTimeInfo) startTime);
    }

    @Override
    public void setEndTimeInterface(DtTimeInfoInterface endTime) {
        setEndTime((DtTimeInfo) endTime);
    }

    @Override
    public void setDurationInterface(DurationInfoInterface duration) {
        setDuration((DurationInfo) duration);
    }

    @Override
    public DtTimeInfoInterface getStartTimeInterface() {
        return startTime;
    }

    @Override
    public DtTimeInfoInterface getEndTimeInterface() {
        return endTime;
    }

    @Override
    public DurationInfoInterface getDurationInterface() {
        return duration;
    }
}
