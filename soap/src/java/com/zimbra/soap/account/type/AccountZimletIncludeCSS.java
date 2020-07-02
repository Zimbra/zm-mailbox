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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.ZimletConstants;
import com.zimbra.soap.base.ZimletIncludeCSS;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

/**
 * Implemented as an object rather than using String with @XmlElement because when constructing a JAXB
 * object containing this and other "Strings" there needs to be a way of differentiating them when
 * marshaling to XML.
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=ZimletConstants.ZIMLET_TAG_CSS)
@GraphQLType(name=GqlConstants.CLASS_ACCOUNT_ZIMLET_INCLUDE_CSS, description="Account zimlet include css")
public class AccountZimletIncludeCSS
implements ZimletIncludeCSS {

    /**
     * @zm-api-field-description Included Cascading Style Sheet (CSS)
     */
    @XmlValue
    private String value;

    @SuppressWarnings("unused")
    private AccountZimletIncludeCSS() { }

    public AccountZimletIncludeCSS(String value) { setValue(value); }

    @Override
    public void setValue(String value) { this.value = value; }
    @Override
    @GraphQLQuery(name=GqlConstants.VALUE, description="Value")
    public String getValue() { return value; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
