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

import com.zimbra.soap.type.TzOnsetInfo;

@XmlAccessorType(XmlAccessType.NONE)
public interface CalTZInfoInterface {
    public CalTZInfoInterface createFromIdStdOffsetDayOffset(String id,
            Integer tzStdOffset, Integer tzDayOffset);
    public void setStandardTzOnset(TzOnsetInfo standardTzOnset);
    public void setDaylightTzOnset(TzOnsetInfo daylightTzOnset);
    public void setStandardTZName(String standardTZName);
    public void setDaylightTZName(String daylightTZName);
    public String getId();
    public Integer getTzStdOffset();
    public Integer getTzDayOffset();
    public TzOnsetInfo getStandardTzOnset();
    public TzOnsetInfo getDaylightTzOnset();
    public String getStandardTZName();
    public String getDaylightTZName();
}
