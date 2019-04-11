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

@XmlEnum
@GraphQLType(name=GqlConstants.CLASS_SOURCE_LOOKUP_OPT_TYPE, description="Source Lookup option type")
public enum SourceLookupOpt {
    // case must match protocol
    @GraphQLEnumValue(description = "While iterating through multiple sources configured for a store, stop if any "
            + "certificates are found in one source - remaining configured sources will not be attempted.") ANY,
    @GraphQLEnumValue(description = "Always iterate through all configured sources") ALL;

    public static SourceLookupOpt fromString(String s)
    throws ServiceException {
        try {
            return SourceLookupOpt.valueOf(s);
        } catch (IllegalArgumentException e) {
           throw ServiceException.INVALID_REQUEST(
                   "unknown 'SourceLookupOpt' key: " + s + ", valid values: " +
                   Arrays.asList(SourceLookupOpt.values()), null);
        }
    }
}
