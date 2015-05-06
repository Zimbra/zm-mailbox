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

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;

public class DefaultFilterChain implements FilterChain {

    List<Filter> filterChain = new ArrayList<Filter>();

    public DefaultFilterChain(Account account) {
        init(account);
    }

    /**
     * @param account
     */
    private void init(Account account) {
        addFilter(new PrefEnabledFilter(account));
        addFilter(new AccountStatusFilter(account));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.zimbra.cs.pushnotifications.filters.FilterChain#addFilter(com.zimbra
     * .cs.pushnotifications.filters.Filter)
     */
    public void addFilter(Filter filter) {
        filterChain.add(filter);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.filters.FilterChain#execute()
     */
    @Override
    public boolean execute() {
        for (Filter filter : filterChain) {
            if (!filter.apply()) {
                ZimbraLog.mailbox.debug("Filter: %s is failing", filter.getClass().getSimpleName());
                return false;
            }
        }
        return true;
    }

}
