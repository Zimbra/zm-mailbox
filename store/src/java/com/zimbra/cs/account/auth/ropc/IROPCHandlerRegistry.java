/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.auth.ropc;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IROPCHandlerRegistry {

    private static final Map<String, IRopcHandler> HANDLERS = new ConcurrentHashMap<>();

    private IROPCHandlerRegistry() {
        // Prevent instantiation.
    }

    static {
        register(new OktaRopcHandler());
        ZimbraLog.account.info("ROPC provider registry initialized with default providers");
    }

    public static void register(IRopcHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("ROPC provider must not be null");
        }
        String key = handler.getName().toLowerCase();
        IRopcHandler existing = HANDLERS.putIfAbsent(key, handler);
        if (existing != null) {
            ZimbraLog.account.warn("ROPC provider already registered for: %s, skipping duplicate", key);
        } else {
            ZimbraLog.account.info("ROPC provider registered: %s", key);
        }
    }

    public static IRopcHandler get(String name) throws ServiceException {
        if (name == null || name.trim().isEmpty()) {
            throw ServiceException.INVALID_REQUEST("ROPC provider type/name must not be null or empty", null);
        }
        String key = name.toLowerCase();
        IRopcHandler handler = HANDLERS.get(key);
        if (handler == null) {
            throw ServiceException.INVALID_REQUEST("No ROPC provider registered for type: " + name, null);
        }
        ZimbraLog.account.debug("Resolved ROPC provider type: %s", handler.getName());
        return handler;
    }
}
