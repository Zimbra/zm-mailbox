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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"until", "count", "interval", "bySecond", "byMinute",
            "byHour", "byDay", "byMonthDay", "byYearDay", "byWeekNo",
            "byMonth", "bySetPos", "weekStart", "xNames"})
public class SimpleRepeatingRule implements RecurRuleBase {

    @XmlAttribute(name=MailConstants.A_CAL_RULE_FREQ, required=true)
    private final String frequency;

    @XmlElement(name=MailConstants.E_CAL_RULE_UNTIL, required=false)
    private DateTimeStringAttr until;

    @XmlElement(name=MailConstants.E_CAL_RULE_COUNT, required=false)
    private NumAttr count;

    @XmlElement(name=MailConstants.E_CAL_RULE_INTERVAL, required=false)
    private IntervalRule interval;

    @XmlElement(name=MailConstants.E_CAL_RULE_BYSECOND, required=false)
    private BySecondRule bySecond;

    @XmlElement(name=MailConstants.E_CAL_RULE_BYMINUTE, required=false)
    private ByMinuteRule byMinute;

    @XmlElement(name=MailConstants.E_CAL_RULE_BYHOUR, required=false)
    private ByHourRule byHour;

    @XmlElement(name=MailConstants.E_CAL_RULE_BYDAY, required=false)
    private ByDayRule byDay;

    @XmlElement(name=MailConstants.E_CAL_RULE_BYMONTHDAY, required=false)
    private ByMonthDayRule byMonthDay;

    @XmlElement(name=MailConstants.E_CAL_RULE_BYYEARDAY, required=false)
    private ByYearDayRule byYearDay;

    @XmlElement(name=MailConstants.E_CAL_RULE_BYWEEKNO, required=false)
    private ByWeekNoRule byWeekNo;

    @XmlElement(name=MailConstants.E_CAL_RULE_BYMONTH, required=false)
    private ByMonthRule byMonth;

    @XmlElement(name=MailConstants.E_CAL_RULE_BYSETPOS, required=false)
    private BySetPosRule bySetPos;

    @XmlElement(name=MailConstants.E_CAL_RULE_WKST, required=false)
    private WkstRule weekStart;

    @XmlElement(name=MailConstants.E_CAL_RULE_XNAME, required=false)
    private List<XNameRule> xNames = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SimpleRepeatingRule() {
        this((String) null);
    }

    public SimpleRepeatingRule(String frequency) {
        this.frequency = frequency;
    }

    public void setUntil(DateTimeStringAttr until) { this.until = until; }
    public void setCount(NumAttr count) { this.count = count; }
    public void setInterval(IntervalRule interval) { this.interval = interval; }
    public void setBySecond(BySecondRule bySecond) { this.bySecond = bySecond; }
    public void setByMinute(ByMinuteRule byMinute) { this.byMinute = byMinute; }
    public void setByHour(ByHourRule byHour) { this.byHour = byHour; }
    public void setByDay(ByDayRule byDay) { this.byDay = byDay; }
    public void setByMonthDay(ByMonthDayRule byMonthDay) {
        this.byMonthDay = byMonthDay;
    }

    public void setByYearDay(ByYearDayRule byYearDay) {
        this.byYearDay = byYearDay;
    }

    public void setByWeekNo(ByWeekNoRule byWeekNo) { this.byWeekNo = byWeekNo; }
    public void setByMonth(ByMonthRule byMonth) { this.byMonth = byMonth; }
    public void setBySetPos(BySetPosRule bySetPos) { this.bySetPos = bySetPos; }
    public void setWeekStart(WkstRule weekStart) { this.weekStart = weekStart; }
    public void setXNames(Iterable <XNameRule> xNames) {
        this.xNames.clear();
        if (xNames != null) {
            Iterables.addAll(this.xNames,xNames);
        }
    }

    public SimpleRepeatingRule addXName(XNameRule xName) {
        this.xNames.add(xName);
        return this;
    }

    public String getFrequency() { return frequency; }
    public DateTimeStringAttr getUntil() { return until; }
    public NumAttr getCount() { return count; }
    public IntervalRule getInterval() { return interval; }
    public BySecondRule getBySecond() { return bySecond; }
    public ByMinuteRule getByMinute() { return byMinute; }
    public ByHourRule getByHour() { return byHour; }
    public ByDayRule getByDay() { return byDay; }
    public ByMonthDayRule getByMonthDay() { return byMonthDay; }
    public ByYearDayRule getByYearDay() { return byYearDay; }
    public ByWeekNoRule getByWeekNo() { return byWeekNo; }
    public ByMonthRule getByMonth() { return byMonth; }
    public BySetPosRule getBySetPos() { return bySetPos; }
    public WkstRule getWeekStart() { return weekStart; }
    public List<XNameRule> getXNames() {
        return Collections.unmodifiableList(xNames);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("frequency", frequency)
            .add("until", until)
            .add("count", count)
            .add("interval", interval)
            .add("bySecond", bySecond)
            .add("byMinute", byMinute)
            .add("byHour", byHour)
            .add("byDay", byDay)
            .add("byMonthDay", byMonthDay)
            .add("byYearDay", byYearDay)
            .add("byWeekNo", byWeekNo)
            .add("byMonth", byMonth)
            .add("bySetPos", bySetPos)
            .add("weekStart", weekStart)
            .add("xNames", xNames);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
