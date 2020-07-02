/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

/*
     <identity name={identity-name} id="...">
       <a name="{name}">{value}</a>
       ...
       <a name="{name}">{value}</a>
     </identity>*

 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"name", "id"})
@GraphQLType(name=GqlConstants.IDENTITY, description="Identity")
public class Identity extends AttrsImpl {

    // TODO:Want constructor for old style Identity

    /**
     * @zm-api-field-tag identity-name
     * @zm-api-field-description Identity name
     */
    @XmlAttribute(name=AccountConstants.A_NAME, required=false)
    private final String name;

    /**
     * @zm-api-field-tag identity-id
     * @zm-api-field-description Identity ID
     */
    @XmlAttribute(name=AccountConstants.A_ID, required=false)
    private String id;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private Identity() {
        this((String) null, (String) null);
    }

    public Identity(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public static Identity fromName(String name) {
        return new Identity(name, null);
    }

    public static Identity fromNameAndId(String name, String id) {
        return new Identity(name, id);
    }
    public Identity(Identity i) {
        name = i.getName();
        id = i.getId();
        super.setAttrs(Lists.transform(i.getAttrs(), Attr.COPY));
    }

    public void setId(String id) { this.id = id; }

    @GraphQLQuery(name=GqlConstants.NAME, description="Identity name")
    public String getName() { return name; }
    @GraphQLQuery(name=GqlConstants.ID, description="Identity ID")
    public String getId() { return id; }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("name", name)
            .add("id", id);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }

}
