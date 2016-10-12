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
    //         (Note:differs slightly from com.zimbra.cs.account.accesscontrol.GranteeType)
        usr((byte)1),     /** access is granted to an authenticated user */
        grp((byte)2),     /** access is granted to a group of users */
        all((byte)3),     /** access is granted to all authenticated users */
        dom((byte)4),     /** access is granted to all users in a domain */
        cos((byte)5),     /** access is granted to users on a cos */
        pub((byte)6),     /** access is granted to public. no authentication needed. */
        guest((byte)7),   /** access is granted to a non-Zimbra email address and a password */
        key((byte)8);     /** access is granted to a non-Zimbra email address and an accesskey */

        private byte byteEquiv;

        GrantGranteeType(byte asByte) {
            byteEquiv = asByte;
        }

        /**
         * @return zm-store ACL byte constant value equivalent
         *         usr     --> GRANTEE_USER
         *         grp     --> GRANTEE_GROUP
         *         all     --> GRANTEE_AUTHUSER
         *         dom     --> GRANTEE_DOMAIN
         *         cos     --> GRANTEE_COS
         *         pub     --> GRANTEE_PUBLIC
         *         guest   --> GRANTEE_GUEST
         *         key     --> GRANTEE_KEY
         */
        public byte asByte() {
            return byteEquiv;
        }

        public static GrantGranteeType fromByte(byte byteValue) {
            for (GrantGranteeType gt :GrantGranteeType.values()) {
                if (gt.asByte() == byteValue) {
                    return gt;
                }
            }
            throw new IllegalArgumentException("Unrecognised GranteeType byte equvalent :" + byteValue);
        }

    public static GrantGranteeType fromString(String s) throws ServiceException {
        try {
            return GrantGranteeType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("Invalid grantee type: " + s + ", valid values: " +
                    Arrays.asList(GrantGranteeType.values()), null);
        }
    }
}
