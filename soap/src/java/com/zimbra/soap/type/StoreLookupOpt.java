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
@GraphQLType(name=GqlConstants.CLASS_STORE_LOOKUP_OPT_TYPE, description="Store Lookup option type")
public enum StoreLookupOpt {
    // case must match protocol
    @GraphQLEnumValue(description = "While iterating through stores, stop if any certs are found in a store and"
            + " just return those certs - remaining stores will not be attempted.") ANY, 
    @GraphQLEnumValue(description = "Always iterate through all specified stores") ALL;

    public static StoreLookupOpt fromString(String s)
    throws ServiceException {
        try {
            return StoreLookupOpt.valueOf(s);
        } catch (IllegalArgumentException e) {
           throw ServiceException.INVALID_REQUEST(
                   "unknown 'StoreLookupOpt' key: " + s + ", valid values: " +
                   Arrays.asList(StoreLookupOpt.values()), null);
        }
    }
}