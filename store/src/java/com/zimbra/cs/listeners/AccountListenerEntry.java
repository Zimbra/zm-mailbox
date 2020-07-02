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

import com.zimbra.cs.listeners.ListenerUtil.Priority;

public class AccountListenerEntry implements Comparable<AccountListenerEntry> {

    private String listenerName;
    private Priority priority;
    private AccountListener accountListener;

    public AccountListenerEntry(String listenerName, Priority priority, AccountListener accountListener) {
        this.listenerName = listenerName;
        this.priority = priority;
        this.accountListener = accountListener;
    }

    @Override
    public int compareTo(AccountListenerEntry other) {
        if (this.priority.ordinal() < other.priority.ordinal())
            return -1;
        else if (this.priority.ordinal() > other.priority.ordinal())
            return 1;
        else
            return 0;
    }

    public AccountListener getAccountListener() {
        return accountListener;
    }

    public void setAccountListener(AccountListener accountListener) {
        this.accountListener = accountListener;
    }

    public String getListenerName() {
        return listenerName;
    }

    public void setListenerName(String listenerName) {
        this.listenerName = listenerName;
    }

}

