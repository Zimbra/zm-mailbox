/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
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
