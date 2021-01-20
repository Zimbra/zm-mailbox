/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019, 2020 Synacor, Inc.
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
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.listeners.ListenerUtil.Priority;

public abstract class DomainListener {

    private static final Map<String, DomainListenerEntry> mListeners = Collections
            .synchronizedMap(new HashMap<String, DomainListenerEntry>());

    public static void registerListener(final String listenerName, final Priority priority, DomainListener listener)
            throws ServiceException {
        if (mListeners.size() == LC.zimbra_listeners_maxsize.intValue()) {
            ZimbraLog.store.warn("Domain listener limit reached, registering of %s is ignored", listenerName);
            throw ServiceException.FORBIDDEN("Domain listener limit reached");
        }
        final DomainListenerEntry listenerEntry = mListeners.get(listenerName);
        if (listenerEntry != null) {
            ZimbraLog.store.warn("Listener %s is already registered, registering of %s is ignored", listenerName,
                    listenerEntry.getDomainListener().getClass().getCanonicalName());
            throw ServiceException.FORBIDDEN("Domain listener with this name is already registered");
        }
        ZimbraLog.store.info("Registering domain listener: %s", listenerName);
        final DomainListenerEntry entry = new DomainListenerEntry(listenerName, priority, listener);

        mListeners.put(listenerName, entry);
    }

    public static void deregisterListner(final String listenerName) {
        ZimbraLog.store.info("De-registering domain listener: %s", listenerName);
        mListeners.remove(listenerName);
    }

    public static void invokeOnDomainCreation(final Domain newDomain) {
        ZimbraLog.store.debug("Domain creation successful for user: %s", newDomain.getName());
        // invoke listeners
        final Map<String, DomainListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, DomainListenerEntry> listener : sortedListeners.entrySet()) {
            final DomainListenerEntry listenerInstance = listener.getValue();
            try {
                listenerInstance.getDomainListener().onDomainCreation(newDomain);
            } catch (ServiceException ex) {
                ZimbraLog.store.warn("Unable to invoke domain creation listener: " + listenerInstance.getListenerName(),
                        ex);
            }
        }
    }

    public static void invokeOnRenameDomain(final Domain domain, final String newName) {
        ZimbraLog.account.debug("Domain %s renamed to '%s'", domain.getName(), newName);

        final Map<String, DomainListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, DomainListenerEntry> listener : sortedListeners.entrySet()) {
            final DomainListenerEntry listenerInstance = listener.getValue();
            try {
                listenerInstance.getDomainListener().onDomainRename(domain, newName);
            } catch (ServiceException ex) {
                ZimbraLog.store.warn("Unable to invoke domain rename listener: " + listenerInstance.getListenerName(),
                        ex);
            }
        }
    }

    public static void invokeOnDeleteDomain(final Domain domain) {
        ZimbraLog.account.debug("Domain %s is getting deleted ", domain.getName());
        final Map<String, DomainListenerEntry> sortedListeners = ListenerUtil.sortByPriority(mListeners);
        for (Map.Entry<String, DomainListenerEntry> listener : sortedListeners.entrySet()) {
            final DomainListenerEntry listenerInstance = listener.getValue();
            try {
                listenerInstance.getDomainListener().onDomainDelete(domain);
            } catch (ServiceException ex) {
                ZimbraLog.store.warn("Unable to invoke domain delete listener: " + listenerInstance.getListenerName(),
                        ex);
            }
        }
    }

    public abstract void onDomainCreation(final Domain newDomain) throws ServiceException;

    public abstract void onDomainRename(final Domain domain, final String newName)
            throws ServiceException;

    public abstract void onDomainDelete(final Domain domain) throws ServiceException;
}