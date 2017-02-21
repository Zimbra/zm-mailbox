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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlEnum;

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;

// TODO: Use this in ZimbraServer code instead of Provisioning.CacheEntryType
@XmlEnum
public enum CacheEntryType {
    // non ldap entries
    acl,
    locale,
    skin,
    uistrings,
    license,

    // ldap entries
    all,  // all ldap entries
    account,
    config,
    globalgrant,
    cos,
    domain,
    galgroup,
    group,
    mime,
    server,
    alwaysOnCluster,
    zimlet;

    private static Joiner PIPE_JOINER = Joiner.on("|");

    public static CacheEntryType fromString(String s) throws ServiceException {
        try {
            return CacheEntryType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("unknown cache type: "+s, e);
        }
    }

    public static String names() {
        return PIPE_JOINER.join(CacheEntryType.values());
    }
}
