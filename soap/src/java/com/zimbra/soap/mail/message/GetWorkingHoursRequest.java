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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-auth-required false - Working hours information considered public if available
 * @zm-api-command-admin-auth-required false
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("startTime", startTime)
            .add("endTime", endTime)
            .add("id", id)
            .add("name", name);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
