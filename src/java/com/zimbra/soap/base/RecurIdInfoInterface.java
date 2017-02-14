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

@XmlAccessorType(XmlAccessType.NONE)
public interface RecurIdInfoInterface {
    public RecurIdInfoInterface createFromRangeTypeAndId(
            int recurrenceRangeType, String recurrenceId);
    public void setRecurrenceRangeType(int recurrenceRangeType);
    public void setRecurrenceId(String recurrenceId);
    public void setTimezone(String timezone);
    public void setRecurIdZ(String recurIdZ);
    public int getRecurrenceRangeType();
    public String getRecurrenceId();
    public String getTimezone();
    public String getRecurIdZ();
}
