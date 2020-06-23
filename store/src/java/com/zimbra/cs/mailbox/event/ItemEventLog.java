/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
package com.zimbra.cs.mailbox.event;

import java.util.Collection;
import java.util.UUID;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxOperation;

public abstract class ItemEventLog {

    public abstract int getEventCount() throws ServiceException;
    public abstract Collection<MailboxEvent> getEvents(int offset, int count) throws ServiceException;
    public abstract Collection<MailboxEvent> getEventsBefore(long timestamp, int count) throws ServiceException;
    public abstract Collection<MailboxOperation> getLoggedOps() throws ServiceException;
    public abstract Collection<String> getLoggedUsers() throws ServiceException;
    public abstract void addEvent(MailboxEvent event) throws ServiceException;

    public String getId() {
        return id;
    }

    public ItemEventLog() {
        id = UUID.randomUUID().toString();
    }

    private String id;
}
