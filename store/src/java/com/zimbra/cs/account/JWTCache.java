/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.cs.account;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zimbra.common.localconfig.LC;

public class JWTCache {

    private static final Cache <String, ZimbraJWT> JWT_CACHE = CacheBuilder.newBuilder()
                                                                           .maximumSize(LC.zimbra_authtoken_cache_size.intValue())
                                                                           .build();

    public static void put(String jti, ZimbraJWT jwtInfo) {
            JWT_CACHE.put(jti, jwtInfo);
    }

    public static ZimbraJWT get(String jti) {
            return JWT_CACHE.getIfPresent(jti);
    }

    public static void remove(String jti) {
           JWT_CACHE.invalidate(jti);
    }
}