/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.mailbox;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;

@XmlEnum
public enum GrantGranteeType {
    // case must match protocol - keep in sync with com.zimbra.client.ZGrant.GranteeType and
    // com.zimbra.soap.type.GrantGranteeType
    //     (which differ slightly from com.zimbra.cs.account.accesscontrol.GranteeType)
        /**
         * access is granted to an authenticated user
         */
        usr,
        /**
         * access is granted to a group of users
         */
        grp,
        /**
         * access is granted to users on a cos
         */
        cos,
        /**
         * access is granted to public. no authentication needed.
         */
        pub,
        /**
         * access is granted to all authenticated users
         */
        all,
        /**
         * access is granted to all users in a domain
         */
        dom,
        /**
         * access is granted to a non-Zimbra email address and a password
         */
        guest,
        /**
         * access is granted to a non-Zimbra email address and an accesskey
         */
        key;

    public static GrantGranteeType fromString(String s) throws ServiceException {
        try {
            return GrantGranteeType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("Invalid grantee type: " + s + ", valid values: " +
                    Arrays.asList(GrantGranteeType.values()), null);
        }
    }
}
