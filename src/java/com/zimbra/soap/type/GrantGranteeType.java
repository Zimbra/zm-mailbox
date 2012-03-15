/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.type;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;

@XmlEnum
public enum GrantGranteeType {
    // case must match protocol - keep in sync with com.zimbra.client.ZGrant.GranteeType
    //     (which differs slightly from com.zimbra.cs.account.accesscontrol.GranteeType)
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
