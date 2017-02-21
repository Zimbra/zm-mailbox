/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.base;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.NONE)
public interface SimpleRepeatingRuleInterface {
    public String getFrequency();
    public void setUntilInterface(DateTimeStringAttrInterface until);
    public void setCountInterface(NumAttrInterface count);
    public void setIntervalInterface(IntervalRuleInterface interval);
    public void setBySecondInterface(BySecondRuleInterface bySecond);
    public void setByMinuteInterface(ByMinuteRuleInterface byMinute);
    public void setByHourInterface(ByHourRuleInterface byHour);
    public void setByDayInterface(ByDayRuleInterface byDay);
    public void setByMonthDayInterface(ByMonthDayRuleInterface byMonthDay);
    public void setByYearDayInterface(ByYearDayRuleInterface byYearDay);
    public void setByWeekNoInterface(ByWeekNoRuleInterface byWeekNo);
    public void setByMonthInterface(ByMonthRuleInterface byMonth);
    public void setBySetPosInterface(BySetPosRuleInterface bySetPos);
    public void setWeekStartInterface(WkstRuleInterface weekStart);
    public void setXNameInterfaces(Iterable<XNameRuleInterface> xNames);
    public void addXNameInterface(XNameRuleInterface xName);
    public DateTimeStringAttrInterface getUntilInterface();
    public NumAttrInterface getCountInterface();
    public IntervalRuleInterface getIntervalInterface();
    public BySecondRuleInterface getBySecondInterface();
    public ByMinuteRuleInterface getByMinuteInterface();
    public ByHourRuleInterface getByHourInterface();
    public ByDayRuleInterface getByDayInterface();
    public ByMonthDayRuleInterface getByMonthDayInterface();
    public ByYearDayRuleInterface getByYearDayInterface();
    public ByWeekNoRuleInterface getByWeekNoInterface();
    public ByMonthRuleInterface getByMonthInterface();
    public BySetPosRuleInterface getBySetPosInterface();
    public WkstRuleInterface getWeekStartInterface();
    public List<XNameRuleInterface> getXNamesInterface();
}
