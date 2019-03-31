/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.gql.GqlConstants;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

/**
 * <cos name="cos-name" id="cos-id"/>
 */
@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_COS, description="Cos objec with id and name")
public class Cos {

    /**
     * @zm-api-field-tag cos-id
     * @zm-api-field-description Class of Service (COS) ID
     */
    @XmlAttribute private String id;

    /**
     * @zm-api-field-tag cos-name
     * @zm-api-field-description Class of Service (COS) name
     */
    @XmlAttribute private String name;

    public Cos() {
    }

    @GraphQLQuery(name=GqlConstants.NAME, description="Name of the cos")
    public String getName() { return name; }
    @GraphQLQuery(name=GqlConstants.ID, description="Id of the cos")
    public String getId() { return id; }

    public Cos setName(String name) {
        this.name = name;
        return this;
    }

    public Cos setId(String id) {
        this.id = id;
        return this;
    }
}
