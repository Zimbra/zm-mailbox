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

@XmlAccessorType(XmlAccessType.FIELD)
public class RecurIdInfo {

    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_RANGE_TYPE, required=true)
    private final int recurrenceRangeType;

    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID, required=true)
    private final String recurrenceId;

    @XmlAttribute(name=MailConstants.A_CAL_TIMEZONE, required=false)
    private String timezone;

    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID_Z, required=false)
    private String recurIdZ;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    protected RecurIdInfo() {
        this(-1, (String) null);
    }

    public RecurIdInfo(int recurrenceRangeType, String recurrenceId) {
        this.recurrenceRangeType = recurrenceRangeType;
        this.recurrenceId = recurrenceId;
    }

    public void setTimezone(String timezone) { this.timezone = timezone; }
    public void setRecurIdZ(String recurIdZ) { this.recurIdZ = recurIdZ; }
    public int getRecurrenceRangeType() { return recurrenceRangeType; }
    public String getRecurrenceId() { return recurrenceId; }
    public String getTimezone() { return timezone; }
    public String getRecurIdZ() { return recurIdZ; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("recurrenceRangeType", recurrenceRangeType)
            .add("recurrenceId", recurrenceId)
            .add("timezone", timezone)
            .add("recurIdZ", recurIdZ)
            .toString();
    }
}
