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
import com.zimbra.cs.listeners.ListenerUtil.Priority;

public abstract class AccountListener {

    private static Map<String, AccountListenerEntry> mListeners = Collections
        .synchronizedMap(new HashMap<String, AccountListenerEntry>());

    public static void registerListener(String listenerName, Priority priority,
        AccountListener listener) throws ServiceException {
        if (mListeners.size() == LC.zimbra_listeners_maxsize.intValue()) {
            ZimbraLog.account.warn("Account listener limit reached, registering of %s is ignored",
                listenerName);
            throw ServiceException.FORBIDDEN("Account listener limit reached");
        }
        AccountListenerEntry obj = mListeners.get(listenerName);
        if (obj != null) {
            ZimbraLog.account.warn(
                "Listener %s is already registered, registering of %s is ignored", listenerName,
                obj.getAccountListener().getClass().getCanonicalName());
            throw ServiceException.FORBIDDEN("Account listener with this name is already registered");
        }
        ZimbraLog.account.info("Registering account listener: %s", listenerName);
        AccountListenerEntry entry = new AccountListenerEntry(listenerName, priority, listener);

        mListeners.put(listenerName, entry);
    }

    public static void deregisterListener(String listenerName) {
        ZimbraLog.account.info("De-registering account listener: %s", listenerName);
        mListeners.remove(listenerName);
    }

    public static void invokeOnAccountCreation(Account acct) throws ServiceException {
        ZimbraLog.account.info("Account creation successful for user: %s", acct.getName());
        // invoke listeners
        Map<String, AccountListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, AccountListenerEntry> listener : sortedListeners.entrySet()) {
            AccountListenerEntry listenerInstance = listener.getValue();
            listenerInstance.getAccountListener().onAccountCreation(acct);
        }
    }

    public static void invokeOnStatusChange(Account acct, String oldStatus, String newStatus)
        throws ServiceException {
        ZimbraLog.account.info("Account status for %s changed from '%s' to '%s'", acct.getName(),
            oldStatus, newStatus);
        // invoke listeners
        Map<String, AccountListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, AccountListenerEntry> listener : sortedListeners.entrySet()) {
            AccountListenerEntry listenerInstance = listener.getValue();
            listenerInstance.getAccountListener().onStatusChange(acct, oldStatus, newStatus);
        }

    }

    public static void invokeOnException(ServiceException exceptionThrown) {
        ZimbraLog.account.info("Error occurred during account creation: %s",
            exceptionThrown.getMessage());
        if (ZimbraLog.account.isDebugEnabled()) {
            ZimbraLog.account.debug(exceptionThrown);
        }
        // invoke listeners
        Map<String, AccountListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, AccountListenerEntry> listener : sortedListeners.entrySet()) {
            AccountListenerEntry listenerInstance = listener.getValue();
            listenerInstance.getAccountListener().onException(exceptionThrown);
        }

    }

    /**
     * called after a successful account creation. should not throw any
     * exceptions.
     *
     * @param USER_ACCOUNT user account created
     */
    public abstract void onAccountCreation(Account acct);

    /**
     * called after an account status change. should not throw any exceptions.
     *
     * @param USER_ACCOUNT user account whose status is changed
     * @param OLD_STATUS old status of user account
     * @param NEW_STATUS new status of user account
     */
    public abstract void onStatusChange(Account acct, String oldStatus, String newStatus);

    /**
     * called when CreateAccount throws ServiceException.
     *
     * @param USER_ACCOUNT user account
     * @param ServiceException original exception thrown by CreateAccount
     */
    public abstract void onException(ServiceException exceptionThrown);

}
