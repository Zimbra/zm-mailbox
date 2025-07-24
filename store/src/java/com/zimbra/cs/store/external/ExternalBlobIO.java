/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.store.external;

import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.MailboxBlob;

/**
 * Interface for the simplest blob store integration possible
 * The implementor must provide functionality for reading, writing, and deleting blobs
 */
public interface ExternalBlobIO {
    /**
     * Write data to blob store
     * @param in: InputStream containing data to be written
     * @param actualSize: size of data in stream, or -1 if size is unknown. To be used by implementor for optimization where possible
     * @param mbox: Mailbox which contains the blob. Can optionally be used by store for partitioning
     * @return locator string for the stored blob, unique identifier created by storage protocol
     * @throws IOException
     * @throws ServiceException
     */
    String writeStreamToStore(InputStream in, long actualSize, Mailbox mbox) throws IOException, ServiceException;

    /**
     * Write data to blob store
     *
     * @param in:         InputStream containing data to be written
     * @param actualSize: size of data in stream, or -1 if size is unknown. To be used by implementor for optimization where possible
     * @param mbox:       Mailbox which contains the blob. Can optionally be used by store for partitioning
     * @param destVolId:  Destination Volume id on which we need to store the blob
     * @return locator string for the stored blob, unique identifier created by storage protocol
     * @throws IOException
     * @throws ServiceException
     */
    default String writeStreamToStore(InputStream in, long actualSize, Mailbox mbox, short destVolId) throws IOException, ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * Create an input stream for reading data from blob store
     * @param locator: identifier string for the blob as returned from write operation
     * @param mbox: Mailbox which contains the blob. Can optionally be used by store for partitioning
     * @return InputStream containing the data
     * @throws IOException
     */
    InputStream readStreamFromStore(String locator, Mailbox mbox) throws IOException;

    /**
     * Delete a blob from the store
     * @param locator: identifier string for the blob
     * @param mbox: Mailbox which contains the blob. Can optionally be used by store for partitioning
     * @return true on success false on failure
     * @throws IOException
     */
    boolean deleteFromStore(String locator, Mailbox mbox) throws IOException;

     String returnS3BucketName(String locator);

     boolean deleteBlobsInBulk(Iterable<MailboxBlob.MailboxBlobInfo> blobs, Integer mailboxId);
}
