/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.io.IOException;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.servicelocator.RoundRobinSelector;
import com.zimbra.common.servicelocator.Selector;
import com.zimbra.common.servicelocator.ServiceLocator;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;


public class ServerAssigner {
    protected ServiceLocator serviceLocator;
    protected Selector<ServiceLocator.Entry> selector;

    public ServerAssigner(ServiceLocator serviceLocator, Selector<ServiceLocator.Entry> selector) {
        this.serviceLocator = serviceLocator;
        this.selector = selector;
    }

    public ServerAssigner(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
        selector = new RoundRobinSelector<>();
    }

    /**
     * Reassign an account to a new mailstore.
     *
     * @return the non-null service locator entry if a reassignment occurred, null if it didn't
     */
    public ServiceLocator.Entry reassign(Account acct, String serviceID) throws ServiceException {
        ZimbraLog.account.debug("No mailhost found for account %s; using service locator to select a new upstream", acct.getName());
        ServiceLocator.Entry serviceEntry = null;
        try {
            List<ServiceLocator.Entry> result = serviceLocator.find(serviceID, null, true);
            int iterations = result.size();
            Provisioning prov = Provisioning.getInstance();
            for (int i = 0; i < iterations; i++) {
                ServiceLocator.Entry entry = result.get(i);

                // Determine if server is in maintenance mode
                Server server = prov.getServerByName(entry.hostName);
                if (server != null && server.isOfflineForMaintenance()) {
                    continue;
                }

                serviceEntry = entry;
            }
        } catch (IOException e) {
            ZimbraLog.account.warn("Could not reach service locator to select a new mailstore for account %s and service id %s; skipping mailstore assignment", acct.getName(), serviceID, e);
        }

        // permanently assign the account to the newly selected server
        if (serviceEntry != null) {
            acct.setMailHost(serviceEntry.hostName);
            ZimbraLog.account.info("Account %s is now assigned to mailhost %s", acct.getName(), serviceEntry.hostName);
        }

        return serviceEntry;
    }
}
