/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.pushnotifications.filters;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Message;

/**
 * Create and execute {@link FilterChain}
 */
public class FilterManager {

    /**
     * Default filter chain that can be executed before all push notifications
     * @param account
     * @return TRUE if filter chain passes else FALSE
     */
    public static boolean executeDefaultFilters(Account account) {
        FilterChain filterChain = new DefaultFilterChain(account);
        return filterChain.execute();
    }

    /**
     * Executes a filter chain before a new message push notification
     * @param account
     * @param message
     * @return TRUE if filter chain passes else FALSE
     */
    public static boolean executeNewMessageFilters(Account account, Message message) {
        FilterChain filterChain = new NewMessageFilterChain(account, message);
        return filterChain.execute();
    }

}
