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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class TzOnsetInfo {

    /**
     * @zm-api-field-tag tzonset-week
     * @zm-api-field-description Week number; 1=first, 2=second, 3=third, 4=fourth, -1=last
     */
    @XmlAttribute(name=MailConstants.A_CAL_TZ_WEEK /* week */, required=false)
    private Integer week;

    /**
     * @zm-api-field-tag tzonset-day-of-week
     * @zm-api-field-description Day of week; 1=Sunday, 2=Monday, etc.
     */
    @XmlAttribute(name=MailConstants.A_CAL_TZ_DAYOFWEEK /* wkday */, required=false)
    private Integer dayOfWeek;

    /**
     * @zm-api-field-tag tzonset-month
     * @zm-api-field-description Month; 1=January, 2=February, etc.
     */
    @XmlAttribute(name=MailConstants.A_CAL_TZ_MONTH /* mon */, required=true)
    private Integer month;

    /**
     * @zm-api-field-tag tzonset-day-of-month
     * @zm-api-field-description Day of month (1..31)
     */
    @XmlAttribute(name=MailConstants.A_CAL_TZ_DAYOFMONTH /* mday */, required=false)
    private Integer dayOfMonth;

    /**
     * @zm-api-field-tag tzonset-hour
     * @zm-api-field-description Transition hour (0..23)
     */
    @XmlAttribute(name=MailConstants.A_CAL_TZ_HOUR /* hour */, required=true)
    private Integer hour;

    /**
     * @zm-api-field-tag tzonset-minute
     * @zm-api-field-description Transition minute (0..59)
     */
    @XmlAttribute(name=MailConstants.A_CAL_TZ_MINUTE /* min */, required=true)
    private Integer minute;

    /**
     * @zm-api-field-tag tzonset-second
     * @zm-api-field-description Transition second; 0..59, usually 0
     */
    @XmlAttribute(name=MailConstants.A_CAL_TZ_SECOND /* sec */, required=true)
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
