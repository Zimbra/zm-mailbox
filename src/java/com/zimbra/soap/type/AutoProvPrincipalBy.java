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
public enum AutoProvPrincipalBy {
    // case must match protocol
    dn,    // DN in external LDAP source
    name;  // name to be applied to the auto provision search or bind DN template configured on the domain

    static public AutoProvPrincipalBy fromString(String str)
    throws ServiceException {
        try {
            return AutoProvPrincipalBy.valueOf(str);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("Invalid AutoProvPrincipalBy: " + str +
                    ", valid values: " + Arrays.asList(AutoProvPrincipalBy.values()), null);
        }
    }
};
