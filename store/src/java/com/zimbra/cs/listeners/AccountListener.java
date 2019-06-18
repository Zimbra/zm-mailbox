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
import java.util.EnumMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;

public abstract class AccountListener {

    // listeners will be invoked by the enum order
    public enum AccountListenerId {
        ACCTL_ZIMBRA;
    }

    /*
     * The collection's iterator will return the values in the order their
     * corresponding keys appear in map, which is their natural order (the order
     * in which the enum constants are declared).
     */
    private static Map<AccountListenerId, AccountListener> mListeners = Collections
        .synchronizedMap(
            new EnumMap<AccountListenerId, AccountListener>(AccountListenerId.class));

    public static void registerListener(AccountListenerId listenerEnum,
        AccountListener listener) {
        AccountListener obj = mListeners.get(listenerEnum);
        if (obj != null) {
            ZimbraLog.account.warn(
                "listener %s is already registered, registering of %s is ignored", listenerEnum,
                obj.getClass().getCanonicalName());
            return;
        }
        ZimbraLog.account.info("Registering Account listener: %s", listenerEnum.name());
        mListeners.put(listenerEnum, listener);
    }
    
    public static void deregisterListener(AccountListenerId listenerEnum) {
        ZimbraLog.account.info("De-registering Account listener: %s", listenerEnum.name());
        mListeners.remove(listenerEnum);
    }

    public static void invokeOnSuccess(Account acct) throws ServiceException {
        ZimbraLog.account.info("Account creation successful for user: %s", acct.getName());
        // invoke listeners
        for (Map.Entry<AccountListenerId, AccountListener> listener : mListeners
            .entrySet()) {
            AccountListener listenerInstance = listener.getValue();
            listenerInstance.onSuccess(acct);
        }

    }

    public static void invokeOnException(ServiceException exceptionThrown) {
        ZimbraLog.account.info("Error occurred during account creation: %s" , exceptionThrown.getMessage());
        if (ZimbraLog.account.isDebugEnabled()) {
            ZimbraLog.account.debug(exceptionThrown);
        }
        // invoke listeners
        for (Map.Entry<AccountListenerId, AccountListener> listener : mListeners
            .entrySet()) {
            AccountListener listenerInstance = listener.getValue();
            listenerInstance.onException(exceptionThrown);
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
     */
    public abstract void onException(ServiceException exceptionThrown);

}
