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

package com.zimbra.common.mailbox;

import java.io.InputStream;

import com.zimbra.common.service.ServiceException;

public interface ZimbraMailItem {
    /** Returns the date the item's content was last modified as number of milliseconds since 1970-01-01 00:00:00 UTC.
     *  For immutable objects (e.g. received messages), this will be the same as the date the item was created. */
    public long getDate();
    /** Returns the item's size as it counts against mailbox quota.  For items
     *  that have a blob, this is the size in bytes of the raw blob. */
    public long getSize();
    public int getModifiedSequence();
    public MailItemType getMailItemType();
    /** @return item's ID.  IDs are unique within a Mailbox and are assigned in increasing
     * (though not necessarily gap-free) order. */
    public int getIdInMailbox() throws ServiceException;
    /** Returns an {@link InputStream} of the raw, uncompressed content of the message.  This is the message body as
     * received via SMTP; no postprocessing has been performed to make opaque attachments (e.g. TNEF) visible.
     *
     * @return The data stream, or <tt>null</tt> if the item has no blob
     * @throws ServiceException when the message file does not exist.
     * @see #getMimeMessage()
     * @see #getContent() */
    public InputStream getContentStream() throws ServiceException;
}
