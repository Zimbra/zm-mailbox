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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.zimbra.common.calendar.ParsedDuration;

@XmlAccessorType(XmlAccessType.NONE)
public interface DurationInfoInterface {
    public DurationInfoInterface create(ParsedDuration parsedDuration);
    public void setDurationNegative(Boolean durationNegative);
    public void setWeeks(Integer weeks);
    public void setDays(Integer days);
    public void setHours(Integer hours);
    public void setMinutes(Integer minutes);
    public void setSeconds(Integer seconds);
    public void setRelated(String related);
    public void setRepeatCount(Integer repeatCount);

    public Boolean getDurationNegative();
    public Integer getWeeks();
    public Integer getDays();
    public Integer getHours();
    public Integer getMinutes();
    public Integer getSeconds();
    public String getRelated();
    public Integer getRepeatCount();
}
