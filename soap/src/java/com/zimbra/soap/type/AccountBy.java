/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;

import io.leangen.graphql.annotations.GraphQLEnumValue;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlEnum
@GraphQLType(name="AccountBy")
public enum AccountBy {
    // case must match protocol
    @GraphQLEnumValue adminName,
    @GraphQLEnumValue appAdminName,
    @GraphQLEnumValue id,
    @GraphQLEnumValue foreignPrincipal,
    @GraphQLEnumValue name,
    @GraphQLEnumValue krb5Principal;

    public static AccountBy fromString(String s)
    throws ServiceException {
        try {
            return AccountBy.valueOf(s);
        } catch (final IllegalArgumentException e) {
           throw ServiceException.INVALID_REQUEST("unknown 'By' key: " + s + ", valid values: " +
                   Arrays.asList(AccountBy.values()), null);
        }
    }

    public com.zimbra.common.account.Key.AccountBy toKeyAccountBy()
    throws ServiceException {
        return com.zimbra.common.account.Key.AccountBy.fromString(this.name());
    }
}
