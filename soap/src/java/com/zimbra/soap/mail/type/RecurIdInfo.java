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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.RecurIdInfoInterface;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_RECURRENCE_ID_INFORMATION, description="Recurrence ID Information")
public class RecurIdInfo implements RecurIdInfoInterface {

    /**
     * @zm-api-field-tag range-type
     * @zm-api-field-description Recurrence range type
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_RANGE_TYPE /* rangeType */, required=true)
    private int recurrenceRangeType;

    /**
     * @zm-api-field-tag YYMMDD[THHMMSS[Z]]
     * @zm-api-field-description Recurrence ID in format : YYMMDD[THHMMSS[Z]]
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID /* recurId */, required=true)
    private String recurrenceId;

    /**
     * @zm-api-field-tag timezone-name
     * @zm-api-field-description Timezone name
     */
    @XmlAttribute(name=MailConstants.A_CAL_TIMEZONE /* tz */, required=false)
    private String timezone;

    /**
     * @zm-api-field-tag YYMMDDTHHMMSSZ
     * @zm-api-field-description Recurrence-id in UTC time zone; used in non-all-day appointments only
     * <br />
     * Format: YYMMDDTHHMMSSZ
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID_Z /* ridZ */, required=false)
    private String recurIdZ;

    public RecurIdInfo() {
        this(-1, (String) null);
    }

    public RecurIdInfo(int recurrenceRangeType, String recurrenceId) {
        this.setRecurrenceRangeType(recurrenceRangeType);
        this.setRecurrenceId(recurrenceId);
    }

    @Override
    public RecurIdInfoInterface createFromRangeTypeAndId(
            int recurrenceRangeType, String recurrenceId) {
        return new RecurIdInfo(recurrenceRangeType, recurrenceId);
    }

    @Override
    @GraphQLInputField(name=GqlConstants.RECURRENCE_RANGE_TYPE, description="Recurrence range type")
    public void setRecurrenceRangeType(int recurrenceRangeType) {
        this.recurrenceRangeType = recurrenceRangeType;
    }

    @Override
    @GraphQLInputField(name=GqlConstants.RECURRENCE_ID, description="Recurrence ID in format : YYMMDD[THHMMSS[Z]]")
    public void setRecurrenceId(@GraphQLNonNull String recurrenceId) {
        this.recurrenceId = recurrenceId;
    }

    @Override
    @GraphQLInputField(name=GqlConstants.TIMEZONE, description="Timezone name")
    public void setTimezone(String timezone) { this.timezone = timezone; }
    @Override
    @GraphQLInputField(name=GqlConstants.RECURRENCE_ID_Z, description="Recurrence-id in UTC time zone; used in non-all-day appointments only")
    public void setRecurIdZ(String recurIdZ) { this.recurIdZ = recurIdZ; }

    @Override
    @GraphQLQuery(name=GqlConstants.RECURRENCE_RANGE_TYPE, description="Recurrence range type")
    public int getRecurrenceRangeType() { return recurrenceRangeType; }
    @Override
    @GraphQLQuery(name=GqlConstants.RECURRENCE_ID, description="Recurrence ID in format : YYMMDD[THHMMSS[Z]]")
    public String getRecurrenceId() { return recurrenceId; }
    @Override
    @GraphQLQuery(name=GqlConstants.TIMEZONE, description="Timezone name")
    public String getTimezone() { return timezone; }
    @Override
    @GraphQLQuery(name=GqlConstants.RECURRENCE_ID_Z, description="Recurrence-id in UTC time zone; used in non-all-day appointments only")
    public String getRecurIdZ() { return recurIdZ; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("recurrenceRangeType", getRecurrenceRangeType())
            .add("recurrenceId", getRecurrenceId())
            .add("timezone", timezone)
            .add("recurIdZ", recurIdZ);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
