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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Id;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_CALENDAR_ACTION_RESPONSE, description="Contains response information for calendar actions (create, modify, reply)")
public class CreateCalendarItemResponse {

    /**
     * @zm-api-field-tag appointment-id
     * @zm-api-field-description Appointment ID
     */
    @XmlAttribute(name=MailConstants.A_CAL_ID /* calItemId */, required=false)
    private String calItemId;

    // For backwards compat
    /**
     * @zm-api-field-tag deprecated-appt-id
     * @zm-api-field-description Appointment ID (deprecated)
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_APPT_ID_DEPRECATE_ME /* apptId */, required=false)
    private String deprecatedApptId;

    /**
     * @zm-api-field-tag invite-message-id
     * @zm-api-field-description Invite Message ID
     */
    @XmlAttribute(name=MailConstants.A_CAL_INV_ID /* invId */, required=false)
    private String calInvId;

    /**
     * @zm-api-field-tag change-sequence
     * @zm-api-field-description Change sequence
     */
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    /**
     * @zm-api-field-tag revision
     * @zm-api-field-description Revision
     */
    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    /**
     * @zm-api-field-description Message information
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private Id msg;

    /**
     * @zm-api-field-tag echo
     * @zm-api-field-description Included if "echo" was set in the request
     */
    @XmlElement(name=MailConstants.A_CAL_ECHO /* echo */, required=false)
    private CalEcho echo;

    public CreateCalendarItemResponse() {
    }

    public void setCalItemId(String calItemId) { this.calItemId = calItemId; }
    @Deprecated
    public void setDeprecatedApptId(String deprecatedApptId) {
        this.deprecatedApptId = deprecatedApptId;
    }
    public void setCalInvId(String calInvId) { this.calInvId = calInvId; }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setRevision(Integer revision) { this.revision = revision; }
    public void setMsg(Id msg) { this.msg = msg; }
    public void setEcho(CalEcho echo) { this.echo = echo; }
    @GraphQLQuery(name=GqlConstants.CALENDAR_ITEM_ID, description="Appointment ID")
    public String getCalItemId() { return calItemId; }
    @Deprecated
    @GraphQLIgnore
    public String getDeprecatedApptId() { return deprecatedApptId; }
    @GraphQLQuery(name=GqlConstants.CALENDAR_INVITE_ID, description="Invite message ID")
    public String getCalInvId() { return calInvId; }
    @GraphQLQuery(name=GqlConstants.MODIFIED_SEQUENCE, description="Change sequence")
    public Integer getModifiedSequence() { return modifiedSequence; }
    @GraphQLQuery(name=GqlConstants.REVISION, description="Revision")
    public Integer getRevision() { return revision; }
    @GraphQLQuery(name=GqlConstants.MESSAGE_ID, description="Message information")
    public Id getMsg() { return msg; }
    @GraphQLQuery(name=GqlConstants.ECHO, description="Included if includeEcho was specified")
    public CalEcho getEcho() { return echo; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("calItemId", calItemId)
            .add("deprecatedApptId", deprecatedApptId)
            .add("calInvId", calInvId)
            .add("modifiedSequence", modifiedSequence)
            .add("revision", revision)
            .add("msg", msg)
            .add("echo", echo);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
