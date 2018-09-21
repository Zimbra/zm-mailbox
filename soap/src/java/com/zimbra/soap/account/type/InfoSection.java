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

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.service.ServiceException;

import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlEnum
@GraphQLType(name=GqlConstants.ENUM_INFO_SECTION)
public enum InfoSection {
    // mbox,prefs,attrs,zimlets,props,idents,sigs,dsrcs,children
    @GraphQLEnumValue @XmlEnumValue("mbox") mbox,
    @GraphQLEnumValue @XmlEnumValue("prefs") prefs,
    @GraphQLEnumValue @XmlEnumValue("attrs") attrs,
    @GraphQLEnumValue @XmlEnumValue("zimlets") zimlets,
    @GraphQLEnumValue @XmlEnumValue("props") props,
    @GraphQLEnumValue @XmlEnumValue("idents") idents,
    @GraphQLEnumValue @XmlEnumValue("sigs") sigs,
    @GraphQLEnumValue @XmlEnumValue("dsrcs") dsrcs,
    @GraphQLEnumValue @XmlEnumValue("children") children;

    public static InfoSection fromString(String s) throws ServiceException {
        try {
            return InfoSection.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("invalid sortBy: "+s+", valid values: "+Arrays.asList(InfoSection.values()), e);
        }
    }
}
