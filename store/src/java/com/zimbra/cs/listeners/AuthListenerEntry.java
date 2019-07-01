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

public class AuthListenerEntry implements Comparable<AuthListenerEntry> {

    private String listenerName;
    private Priority priority;
    private AuthListener authListener;

    public AuthListenerEntry(String listenerName, Priority priority, AuthListener authListener) {
        this.listenerName = listenerName;
        this.priority = priority;
        this.authListener = authListener;
    }

    @Override
    public int compareTo(AuthListenerEntry other) {
        if (this.priority.ordinal() < other.priority.ordinal())
            return -1;
        else if (this.priority.ordinal() > other.priority.ordinal())
            return 1;
        else
            return 0;
    }

    public AuthListener getAuthListener() {
        return authListener;
    }

    public void setAuthListener(AuthListener authListener) {
        this.authListener = authListener;
    }

    public String getListenerName() {
        return listenerName;
    }

    public void setListenerName(String listenerName) {
        this.listenerName = listenerName;
    }

}

