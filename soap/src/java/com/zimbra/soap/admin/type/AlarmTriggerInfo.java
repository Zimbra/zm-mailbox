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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.AlarmTriggerInfoInterface;
import com.zimbra.soap.base.DateAttrInterface;
import com.zimbra.soap.base.DurationInfoInterface;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class AlarmTriggerInfo implements AlarmTriggerInfoInterface {

    /**
     * @zm-api-field-description Absolute trigger information
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM_ABSOLUTE /* abs */, required=false)
    private DateAttr absolute;

    /**
     * @zm-api-field-description Relative trigger information
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM_RELATIVE /* rel */, required=false)
    private DurationInfo relative;

    public AlarmTriggerInfo() {
    }

    public void setAbsolute(DateAttr absolute) { this.absolute = absolute; }
    public void setRelative(DurationInfo relative) { this.relative = relative; }
    public DateAttr getAbsolute() { return absolute; }
    public DurationInfo getRelative() { return relative; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("absolute", absolute)
            .add("relative", relative)
            .toString();
    }

    @Override
    public void setAbsoluteInterface(DateAttrInterface absolute) {
        setAbsolute((DateAttr) absolute);
    }

    @Override
    public void setRelativeInterface(DurationInfoInterface relative) {
        setRelative((DurationInfo) relative);
    }

    @Override
    public DateAttrInterface getAbsoluteInterface() {
        return absolute;
    }

    @Override
    public DurationInfoInterface getRelativeInterface() {
        return relative;
    }
}
