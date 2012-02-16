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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-description User's working hours within the given time range are expressed in a similar format
 * to the format used for GetFreeBusy.
 * <br />
 * Working hours are indicated as free, non-working hours as unavailable/out of office.
 * The entire time range is marked as unknown if there was an error determining the working hours,
 * e.g. unknown user.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_WORKING_HOURS_REQUEST)
public class GetWorkingHoursRequest {

    /**
     * @zm-api-field-tag range-start-millis
     * @zm-api-field-description Range start in milliseconds since the epoch
     */
    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=true)
    private final long startTime;

    /**
     * @zm-api-field-tag range-end-millis
     * @zm-api-field-description Range end in milliseconds since the epoch
     */
    @XmlAttribute(name=MailConstants.A_CAL_END_TIME /* e */, required=true)
    private final long endTime;

    /**
     * @zm-api-field-tag comma-sep-zimbra-ids
     * @zm-api-field-description Comma-separated list of Zimbra IDs
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag comma-sep-emails
     * @zm-api-field-description Comma-separated list of email addresses
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetWorkingHoursRequest() {
        this(-1L, -1L);
    }

    public GetWorkingHoursRequest(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public String getId() { return id; }
    public String getName() { return name; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("startTime", startTime)
            .add("endTime", endTime)
            .add("id", id)
            .add("name", name);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
