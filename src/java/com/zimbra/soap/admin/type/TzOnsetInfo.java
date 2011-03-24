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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class TzOnsetInfo {

    @XmlAttribute(name=MailConstants.A_CAL_TZ_WEEK, required=false)
    private Integer week;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_DAYOFWEEK, required=false)
    private Integer dayOfWeek;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_MONTH, required=true)
    private Integer month;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_DAYOFMONTH, required=false)
    private Integer dayOfMonth;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_HOUR, required=true)
    private Integer hour;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_MINUTE, required=true)
    private Integer minute;

    @XmlAttribute(name=MailConstants.A_CAL_TZ_SECOND, required=true)
    private Integer second;

    public TzOnsetInfo() {
    }

    public void setWeek(Integer week) { this.week = week; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setMonth(Integer month) { this.month = month; }
    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public void setHour(Integer hour) { this.hour = hour; }
    public void setMinute(Integer minute) { this.minute = minute; }
    public void setSecond(Integer second) { this.second = second; }
    public Integer getWeek() { return week; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public Integer getMonth() { return month; }
    public Integer getDayOfMonth() { return dayOfMonth; }
    public Integer getHour() { return hour; }
    public Integer getMinute() { return minute; }
    public Integer getSecond() { return second; }
}
