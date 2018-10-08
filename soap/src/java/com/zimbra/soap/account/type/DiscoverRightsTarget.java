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
package com.zimbra.soap.account.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.TargetType;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_DISCOVER_RIGHTS_TARGET, description="Discover rights target")
public class DiscoverRightsTarget {

    /**
     * @zm-api-field-tag target-type
     * @zm-api-field-description Target type
     */
    @XmlAttribute(name=AccountConstants.A_TYPE /* type */, required=true)
    private TargetType type;

    /**
     * @zm-api-field-tag target-id
     * @zm-api-field-description Target ID
     */
    @XmlAttribute(name=AccountConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag target-name
     * @zm-api-field-description Target name
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag target-display-name
     * @zm-api-field-description If a discovered target is an account or a group and the entry has a display name set
     * then this is set to that display name.
     */
    @XmlAttribute(name=AccountConstants.A_DISPLAY /* d */, required=false)
    private String displayName;

    /**
     * @zm-api-field-description Email addresses
     */
    @XmlElement(name=AccountConstants.E_EMAIL /* email */, required=false)
    private List<DiscoverRightsEmail> emails;

    public DiscoverRightsTarget() {
        this(null, null, null, null);
    }

    public DiscoverRightsTarget(TargetType type) {
        this(type, null, null, null);
    }

    public DiscoverRightsTarget(TargetType type, String id, String name, String displayName) {
        setType(type);
        setId(id);
        setName(name);
        setDisplayName(displayName);
    }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.TYPE, description="Target type")
    public TargetType getType() { return type; }
    @GraphQLQuery(name=GqlConstants.NAME, description="Target name")
    public String getName() { return name; }
    @GraphQLQuery(name=GqlConstants.DISPLAY_NAME, description="Display name of the entry")
    public String getDisplayName() { return displayName; }
    @GraphQLQuery(name=GqlConstants.ID, description="Target ID")
    public String getId() { return id; }
    @GraphQLQuery(name=GqlConstants.EMAILS, description="Email addresses")
    public List<DiscoverRightsEmail> getAddrs() { return emails; }

    public void setType(TargetType type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmails(Iterable<DiscoverRightsEmail> emails) {
        this.emails = Lists.newArrayList(emails);
    }

    public void addEmail(DiscoverRightsEmail email) {
        if (emails == null) {
            emails = Lists.newArrayList();
        }
        emails.add(email);
    }
}
