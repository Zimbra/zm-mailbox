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

package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.admin.type.CacheEntryType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Callback for the zimbraPushNotificationPayloadMode attribute.
 * Ensures the attribute can only be modified at the global config level.
 */
public class MobileNotificationPayload extends AttributeCallback {

    private static final String ATTR_NAME = "zimbraPushNotificationPayloadMode";

    private final Set<CallbackContext> flushedContexts = new HashSet<>();

    /**
     * Validates that zimbraPushNotificationPayloadMode can only be modified at global config level.
     *
     * @param context the callback context
     * @param attrName the attribute name
     * @param value the new value
     * @param attrsToModify the map of attributes to modify
     * @param entry the entry being modified
     * @throws ServiceException if modification is attempted at domain, account, or COS level
     */
    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) throws ServiceException {
        // Check if trying to create a non-config entry type
        Class<? extends Entry> creatingEntryType = context.getCreatingEntryType();
        if (creatingEntryType != null && !(creatingEntryType == Config.class)) {
            throw ServiceException.PERM_DENIED(
                    "zimbraPushNotificationPayloadMode can only be configured at global config level");
        }

        // Only allow modifications to global config entry
        if (entry != null && !(entry instanceof Config)) {
            throw ServiceException.PERM_DENIED(
                    "zimbraPushNotificationPayloadMode can only be configured at global config level");
        }
    }

    /**
     * Flushes the config cache after modification to ensure changes are propagated.
     *
     * @param context the callback context
     * @param attrName the attribute name
     * @param entry the entry that was modified
     */
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        // Only process config entries
        if (!(entry instanceof Config)) {
            return;
        }

        // Only flush once per context to avoid redundant operations
        if (flushedContexts.contains(context)) {
            return;
        }

        flushedContexts.add(context);
        try {
            flushCache(CacheEntryType.config, null);
        } catch (ServiceException e) {
            // Log error but don't throw - postModify should not throw exceptions
        }
    }

    /**
     * Flushes the cache entry. This method can be overridden for testing.
     *
     * @param type the cache entry type
     * @param entries the cache entries to flush (null for all)
     * @throws ServiceException if cache flush fails
     */
    protected void flushCache(CacheEntryType type, Provisioning.CacheEntry[] entries)
            throws ServiceException {
        Provisioning.getInstance().flushCache(type, entries);
    }
}
