/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.Zimbra;

public abstract class StoreManager {

    private static StoreManager sInstance;
    static {
        try {
            sInstance = new FileBlobStore();
        } catch (Throwable t) {
            Zimbra.halt("Unable to initialize blob store", t);
        }
    }

    public static StoreManager getInstance() {
        return sInstance;
    }

    /**
     * Starts the blob store.
     */
    public abstract void startup() throws IOException, ServiceException;

    /**
     * Shuts down the blob store.
     */
    public abstract void shutdown();

    /**
     * Returns a 'BlobBuilder' which can be used to store a blob in incoming
     * directory asynchronously one chunk at a time. Blob will be compressed
     * if volume supports compression and blob size is over the compression
     * threshold.
     *
     * @return the BlobBuilder to use to construct the Blob
     * @throws IOException if an I/O error occurred
     * @throws ServiceException if a service exception occurred
     */
    public abstract BlobBuilder getBlobBuilder() throws IOException, ServiceException;
    
    /**
     * Store a blob in incoming directory.  Blob will be compressed if volume supports compression
     * and blob size is over the compression threshold.
     * @param data
     * @param sizeHint used for determining whether data should be compressed
     * @param digest
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public Blob storeIncoming(InputStream data, int sizeHint, StorageCallback callback)
    throws IOException, ServiceException {
        return storeIncoming(data, sizeHint, callback, false);
    }

    /**
     * Store a blob in incoming directory.  Blob will be compressed if volume supports compression
     * and blob size is over the compression threshold and storeAsIs is false.
     * @param data
     * @param sizeHint used for determining whether data should be compressed
     * @param digest
     * @param storeAsIs if true, store the blob as is even if volume supports compression
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public abstract Blob storeIncoming(InputStream data, int sizeHint, StorageCallback callback, boolean storeAsIs)
    throws IOException, ServiceException;

    /**
     * Store a blob in incoming directory.  Blob will be compressed if volume supports compression
     * and blob size is over the compression threshold.
     * @param data
     * @param digest
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public Blob storeIncoming(byte[] data, String digest)
    throws IOException, ServiceException {
        return storeIncoming(data, digest, false);
    }

    /**
     * Store a blob in incoming directory.  Blob will be compressed if volume supports compression
     * and blob size is over the compression threshold and storeAsIs is false.
     * @param data
     * @param digest
     * @param storeAsIs if true, store the blob as is even if volume supports compression
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public Blob storeIncoming(byte[] data, String digest, boolean storeAsIs)
    throws IOException, ServiceException {
        // Prevent bogus digest values.
        if (!ByteUtil.isValidDigest(digest))
            throw ServiceException.FAILURE("Invalid blob digest \"" + digest + "\"", null);

        return storeIncoming(new ByteArrayInputStream(data), data.length, null, storeAsIs);
    }

    /**
     * Create a copy in destMbox mailbox with message ID of destMsgId that
     * points to srcBlob.
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @return MailboxBlob object representing the copied blob
     * @throws IOException
     * @throws ServiceException
     */
    public abstract MailboxBlob copy(Blob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException;

    /**
	 * Create a link in destMbox mailbox with message ID of destMsgId that
	 * points to srcBlob.
	 * @param src
	 * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
	 * @return MailboxBlob object representing the linked blob
     * @throws IOException
     * @throws ServiceException
     */
    public abstract MailboxBlob link(Blob src, Mailbox destMbox, int destMsgId, int destRevision)
	throws IOException, ServiceException;

    /**
     * Rename a blob to a blob in mailbox directory.
     * @param src
     * @param destMbox
     * @param destMsgId mail_item.id value for message in destMbox
     * @param destRevision mail_item.mod_content value for message in destMbox
     * @return MailboxBlob object representing the renamed blob
     * @throws IOException
     * @throws ServiceException
     */
    public abstract MailboxBlob renameTo(Blob src, Mailbox destMbox, int destMsgId, int destRevision)
    throws IOException, ServiceException;

    /**
     * Deletes a blob from store.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param blob
     * @return true if blob was actually deleted
     * @throws IOException
     */
    public abstract boolean delete(MailboxBlob blob) throws IOException;

    /**
     * Deletes a blob from incoming directory.  If blob doesn't exist, no exception is
     * thrown and false is returned.
     * @param blobFile
     * @return true if blob was actually deleted
     * @throws IOException
     */
    public abstract boolean delete(Blob blob) throws IOException;

    /**
     * Find the MailboxBlob in mailbox mbox with matching message ID.
     * @param mbox
     * @param msgId mail_item.id value for message
     * @param revision mail_item.mod_content value for message
     * @return the <code>MailboxBlob</code>, or <code>null</code> if the file
     * does not exist
     * 
     * @throws ServiceException
     */
    public abstract MailboxBlob getMailboxBlob(Mailbox mbox, int msgId, int revision, short volumeId)
    throws ServiceException;

    /**
     * Return an InputStream of blob content.  Caller should close the
     * stream when done.
     * @param mboxBlob
     * @return
     * @throws IOException
     */
    public abstract InputStream getContent(MailboxBlob mboxBlob)
    throws IOException;

    /**
     * Return an InputStream of blob content.  Caller should close the
     * stream when done.
     * @param blob
     * @return
     * @throws IOException
     */
    public abstract InputStream getContent(Blob blob) throws IOException;

    /**
     * Deletes a user's entire store.  SHOULD BE CALLED CAREFULLY.  No going back.
     * @param mbox
     * @return true if store was actually deleted
     * @throws IOException
     * @throws ServiceException
     */
    public abstract boolean deleteStore(Mailbox mbox)
    throws IOException, ServiceException;
}
