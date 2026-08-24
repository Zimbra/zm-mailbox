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
import com.zimbra.cs.account.auth.ropc.okta.OktaRopcHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * registry for ROPC (Resource Owner Password Credentials) handler implementations.
 * maintains a map of provider name to {@link IRopcHandler} and provides
 * static methods to register and retrieve handlers.
 * initialized with {@link OktaRopcHandler} by default.
 */
public final class IROPCHandlerRegistry {

    private static final Map<String, IRopcHandler> HANDLERS = new ConcurrentHashMap<>();

    private IROPCHandlerRegistry() {
        // Prevent instantiation.
    }

    static {
        register(new OktaRopcHandler());
        ZimbraLog.account.debug("ROPC provider registry initialized with default providers");
    }


    /**
     * registers an {@link IRopcHandler} in the registry.
     * duplicate registrations for the same provider name are silently skipped.
     *
     * @param handler the handler to register; must not be {@code null}
     * @throws IllegalArgumentException if {@code handler} is {@code null}
     */
    public static void register(IRopcHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("ROPC provider must not be null");
        }
        String key = handler.getName().toLowerCase();
        IRopcHandler existing = HANDLERS.putIfAbsent(key, handler);
        if (existing != null) {
            ZimbraLog.account.debug("ROPC provider already registered for: %s, skipping duplicate", key);
        } else {
            ZimbraLog.account.info("ROPC provider registered: %s", key);
        }
    }

    /**
     * retrieves the {@link IRopcHandler} registered for the given provider name.
     *
     * @param name the provider name; must not be {@code null} or empty
     * @return the matching {@link IRopcHandler}
     * @throws ServiceException if the name is null/empty or no handler is registered for it
     */
    public static IRopcHandler get(String name) throws ServiceException {
        if (name == null || name.trim().isEmpty()) {
            throw ServiceException.INVALID_REQUEST("ROPC provider type/name must not be null or empty", null);
        }
        String key = name.toLowerCase();
        IRopcHandler handler = HANDLERS.get(key);
        if (handler == null) {
            throw ServiceException.INVALID_REQUEST("No ROPC handler registered for type: " + name, null);
        }
        ZimbraLog.account.debug("Resolved ROPC provider type: %s", handler.getName());
        return handler;
    }
}
