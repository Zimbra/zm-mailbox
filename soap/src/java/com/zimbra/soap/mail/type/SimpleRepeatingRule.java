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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.ByDayRuleInterface;
import com.zimbra.soap.base.ByHourRuleInterface;
import com.zimbra.soap.base.ByMinuteRuleInterface;
import com.zimbra.soap.base.ByMonthDayRuleInterface;
import com.zimbra.soap.base.ByMonthRuleInterface;
import com.zimbra.soap.base.BySecondRuleInterface;
import com.zimbra.soap.base.BySetPosRuleInterface;
import com.zimbra.soap.base.ByWeekNoRuleInterface;
import com.zimbra.soap.base.ByYearDayRuleInterface;
import com.zimbra.soap.base.DateTimeStringAttrInterface;
import com.zimbra.soap.base.IntervalRuleInterface;
import com.zimbra.soap.base.NumAttrInterface;
import com.zimbra.soap.base.SimpleRepeatingRuleInterface;
import com.zimbra.soap.base.WkstRuleInterface;
import com.zimbra.soap.base.XNameRuleInterface;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_SIMPLE_REPEATING_RULE, description="Simple repeating rule information")
public class SimpleRepeatingRule
implements RecurRuleBase, SimpleRepeatingRuleInterface {

    /**
     * @zm-api-field-tag freq
     * @zm-api-field-description Frequency - <b>SEC,MIN,HOU,DAI,WEE,MON,YEA</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_RULE_FREQ /* freq */, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.FREQUENCY, description="Frequency. SEC, MIN, HOU, DAI, WEE, MON, YEA")
    private final String frequency;

    /**
     * @zm-api-field-tag until
     * @zm-api-field-description UNTIL date specification
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_UNTIL /* until */, required=false)
    private DateTimeStringAttr until;

    /**
     * @zm-api-field-tag instance-count
     * @zm-api-field-description Count of instances to generate
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_COUNT /* count */, required=false)
    private NumAttr count;

    /**
     * @zm-api-field-description Interval specification
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_INTERVAL /* interval */, required=false)
    private IntervalRule interval;

    /**
     * @zm-api-field-description BYSECOND rule
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_BYSECOND /* bysecond */, required=false)
    private BySecondRule bySecond;

    /**
     * @zm-api-field-description BYMINUTE rule
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_BYMINUTE /* byminute */, required=false)
    private ByMinuteRule byMinute;

    /**
     * @zm-api-field-description BYHOUR rule
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_BYHOUR /* byhour */, required=false)
    private ByHourRule byHour;

    /**
     * @zm-api-field-description BYDAY rule
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_BYDAY /* byday */, required=false)
    private ByDayRule byDay;

    /**
     * @zm-api-field-description BYMONTHDAY rule
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_BYMONTHDAY /* bymonthday */, required=false)
    private ByMonthDayRule byMonthDay;

    /**
     * @zm-api-field-description BYYEARDAY rule
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_BYYEARDAY /* byyearday */, required=false)
    private ByYearDayRule byYearDay;

    /**
     * @zm-api-field-description BYWEEKNO rule
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_BYWEEKNO /* byweekno */, required=false)
    private ByWeekNoRule byWeekNo;

    /**
     * @zm-api-field-description BYMONTH rule
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_BYMONTH /* bymonth */, required=false)
    private ByMonthRule byMonth;

    /**
     * @zm-api-field-description BYSETPOS rule
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_BYSETPOS /* bysetpos */, required=false)
    private BySetPosRule bySetPos;

    /**
     * @zm-api-field-tag wkst
     * @zm-api-field-description Week start day - <b>SU,MO,TU,WE,TH,FR,SA</b>
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_WKST /* wkst */, required=false)
    private WkstRule weekStart;

    /**
     * @zm-api-field-description X Name rules
     */
    @XmlElement(name=MailConstants.E_CAL_RULE_XNAME /* rule-x-name */, required=false)
    @GraphQLQuery(name=GqlConstants.X_NAMES, description="X Name rules")
    private final List<XNameRule> xNames = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SimpleRepeatingRule() {
        this((String) null);
    }

    public SimpleRepeatingRule(@GraphQLNonNull @GraphQLInputField(name=GqlConstants.FREQUENCY) String frequency) {
        this.frequency = frequency;
    }

    public static SimpleRepeatingRule createFromFrequencyAndInterval(String freq, IntervalRule ir) {
        SimpleRepeatingRule srr = new SimpleRepeatingRule(freq);
        srr.setInterval(ir);
        return srr;
    }

    public static SimpleRepeatingRuleInterface createFromFrequency(String frequency) {
        return new SimpleRepeatingRule(frequency);
    }

    @GraphQLInputField(name=GqlConstants.UNTIL, description="UNTIL date specification")
    public void setUntil(DateTimeStringAttr until) { this.until = until; }
    @GraphQLInputField(name=GqlConstants.COUNT, description="Count of instances to generate")
    public void setCount(NumAttr count) { this.count = count; }
    @GraphQLInputField(name=GqlConstants.INTERVAL, description="Interval specification")
    public void setInterval(IntervalRule interval) { this.interval = interval; }
    @GraphQLInputField(name=GqlConstants.BY_SECOND, description="BYYEARDAY rule")
    public void setBySecond(BySecondRule bySecond) { this.bySecond = bySecond; }
    @GraphQLInputField(name=GqlConstants.BY_MINUTE, description="BYMINUTE rule")
    public void setByMinute(ByMinuteRule byMinute) { this.byMinute = byMinute; }
    @GraphQLInputField(name=GqlConstants.BY_HOUR, description="BYHOUR rule")
    public void setByHour(ByHourRule byHour) { this.byHour = byHour; }
    @GraphQLInputField(name=GqlConstants.BY_DAY, description="BYDAY rule")
    public void setByDay(ByDayRule byDay) { this.byDay = byDay; }
    @GraphQLInputField(name=GqlConstants.BY_MONTH_DAY, description="BYMONTHDAY rule")
    public void setByMonthDay(ByMonthDayRule byMonthDay) {
        this.byMonthDay = byMonthDay;
    }
    @GraphQLInputField(name=GqlConstants.BY_YEAR_DAY, description="BYYEARDAY")
    public void setByYearDay(ByYearDayRule byYearDay) {
        this.byYearDay = byYearDay;
    }
    @GraphQLInputField(name=GqlConstants.BY_WEEK_NO, description="BYWEEKNO rule")
    public void setByWeekNo(ByWeekNoRule byWeekNo) { this.byWeekNo = byWeekNo; }
    @GraphQLInputField(name=GqlConstants.BY_MONTH, description="BYMONTH rule")
    public void setByMonth(ByMonthRule byMonth) { this.byMonth = byMonth; }
    @GraphQLInputField(name=GqlConstants.BY_SET_POS, description="BYSETPOS rule")
    public void setBySetPos(BySetPosRule bySetPos) { this.bySetPos = bySetPos; }
    @GraphQLInputField(name=GqlConstants.WEEK_START, description="Week start day - SU,MO,TU,WE,TH,FR,SA")
    public void setWeekStart(WkstRule weekStart) { this.weekStart = weekStart; }
    @GraphQLInputField(name=GqlConstants.X_NAMES, description="X Name rules")
    public void setXNames(Iterable <XNameRule> xNames) {
        this.xNames.clear();
        if (xNames != null) {
            Iterables.addAll(this.xNames,xNames);
        }
    }

    @GraphQLIgnore
    public SimpleRepeatingRule addXName(XNameRule xName) {
        this.xNames.add(xName);
        return this;
    }

    @Override
    @GraphQLQuery(name=GqlConstants.FREQUENCY, description="Frequency. SEC, MIN, HOU, DAI, WEE, MON, YEA")
    public String getFrequency() { return frequency; }
    @GraphQLQuery(name=GqlConstants.UNTIL, description="UNTIL date specification")
    public DateTimeStringAttr getUntil() { return until; }
    @GraphQLQuery(name=GqlConstants.COUNT, description="Count of instances to generate")
    public NumAttr getCount() { return count; }
    @GraphQLQuery(name=GqlConstants.INTERVAL, description="Interval specification")
    public IntervalRule getInterval() { return interval; }
    @GraphQLQuery(name=GqlConstants.BY_SECOND, description="BYYEARDAY rule")
    public BySecondRule getBySecond() { return bySecond; }
    @GraphQLQuery(name=GqlConstants.BY_MINUTE, description="BYMINUTE rule")
    public ByMinuteRule getByMinute() { return byMinute; }
    @GraphQLQuery(name=GqlConstants.BY_HOUR, description="BYHOUR rule")
    public ByHourRule getByHour() { return byHour; }
    @GraphQLQuery(name=GqlConstants.BY_DAY, description="BYDAY rule")
    public ByDayRule getByDay() { return byDay; }
    @GraphQLQuery(name=GqlConstants.BY_MONTH_DAY, description="BYMONTHDAY rule")
    public ByMonthDayRule getByMonthDay() { return byMonthDay; }
    @GraphQLQuery(name=GqlConstants.BY_YEAR_DAY, description="BYYEARDAY")
    public ByYearDayRule getByYearDay() { return byYearDay; }
    @GraphQLQuery(name=GqlConstants.BY_WEEK_NO, description="BYWEEKNO rule")
    public ByWeekNoRule getByWeekNo() { return byWeekNo; }
    @GraphQLQuery(name=GqlConstants.BY_MONTH, description="BYMONTH rule")
    public ByMonthRule getByMonth() { return byMonth; }
    @GraphQLQuery(name=GqlConstants.BY_SET_POS, description="BYSETPOS rule")
    public BySetPosRule getBySetPos() { return bySetPos; }
    @GraphQLQuery(name=GqlConstants.WEEK_START, description="Week start day - SU,MO,TU,WE,TH,FR,SA")
    public WkstRule getWeekStart() { return weekStart; }
    @GraphQLQuery(name=GqlConstants.X_NAMES, description="X Name rules")
    public List<XNameRule> getXNames() {
        return Collections.unmodifiableList(xNames);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
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
    @GraphQLIgnore
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

    @Override
    @GraphQLIgnore
    public void setUntilInterface(DateTimeStringAttrInterface until) {
        setUntil((DateTimeStringAttr) until);
    }

    @Override
    @GraphQLIgnore
    public void setCountInterface(NumAttrInterface count) {
        setCount((NumAttr) count);
    }

    @Override
    @GraphQLIgnore
    public void setIntervalInterface(IntervalRuleInterface interval) {
        setInterval((IntervalRule) interval);
    }

    @Override
    @GraphQLIgnore
    public void setBySecondInterface(BySecondRuleInterface bySecond) {
        setBySecond((BySecondRule) bySecond);
    }

    @Override
    @GraphQLIgnore
    public void setByMinuteInterface(ByMinuteRuleInterface byMinute) {
        setByMinute((ByMinuteRule) byMinute);
    }

    @Override
    @GraphQLIgnore
    public void setByHourInterface(ByHourRuleInterface byHour) {
        setByHour((ByHourRule) byHour);
    }

    @Override
    @GraphQLIgnore
    public void setByDayInterface(ByDayRuleInterface byDay) {
        setByDay((ByDayRule) byDay);
    }

    @Override
    @GraphQLIgnore
    public void setByMonthDayInterface(ByMonthDayRuleInterface byMonthDay) {
        setByMonthDay((ByMonthDayRule) byMonthDay);
    }

    @Override
    @GraphQLIgnore
    public void setByYearDayInterface(ByYearDayRuleInterface byYearDay) {
        setByYearDay((ByYearDayRule) byYearDay);
    }

    @Override
    @GraphQLIgnore
    public void setByWeekNoInterface(ByWeekNoRuleInterface byWeekNo) {
        setByWeekNo((ByWeekNoRule) byWeekNo);
    }

    @Override
    @GraphQLIgnore
    public void setByMonthInterface(ByMonthRuleInterface byMonth) {
        setByMonth((ByMonthRule) byMonth);
    }

    @Override
    @GraphQLIgnore
    public void setBySetPosInterface(BySetPosRuleInterface bySetPos) {
        setBySetPos((BySetPosRule) bySetPos);
    }

    @Override
    @GraphQLIgnore
    public void setWeekStartInterface(WkstRuleInterface weekStart) {
        setWeekStart((WkstRule) weekStart);
    }

    @Override
    @GraphQLIgnore
    public void setXNameInterfaces(Iterable<XNameRuleInterface> xNames) {
        setXNames(XNameRule.fromInterfaces(xNames));
    }

    @Override
    @GraphQLIgnore
    public void addXNameInterface(XNameRuleInterface xName) {
        addXName((XNameRule) xName);
    }

    @Override
    @GraphQLIgnore
    public DateTimeStringAttrInterface getUntilInterface() {
        return until;
    }

    @Override
    @GraphQLIgnore
    public NumAttrInterface getCountInterface() {
        return count;
    }

    @Override
    @GraphQLIgnore
    public IntervalRuleInterface getIntervalInterface() {
        return interval;
    }

    @Override
    @GraphQLIgnore
    public BySecondRuleInterface getBySecondInterface() {
        return this.bySecond;
    }

    @Override
    @GraphQLIgnore
    public ByMinuteRuleInterface getByMinuteInterface() {
        return this.byMinute;
    }

    @Override
    @GraphQLIgnore
    public ByHourRuleInterface getByHourInterface() {
        return this.byHour;
    }

    @Override
    @GraphQLIgnore
    public ByDayRuleInterface getByDayInterface() {
        return this.byDay;
    }

    @Override
    @GraphQLIgnore
    public ByMonthDayRuleInterface getByMonthDayInterface() {
        return this.byMonthDay;
    }

    @Override
    @GraphQLIgnore
    public ByYearDayRuleInterface getByYearDayInterface() {
        return this.byYearDay;
    }

    @Override
    @GraphQLIgnore
    public ByWeekNoRuleInterface getByWeekNoInterface() {
        return this.byWeekNo;
    }

    @Override
    @GraphQLIgnore
    public ByMonthRuleInterface getByMonthInterface() {
        return this.byMonth;
    }

    @Override
    @GraphQLIgnore
    public BySetPosRuleInterface getBySetPosInterface() {
        return this.bySetPos;
    }

    @Override
    @GraphQLIgnore
    public WkstRuleInterface getWeekStartInterface() {
        return this.weekStart;
    }

    @Override
    @GraphQLIgnore
    public List<XNameRuleInterface> getXNamesInterface() {
        return XNameRule.toInterfaces(xNames);
    }
}
