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
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.ExceptionRecurIdInfoInterface;

// See ToXML.encodeRecurId

@XmlAccessorType(XmlAccessType.NONE)
public class ExceptionRecurIdInfo
implements ExceptionRecurIdInfoInterface {

    /**
     * @zm-api-field-tag YYYYMMDD['T'HHMMSS[Z]]
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
    @XmlAttribute(name=MailConstants.A_CAL_DATETIME /* d */, required=true)
    private final String dateTime;

    /**
     * @zm-api-field-tag timezone-identifier
     * @zm-api-field-description Java timezone identifier
     */
    @XmlAttribute(name=MailConstants.A_CAL_TIMEZONE /* tz */, required=false)
    private String timezone;

    /**
     * @zm-api-field-tag range-type
     * @zm-api-field-description Range type - 1 means NONE, 2 means THISANDFUTURE, 3 means THISANDPRIOR
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_RANGE_TYPE /* rangeType */, required=false)
    private Integer recurrenceRangeType;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ExceptionRecurIdInfo() {
        this((String) null);
    }

    public ExceptionRecurIdInfo(String dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public ExceptionRecurIdInfoInterface create(String dateTime) {
        return new ExceptionRecurIdInfo(dateTime);
    }

    @Override
    public void setTimezone(String timezone) { this.timezone = timezone; }
    @Override
    public void setRecurrenceRangeType(Integer recurrenceRangeType) {
        this.recurrenceRangeType = recurrenceRangeType;
    }

    @Override
    public String getDateTime() { return dateTime; }
    @Override
    public String getTimezone() { return timezone; }
    @Override
    public Integer getRecurrenceRangeType() { return recurrenceRangeType; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("dateTime", dateTime)
            .add("timezone", timezone)
            .add("recurrenceRangeType", recurrenceRangeType)
            .toString();
    }
}
