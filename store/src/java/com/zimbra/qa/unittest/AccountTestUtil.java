/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.qa.unittest;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * Utilities for tests related to accounts.
 * Note that Junit classes are not imported; this needs to be runable even in shipped product where Junit is not provided
 *
 */
public class AccountTestUtil {
    public static final String DEFAULT_PASSWORD = "test123";

    public static boolean accountExists(String userName)
    throws ServiceException {
        String address = getAddress(userName);
        Account account = Provisioning.getInstance().get(AccountBy.name, address);
        return (account != null);
    }

    /**
     * @return the <code>Account</code>, or <code>null</code> if account does not exist.
     * @throws ServiceException if name is invalid or can't determine the default domain
     */
    public static Account getAccount(String userName)
    throws ServiceException {
        String address = getAddress(userName);
        return Provisioning.getInstance().get(AccountBy.name, address);
    }

    public static String getDomain()
    throws ServiceException {
        Config config = Provisioning.getInstance().getConfig(Provisioning.A_zimbraDefaultDomainName);
        String domain = config.getAttr(Provisioning.A_zimbraDefaultDomainName, null);
        assert(domain != null && domain.length() > 0);
        return domain;
    }

    public static Mailbox getMailbox(String userName)
    throws ServiceException {
        Account account = getAccount(userName);
        return MailboxManager.getInstance().getMailboxByAccount(account);
    }

    public static Server getServer(String userName)
    throws ServiceException {
        Account account = getAccount(userName);
        if (null == account) {
            return null;
        }
        return account.getServer();
    }

    public static String getAddress(String userName)
    throws ServiceException {
        if (userName.contains("@")) {
            return userName;
        } else {
            return userName + "@" + getDomain();
        }
    }

    public static String getAddress(String userName, String domainName) {
        return userName + "@" + domainName;
    }
}
