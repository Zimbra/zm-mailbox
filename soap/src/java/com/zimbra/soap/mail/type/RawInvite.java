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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_RAW_INVITE, description="The raw invitation")
public class RawInvite {

    /**
     * @zm-api-field-tag UID
     * @zm-api-field-description UID
     */
    @XmlAttribute(name=MailConstants.A_UID /* uid */, required=false)
    private String uid;

    /**
     * @zm-api-field-tag summary
     * @zm-api-field-description summary
     */
    @XmlAttribute(name=MailConstants.A_SUMMARY /* summary */, required=false)
    private String summary;

    /**
     * @zm-api-field-tag raw-icalendar
     * @zm-api-field-description Raw iCalendar data
     */
    @XmlValue
    private String content;

    public RawInvite() {
    }

    @GraphQLInputField(name=GqlConstants.UID, description="UID")
    public void setUid(String uid) { this.uid = uid; }
    @GraphQLInputField(name=GqlConstants.SUMMARY, description="Summary")
    public void setSummary(String summary) { this.summary = summary; }
    @GraphQLInputField(name=GqlConstants.CONTENT, description="Raw iCalendar data")
    public void setContent(String content) { this.content = content; }
    @GraphQLQuery(name=GqlConstants.UID, description="UID")
    public String getUid() { return uid; }
    @GraphQLQuery(name=GqlConstants.SUMMARY, description="Summary")
    public String getSummary() { return summary; }
    @GraphQLQuery(name=GqlConstants.CONTENT, description="Raw iCalendar data")
    public String getContent() { return content; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("uid", uid)
            .add("summary", summary)
            .add("content", content);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
