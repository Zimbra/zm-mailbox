/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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
package com.zimbra.cs.listeners;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.listeners.ListenerUtil.Priority;

public abstract class AuthListener {

    private static Map<String, AuthListenerEntry> mListeners = Collections
        .synchronizedMap(new HashMap<String, AuthListenerEntry>());

    public static void registerListener(String listenerName, Priority priority,
        AuthListener listener) throws ServiceException {
        if (mListeners.size() == LC.zimbra_listeners_maxsize.intValue()) {
            ZimbraLog.account.warn("Auth listener limit reached, registering of %s is ignored",
                listenerName);
            throw ServiceException.FORBIDDEN("Auth listener limit reached");
        }
        AuthListenerEntry obj = mListeners.get(listenerName);
        if (obj != null) {
            ZimbraLog.account.warn(
                "listener %s is already registered, registering of %s is ignored", listenerName,
                obj.getAuthListener().getClass().getCanonicalName());
            throw ServiceException.FORBIDDEN("Auth listener with this name is already registered");
        }
        ZimbraLog.account.info("Registering Auth listener: %s", listenerName);
        AuthListenerEntry entry = new AuthListenerEntry(listenerName, priority, listener);
        mListeners.put(listenerName, entry);

    }

    public static void deregisterListener(String listenerName) {
        ZimbraLog.account.info("De-registering Auth listener: %s", listenerName);
        mListeners.remove(listenerName);
    }

    public static void invokeOnSuccess(Account acct) throws ServiceException {
        ZimbraLog.account.info("Authentication successful for user: %s", acct.getName());
        // invoke listeners
        Map<String, AuthListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, AuthListenerEntry> listener : sortedListeners.entrySet()) {
            AuthListenerEntry listenerInstance = listener.getValue();
            listenerInstance.getAuthListener().onSuccess(acct);
        }

    }

    public static void invokeOnException(ServiceException exceptionThrown) {
        if (exceptionThrown instanceof AuthFailedServiceException) {
            ZimbraLog.account.info("Error occurred during authentication: %s. Reason: %s.",
                exceptionThrown.getMessage(),
                ((AuthFailedServiceException) exceptionThrown).getReason());
        } else {
            ZimbraLog.account.info("Error occurred during authentication: %s",
                exceptionThrown.getMessage());
        }
        if (ZimbraLog.account.isDebugEnabled()) {
            ZimbraLog.account.debug(exceptionThrown);
        }
        // invoke listeners
        Map<String, AuthListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, AuthListenerEntry> listener : sortedListeners.entrySet()) {
            AuthListenerEntry listenerInstance = listener.getValue();
            listenerInstance.getAuthListener().onException(exceptionThrown);
        }

    }

    /**
     * called after a successful authentication. should not throw any
     * exceptions.
     *
     * @param USER_ACCOUNT authenticated user account
     */
    public abstract void onSuccess(Account acct);

    /**
     * called when auth throws ServiceException.
     *
     * @param USER_ACCOUNT authenticated user account
     * @param ServiceException original exception thrown by auth
     *            
     */
    public abstract void onException(ServiceException exceptionThrown);

}