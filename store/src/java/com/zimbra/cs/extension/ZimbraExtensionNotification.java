/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

package com.zimbra.cs.extension;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

public abstract class ZimbraExtensionNotification {

    private static Map<String, ZimbraExtensionNotification> mHandlers;

    /*
     * Register a handler for a notification.
     * Only a single handler can be registered on each notification ID.
     *
     * It should be invoked from the init() method of ZimbraExtension.
     */
    public synchronized static final void register(String notificationId, ZimbraExtensionNotification handler) {
        if (mHandlers == null) {
            mHandlers = new HashMap<String, ZimbraExtensionNotification>();
        }
        ZimbraExtensionNotification obj = getHandler(notificationId);
        if (obj != null) {
            ZimbraLog.extensions.warn("handler for " + notificationId + " is already registered, " +
                                   "registering of " + obj.getClass().getCanonicalName() + " is ignored");
            return;
        }
        mHandlers.put(notificationId, handler);
    }

    public synchronized static final void unregister(String notificationId) {
        if (!StringUtil.isNullOrEmpty(notificationId)) {
            mHandlers.remove(notificationId);
        }
    }

    private static final ZimbraExtensionNotification getHandler(String notificationId) {
        if (mHandlers == null || StringUtil.isNullOrEmpty(notificationId)) {
            return null;
        } else {
            return mHandlers.get(notificationId);
        }
    }

    /*
     * It is responsible for an extension to make thread-safe process
     */
    public static final void notifyExtension(String notificationId, Object ... obj) throws ServiceException {
        ZimbraExtensionNotification handler = getHandler(notificationId);
        if (handler != null) {
            handler.execute(obj);
        }
    }

    /*
     * It is responsible for an extension to cast a parameter to a right class
     */
    public abstract void execute(Object[] args) throws ServiceException;
}