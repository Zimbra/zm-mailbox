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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.listeners.ListenerUtil.Priority;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class AccountListener {
    private static final String SMX_VIA_HEADER = "smx";
    private static final String IDBRIDGE_VIA_HEADER = "idbridge";

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

    public static void invokeBeforeAccountCreation(String email, Map<String, Object> attrs) throws ServiceException {
        ZimbraLog.account.debug("Invoking listeners before account creation %s", email);
        // invoke listeners
        Map<String, AccountListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, AccountListenerEntry> listener : sortedListeners.entrySet()) {
            AccountListenerEntry listenerInstance = listener.getValue();
            listenerInstance.getAccountListener().beforeAccountCreation(email, attrs);
        }
    }

    public static void invokeOnAccountCreation(Account acct) throws ServiceException {
        ZimbraLog.account.debug("Account creation successful for user: %s", acct.getName());
        // invoke listeners
        Map<String, AccountListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, AccountListenerEntry> listener : sortedListeners.entrySet()) {
            AccountListenerEntry listenerInstance = listener.getValue();
            listenerInstance.getAccountListener().onAccountCreation(acct);
        }
    }

    public static void invokeOnAccountCreation(Account acct, ZimbraSoapContext zsc) throws ServiceException {
        ZimbraLog.account.debug("Account creation successful for user: %s", acct.getName());
        String requestVia = zsc.getVia();
        // invoke listeners
        Map<String, AccountListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, AccountListenerEntry> listener : sortedListeners.entrySet()) {
            AccountListenerEntry listenerInstance = listener.getValue();
            if (!notifyCaller(requestVia, listenerInstance.getListenerName())) {
                ZimbraLog.account.debug("Account creation request received from \"%s\", no need to call \"%s\".", requestVia,
                        listenerInstance.getListenerName());
                continue;
            }
            listenerInstance.getAccountListener().onAccountCreation(acct);
        }
    }

    public static void invokeBeforeAccountDeletion(Account acct, ZimbraSoapContext zsc, boolean rollbackOnFailure) throws ServiceException {
        ZimbraLog.account.debug("Account to be deleted for user: %s", acct.getName());
        ArrayList<AccountListenerEntry> notifiedListeners = new ArrayList<AccountListenerEntry>();
        String requestVia = zsc.getVia();
        try {
            // invoke listeners
            Map<String, AccountListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
            for (Map.Entry<String, AccountListenerEntry> listener : sortedListeners.entrySet()) {
                AccountListenerEntry listenerInstance = listener.getValue();
                if (!notifyCaller(requestVia, listenerInstance.getListenerName())) {
                    ZimbraLog.account.debug("Account deletion request received from \"%s\", no need to call \"%s\".", requestVia,
                            listenerInstance.getListenerName());
                    continue;
                }
                listenerInstance.getAccountListener().beforeAccountDeletion(acct);
                notifiedListeners.add(listenerInstance);
            }
        } catch (ServiceException se) {
            if (rollbackOnFailure) {
                rollbackAccountDeletion(notifiedListeners, acct);
            } else {
                ZimbraLog.account.warn("No rollback on account listener for zimbra delete account failure, there may be inconsistency in account. %s", se.getMessage());
            }
            throw se;
        }
    }

    public static void invokeOnStatusChange(Account acct, String oldStatus, String newStatus, ZimbraSoapContext zsc, boolean rollbackOnFailure)
            throws ServiceException {
        ZimbraLog.account.debug("Account status for %s changed from '%s' to '%s'", acct.getName(),
            oldStatus, newStatus);
        ArrayList<AccountListenerEntry> notifiedListeners = new ArrayList<AccountListenerEntry>();
        String requestVia = zsc.getVia();
        try {
            // invoke listeners
            Map<String, AccountListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
            for (Map.Entry<String, AccountListenerEntry> listener : sortedListeners.entrySet()) {
                AccountListenerEntry listenerInstance = listener.getValue();
                if (!notifyCaller(requestVia, listenerInstance.getListenerName())) {
                    ZimbraLog.account.debug("Account status change request received from \"%s\", no need to call \"%s\".", requestVia,
                            listenerInstance.getListenerName());
                    continue;
                }
                listenerInstance.getAccountListener().onStatusChange(acct, oldStatus, newStatus);
                notifiedListeners.add(listenerInstance);
            }
        } catch (ServiceException se) {
            if (rollbackOnFailure) {
                rollbackChangedStatus(notifiedListeners, acct, oldStatus, newStatus);
            } else {
                ZimbraLog.account.warn("No rollback on account listener for zimbra modify account failure, there may be inconsistency in account. %s", se.getMessage());
            }
            throw se;
        }
    }

    public static void invokeOnNameChange(Account acct, String firstName, String lastname, ZimbraSoapContext zsc, boolean rollbackOnFailure)
            throws ServiceException {
        ZimbraLog.account.debug("Account name changed for %s. First Name: %s, Last Name: %s", acct.getName(),
                firstName, lastname);
        ArrayList<AccountListenerEntry> notifiedListeners = new ArrayList<AccountListenerEntry>();
        String requestVia = zsc.getVia();
        try {
            // invoke listeners
            Map<String, AccountListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
            for (Map.Entry<String, AccountListenerEntry> listener : sortedListeners.entrySet()) {
                AccountListenerEntry listenerInstance = listener.getValue();
                if (!notifyCaller(requestVia, listenerInstance.getListenerName())) {
                    ZimbraLog.account.debug("Account name change request received from \"%s\", no need to call \"%s\".", requestVia,
                            listenerInstance.getListenerName());
                    continue;
                }
                listenerInstance.getAccountListener().onNameChange(acct, firstName, lastname);
                notifiedListeners.add(listenerInstance);
            }
        } catch (ServiceException se) {
            if (rollbackOnFailure) {
                rollbackNameChange(notifiedListeners, acct, acct.getGivenName(), acct.getSn());
            } else {
                ZimbraLog.account.warn("No rollback on account listener for zimbra account name change failure, there may be inconsistency in account %s", se.getMessage());
            }
            throw se;
        }
    }

    public static void invokeOnSANChange(Account acct, String serviceAcctNumber, ZimbraSoapContext zsc, boolean rollbackOnFailure) throws ServiceException {
        ZimbraLog.account.debug("Account SAN changed for %s. new SAN: %s", acct.getName(),
                serviceAcctNumber);
        ArrayList<AccountListenerEntry> notifiedListeners = new ArrayList<AccountListenerEntry>();
        String requestVia = zsc.getVia();
        try {
            // invoke listeners
            Map<String, AccountListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
            for (Map.Entry<String, AccountListenerEntry> listener : sortedListeners.entrySet()) {
                AccountListenerEntry listenerInstance = listener.getValue();
                if (!notifyCaller(requestVia, listenerInstance.getListenerName())) {
                    ZimbraLog.account.debug("Account SAN change request received from \"%s\", no need to call \"%s\".", requestVia,
                            listenerInstance.getListenerName());
                    continue;
                }
                listenerInstance.getAccountListener().onSANChange(acct, serviceAcctNumber);
                notifiedListeners.add(listenerInstance);
            }
        } catch (ServiceException se) {
            if (rollbackOnFailure) {
                String oldServiceAccountNumber = acct.getServiceAccountNumber();
                rollbackSANChange(notifiedListeners, acct, oldServiceAccountNumber);
            } else {
                ZimbraLog.account.warn("No rollback on account listener for zimbra account SAN update failure, there may be inconsistency in account. %s", se.getMessage());
            }
            throw se;
        }
    }

    public static void invokeOnException(ServiceException exceptionThrown) {
        ZimbraLog.account.debug("Error occurred during account creation: %s",
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
     * called before account deletion.
     *
     * @param USER_ACCOUNT user account to be deleted
     */
    public abstract void beforeAccountDeletion(Account acct) throws ServiceException;

    /**
     * called before a successful account creation.
     *
     * @param email - email address of account to be created
     * @param attrs - map of attributes to be applied on account
     */
    public abstract void beforeAccountCreation(String email, Map<String, Object> attrs) throws ServiceException;

    /**
     * called after a successful account creation.
     *
     * @param USER_ACCOUNT user account created
     */
    public abstract void onAccountCreation(Account acct) throws ServiceException;

    /**
     * called after an account status change.
     *
     * @param USER_ACCOUNT user account whose status is changed
     * @param OLD_STATUS old status of user account
     * @param NEW_STATUS new status of user account
     * @throws ServiceException
     */
    public abstract void onStatusChange(Account acct, String oldStatus, String newStatus) throws ServiceException;

    /**
     * called after an account name change.
     *
     * @param USER_ACCOUNT user account whose status is changed
     * @param FIRST_NAME new first name of user account
     * @param LAST_NAME new last name of user account
     * @throws ServiceException
     */
    public abstract void onNameChange(Account acct, String firstName, String lastName) throws ServiceException;

    /**
     * called on SAN(Service Account Number) update request
     *
     * @param USER_ACCOUNT user account whose SAN is to be updated
     * @param SERVICE_ACCOUNT_NUMBER new SAN to be updated
     */
    public abstract void onSANChange(Account acct, String serviceAcctNumber) throws ServiceException;

    /**
     * called when CreateAccount throws ServiceException.
     *
     * @param USER_ACCOUNT user account
     * @param ServiceException original exception thrown by CreateAccount
     */
    public abstract void onException(ServiceException exceptionThrown);

    private static boolean notifyCaller(String requestVia, String listenerName) {
        boolean notify = true;
        if (!StringUtil.isNullOrEmpty(requestVia) && !StringUtil.isNullOrEmpty(listenerName)) {
            if (requestVia.toLowerCase().contains(SMX_VIA_HEADER) && listenerName.toLowerCase().contains(SMX_VIA_HEADER)) {
                notify = false;
            }

            if (requestVia.toLowerCase().contains(IDBRIDGE_VIA_HEADER) && listenerName.toLowerCase().contains(IDBRIDGE_VIA_HEADER)) {
                notify = false;
            }
        }

        return notify;
    }

    private static void rollbackChangedStatus(ArrayList<AccountListenerEntry> notifiedListeners, Account acct, String oldStatus, String newStatus) {
        if (!notifiedListeners.isEmpty()) {
            for(int i = 0; i < notifiedListeners.size(); i++)
            {
                AccountListenerEntry listenerInstance = notifiedListeners.get(i);
                ZimbraLog.account.debug("Rollback account listener status change request from \"%s\".", listenerInstance.getListenerName());
                try {
                    listenerInstance.getAccountListener().onStatusChange(acct, newStatus, oldStatus);
                } catch (ServiceException se) {
                    ZimbraLog.account.debug("Rollback account listener status change failed from \"%s\". %s", listenerInstance.getListenerName(), se.getMessage());
                }
            }
        }
    }

    private static void rollbackNameChange(ArrayList<AccountListenerEntry> notifiedListeners, Account acct, String firstName, String lastName) {
        if (!notifiedListeners.isEmpty()) {
            for(int i = 0; i < notifiedListeners.size(); i++)
            {
                AccountListenerEntry listenerInstance = notifiedListeners.get(i);
                ZimbraLog.account.debug("Rollback account listener name change request from \"%s\".", listenerInstance.getListenerName());
                try {
                    listenerInstance.getAccountListener().onNameChange(acct, firstName, lastName);
                } catch (ServiceException se) {
                    ZimbraLog.account.debug("Rollback account listener name change failed from \"%s\". %s", listenerInstance.getListenerName(), se.getMessage());
                }
            }
        }
    }

    private static void rollbackAccountDeletion(ArrayList<AccountListenerEntry> notifiedListeners, Account acct) {
        if (!notifiedListeners.isEmpty()) {
            for(int i = 0; i < notifiedListeners.size(); i++)
            {
                AccountListenerEntry listenerInstance = notifiedListeners.get(i);
                ZimbraLog.account.debug("Rollback account listener delete request from \"%s\".", listenerInstance.getListenerName());
                try {
                    listenerInstance.getAccountListener().onAccountCreation(acct);
                } catch (ServiceException se) {
                    ZimbraLog.account.debug("Rollback account listener delete failed from \"%s\". %s", listenerInstance.getListenerName(), se.getMessage());
                }
            }
        }
    }

    private static void rollbackSANChange(ArrayList<AccountListenerEntry> notifiedListeners, Account acct,
            String oldServiceAccountNumber) {
        if (!notifiedListeners.isEmpty()) {
            for(int i = 0; i < notifiedListeners.size(); i++)
            {
                AccountListenerEntry listenerInstance = notifiedListeners.get(i);
                ZimbraLog.account.debug("Rollback account listener SAN update request from \"%s\".", listenerInstance.getListenerName());
                try {
                    listenerInstance.getAccountListener().onSANChange(acct, oldServiceAccountNumber);
                } catch (ServiceException se) {
                    ZimbraLog.account.debug("Rollback account listener SAN update failed from \"%s\". %s", listenerInstance.getListenerName(), se.getMessage());
                }
            }
        }
    }
}
