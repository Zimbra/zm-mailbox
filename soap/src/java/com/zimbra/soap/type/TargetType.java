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

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.service.ServiceException;

import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.types.GraphQLType;

/**
 * JAXB analog to {@com.zimbra.cs.account.accesscontrol.TargetType}
 */
@XmlEnum
@GraphQLType(name=GqlConstants.ENUM_TARGET_TYPE, description="")
public enum TargetType {
    // case must match protocol
    @GraphQLEnumValue
    account,
    @GraphQLEnumValue
    calresource,
    @GraphQLEnumValue
    cos,
    @GraphQLEnumValue
    dl,
    @GraphQLEnumValue
    group,
    @GraphQLEnumValue
    domain,
    @GraphQLEnumValue
    server,
    @GraphQLEnumValue
    alwaysoncluster,
    @GraphQLEnumValue
    ucservice,
    @GraphQLEnumValue
    xmppcomponent,
    @GraphQLEnumValue
    zimlet,
    @GraphQLEnumValue
    config,
    @GraphQLEnumValue
    global;

    public static TargetType fromString(String s)
    throws ServiceException {
        try {
            return TargetType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST(
                    "unknown 'TargetType' key: " + s + ", valid values: " +
                   Arrays.asList(TargetType.values()), null);
        }
    }
}
