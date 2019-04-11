/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.service.ServiceException;

import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.types.GraphQLType;

@GraphQLType(name=GqlConstants.CLASS_MEMBER_OF_SELECTOR, description="Criteria to decide when \"isMember\" is set for group")
public enum MemberOfSelector {
    @GraphQLEnumValue(description = "the isMember flag returned is set if the user is a direct or indirect member of the group, otherwise it is unset") all, 
    @GraphQLEnumValue(description = "the isMember flag returned is set if the user is a direct member of the group, otherwise it is unset") directOnly,
    @GraphQLEnumValue(description = "the isMember flag is not returned") none;

    public static MemberOfSelector fromString(String s) throws ServiceException {
        try {
            return MemberOfSelector.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("unknown NeedMemberOf: "+s, e);
        }
    }
}
