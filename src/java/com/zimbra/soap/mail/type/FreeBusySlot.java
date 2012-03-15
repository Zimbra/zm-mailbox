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
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class FreeBusySlot {

    /**
     * @zm-api-field-tag start-millis-gmt
     * @zm-api-field-description GMT Start time for slot in milliseconds
     */
    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=true)
    private final long startTime;

    /**
     * @zm-api-field-tag end-millis-gmt
     * @zm-api-field-description GMT End time for slot in milliseconds
     */
    @XmlAttribute(name=MailConstants.A_CAL_END_TIME /* e */, required=true)
    private final long endTime;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    protected FreeBusySlot() {
        this(-1L, -1L);
    }

    public FreeBusySlot(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("startTime", startTime)
            .add("endTime", endTime);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
