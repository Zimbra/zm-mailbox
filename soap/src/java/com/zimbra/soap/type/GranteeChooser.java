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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AdminConstants;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_GRANTEE_SELECTOR, description="grantee selector")
public class GranteeChooser {

    //See ACL.stringToType for valid (case insensitive) grantee types
    /**
     * @zm-api-field-tag grantee-type
     * @zm-api-field-description If specified, filters the result by the specified grantee type.
     */
    @XmlAttribute(name=AdminConstants.A_TYPE, required=false)
    private final String type;
    /**
     * @zm-api-field-tag grantee-id
     * @zm-api-field-description If specified, filters the result by the specified grantee ID.
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=false)
    private final String id;
    /**
     * @zm-api-field-tag grantee-name
     * @zm-api-field-description If specified, filters the result by the specified grantee name.
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=false)
    private final String name;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GranteeChooser() {
        this(null, null, null);
    }

    public GranteeChooser(@GraphQLInputField(name=GqlConstants.TYPE, description="grantee type") String type,
            @GraphQLInputField(name=GqlConstants.ID, description="grantee id") String id,
            @GraphQLInputField(name=GqlConstants.NAME, description="grantee name") String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }

    @GraphQLIgnore
    public static GranteeChooser createForId(String id) {
        return new GranteeChooser(null, id, null);
    }

    public String getType() { return type; }
    public String getId() { return id; }
    public String getName() { return name; }
}
