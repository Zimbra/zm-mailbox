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
package com.zimbra.common.util;

import java.util.UUID;

// just a wrapper to call UUID.randomUUID()
// so we can use another UUID generator if we want to (highly unlikely though)
public class UUIDUtil {

    /**
     * Returns a new UUID.
     * @return
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static String generateAccountAndTimeBasedUUID(String accountId) {
        return UUID.nameUUIDFromBytes((accountId + System.currentTimeMillis()).getBytes()).toString();
    }
}
