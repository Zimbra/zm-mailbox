/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.zimbra.common.service.ServiceException;

public class ImapServerListener {
    private final String server;

    private final ConcurrentMap<String, ImapListener> accountToSessionMap = new ConcurrentHashMap<String, ImapListener>();

    public ImapServerListener(String svr) {
        this.server = svr;
    }

    public void addListener(ImapListener listener) throws ServiceException {
        accountToSessionMap.put(listener.getTargetAccountId(), listener);
    }

    public void removeListener(ImapListener listener) throws ServiceException {
        accountToSessionMap.remove(listener.getTargetAccountId(), listener);
    }

    public boolean isListeningOn(String accountId) {
        return accountToSessionMap.containsKey(accountId);
    }
}
