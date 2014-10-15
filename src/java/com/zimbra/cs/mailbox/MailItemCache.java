package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;

/** A cache of MailItem's, with itemId and uuid indexes */
public interface MailItemCache {

    /** Retrieves an item from the cache, by its id */
    public MailItem get(Mailbox mbox, int itemId) throws ServiceException;

    /** Retrieves an item from the cache, by its uuid */
    public MailItem get(Mailbox mbox, String uuid) throws ServiceException;

    /** Puts an item into the cache */
    public void put(Mailbox mbox, MailItem item) throws ServiceException;

    /** Removes an item from the cache */
    public MailItem remove(Mailbox mbox, int itemId) throws ServiceException;

    /** Removes all items from the cache */
    public void remove(Mailbox mbox) throws ServiceException, UnsupportedOperationException;
}
