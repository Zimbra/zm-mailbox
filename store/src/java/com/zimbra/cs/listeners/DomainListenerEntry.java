/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019, 2021 Synacor, Inc.
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

public class DomainListenerEntry implements Comparable<DomainListenerEntry> {
    private String listenerName;
    private Priority priority;
    private DomainListener domainListener;

    public DomainListenerEntry(String listenerName, Priority priority, DomainListener domainListener) {
        this.listenerName = listenerName;
        this.priority = priority;
        this.domainListener = domainListener;
    }

    @Override
    public int compareTo(DomainListenerEntry other) {
        if (this.priority.ordinal() < other.priority.ordinal()) {
            return -1;
        } else if (this.priority.ordinal() > other.priority.ordinal()) {
            return 1;
        } else {
            return 0;
        }
    }

    public String getListenerName() {
        return this.listenerName;
    }

    public void setListenerName(String listenerName) {
        this.listenerName = listenerName;
    }

    public DomainListener getDomainListener() {
        return this.domainListener;
    }

    public void setDomainListener(DomainListener domainListener) {
        this.domainListener = domainListener;
    }
}