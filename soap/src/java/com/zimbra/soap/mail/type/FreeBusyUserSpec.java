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

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_FREE_BUSY_USER_SPEC, description="Free busy user request information")
public class FreeBusyUserSpec {

    /**
     * @zm-api-field-tag calendar-folder-id
     * @zm-api-field-description Calendar folder ID; if omitted, get f/b on all calendar folders
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private Integer folderId;

    /**
     * @zm-api-field-tag zimbra-id
     * @zm-api-field-description Zimbra ID Either "name" or "id" must be specified
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag email
     * @zm-api-field-description Email address.  Either "name" or "id" must be specified
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    public FreeBusyUserSpec() {
    }

    @GraphQLInputField(name=GqlConstants.FOLDER_ID, description="Calendar folder ID; if omitted, get f/b on all calendar folders")
    public void setFolderId(Integer folderId) { this.folderId = folderId; }
    @GraphQLInputField(name=GqlConstants.ID, description="Zimbra ID Either name or id must be specified")
    public void setId(String id) { this.id = id; }
    @GraphQLInputField(name=GqlConstants.NAME, description="Email address.  Either name or id must be specified")
    public void setName(String name) { this.name = name; }
    public Integer getFolderId() { return folderId; }
    public String getId() { return id; }
    public String getName() { return name; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("folderId", folderId)
            .add("id", id)
            .add("name", name);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
