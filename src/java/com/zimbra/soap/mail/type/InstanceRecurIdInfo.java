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
public class InstanceRecurIdInfo {

    /**
     * @zm-api-field-tag range-THISANDFUTURE|THISANDPRIOR
     * @zm-api-field-description Range - <b>THISANDFUTURE|THISANDPRIOR</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_RANGE /* range */, required=false)
    private String range;

    /**
     * @zm-api-field-tag DATETIME-YYYYMMDD['T'HHMMSS[Z]]
     * @zm-api-field-description Date and/or time.  Format is : <b>YYYYMMDD['T'HHMMSS[Z]]</b>
     * <br />
     * where:
     * <pre>
     *     YYYY - 4 digit year
     *     MM   - 2 digit month
     *     DD   - 2 digit day
     * Optionally:
     *     'T' the literal char "T" then 
     *     HH - 2 digit hour (00-23)
     *     MM - 2 digit minute (00-59)
     *     SS - 2 digit second (00-59)
     *     ...and finally an optional "Z" meaning that the time is UTC,
     *     otherwise the tz="TIMEZONE" param MUST be specified with the DATETIME
     *     e.g:
     *         20050612  June 12, 2005
     *         20050315T18302305Z  March 15, 2005 6:30:23.05 PM UTC
     * </pre>
     */
    @XmlAttribute(name=MailConstants.A_CAL_DATETIME /* d */, required=false)
    private String dateTime;

    /**
     * @zm-api-field-tag timezone-identifier
     * @zm-api-field-description Java timezone identifier
     */
    @XmlAttribute(name=MailConstants.A_CAL_TIMEZONE /* tz */, required=false)
    private String timezone;

    public InstanceRecurIdInfo() {
    }

    public void setRange(String range) { this.range = range; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getRange() { return range; }
    public String getDateTime() { return dateTime; }
    public String getTimezone() { return timezone; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("range", range)
            .add("dateTime", dateTime)
            .add("timezone", timezone);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
