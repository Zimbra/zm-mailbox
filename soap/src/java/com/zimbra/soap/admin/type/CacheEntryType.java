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
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * @param commaSepList - comma separated list of cache entry types.  Note that any unknown ones (which should
     *                   be handled by a registered extension) will be ignored.
     */
    public static List<CacheEntryType> toListOfKnownCacheEntryTypes(String commaSepList) {
        String[] types = Strings.nullToEmpty(commaSepList).split(",");
        List<CacheEntryType> entryTypes = new ArrayList<>(types.length);
        for (String type : types) {
            try {
                entryTypes.add(CacheEntryType.fromString(type));
            } catch (ServiceException e) {
            }
        }
        return entryTypes;
    }

    /**
     * @param commaSepList - comma separated list of cache entry types.
     * @return unknown cache entry types (which might be handled by a registered extension)
     */
    public static List<String> toListOfUnknownCacheEntryTypes(String commaSepList) {
        String[] types = Strings.nullToEmpty(commaSepList).split(",");
        List<String> entryTypes = new ArrayList<>(types.length);
        for (String type : types) {
            try {
                CacheEntryType.valueOf(type);
            } catch (IllegalArgumentException e) {
                entryTypes.add(type);
            }
        }
        return entryTypes;
    }
}
