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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class TZFixupRuleMatchRule {

    /**
     * @zm-api-field-tag match-month
     * @zm-api-field-description Match month.  Value between 1 (January) and 12 (December)
     */
    @XmlAttribute(name=AdminConstants.A_MON /* mon */, required=true)
    private final int month;

    /**
     * @zm-api-field-tag match-week
     * @zm-api-field-description Match week.  -1 means last week of month else between 1 and 4
     */
    @XmlAttribute(name=AdminConstants.A_WEEK /* week */, required=true)
    private final int week;

    /**
     * @zm-api-field-tag match-week-day
     * @zm-api-field-description Match week day.  Value between 1 (Sunday) and 7 (Saturday)
     */
    @XmlAttribute(name=AdminConstants.A_WKDAY /* wkday */, required=true)
    private final int weekDay;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private TZFixupRuleMatchRule() {
        this(-1, -1, -1);
    }

    public TZFixupRuleMatchRule(int month, int week, int weekDay) {
        this.month = month;
        this.week = week;
        this.weekDay = weekDay;
    }

    public int getMonth() { return month; }
    public int getWeek() { return week; }
    public int getWeekDay() { return weekDay; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("month", month)
            .add("week", week)
            .add("weekDay", weekDay);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
