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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.AlarmTriggerInfoInterface;
import com.zimbra.soap.base.DateAttrInterface;
import com.zimbra.soap.base.DurationInfoInterface;

@XmlAccessorType(XmlAccessType.NONE)
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
        return Objects.toStringHelper(this)
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
