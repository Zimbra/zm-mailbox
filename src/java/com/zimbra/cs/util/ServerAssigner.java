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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.servicelocator.RoundRobinSelector;
import com.zimbra.common.servicelocator.Selector;
import com.zimbra.common.servicelocator.ServiceLocator;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;


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
        ServiceLocator.Entry serviceInfo = null;
        try {
            serviceInfo = serviceLocator.findOne(serviceID, selector, null, true);
        } catch (IOException e) {
            ZimbraLog.account.warn("Could not reach service locator to select a new mailstore for account %s and service id %s; skipping mailstore assignment", acct.getName(), serviceID, e);
        }

        // permanently assign the account to the newly selected server
        if (serviceInfo != null) {
            acct.setMailHost(serviceInfo.hostName);
            ZimbraLog.account.info("Account %s is now assigned to mailhost %s", acct.getName(), serviceInfo.hostName);
        }

        return serviceInfo;
    }
}
