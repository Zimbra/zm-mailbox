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
        ZimbraLog.account.info("ROPC handler registry initialized with default providers");
    }

    public static void register(IRopcHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("ROPC handler must not be null");
        }
        String key = handler.getName().toLowerCase();
        IRopcHandler existing = HANDLERS.put(key, handler);
        if (existing != null) {
            ZimbraLog.account.warn("ROPC handler already registered for: %s", key);
        }
        ZimbraLog.account.info("ROPC handler registered: %s", key);
    }

    public static IRopcHandler get(String name) throws ServiceException {
        if (name == null || name.trim().isEmpty()) {
            throw ServiceException.INVALID_REQUEST("ROPC handler type/name must not be null or empty", null);
        }
        String key = name.toLowerCase();
        IRopcHandler handler = HANDLERS.get(key);
        if (handler == null) {
            throw ServiceException.INVALID_REQUEST("No ROPC handler registered for type: " + name, null);
        }
        ZimbraLog.account.debug("Resolved ROPC handler type: %s", handler.getName());
        return handler;
    }
}
